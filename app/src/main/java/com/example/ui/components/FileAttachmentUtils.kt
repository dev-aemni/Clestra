package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.network.CompressionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.util.UUID

enum class ClestraFileKind(val badgeLabel: String) {
    APK("APK"),
    CODE_OR_TEXT("TEXT"),
    PDF("PDF"),
    ARCHIVE("ARCHIVE"),
    AUDIO("AUDIO"),
    VIDEO("VIDEO"),
    IMAGE("IMAGE"),
    UNKNOWN("FILE")
}

data class EmbeddedAttachmentResult(
    val localFileUri: String,
    val transportData: String, // Embedded URI or base64 data URI when compact
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isImage: Boolean
)

object FileAttachmentUtils {

    private val textExtensions = setOf(
        "txt", "md", "markdown", "json", "kt", "kts", "java", "py", "js", "jsx",
        "ts", "tsx", "html", "htm", "css", "scss", "xml", "yaml", "yml", "sql",
        "sh", "bash", "zsh", "csv", "log", "properties", "toml", "ini", "cfg",
        "conf", "gradle", "c", "cpp", "h", "hpp", "rs", "go", "dart", "swift",
        "rb", "php", "env", "gitignore"
    )

    fun classifyFileKind(fileName: String?, mimeType: String?): ClestraFileKind {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val mime = mimeType?.lowercase().orEmpty()
        return when {
            ext == "apk" || mime.contains("android.package-archive") -> ClestraFileKind.APK
            ext == "pdf" || mime == "application/pdf" -> ClestraFileKind.PDF
            ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") || mime.contains("zip") || mime.contains("archive") -> ClestraFileKind.ARCHIVE
            ext in setOf("mp3", "wav", "m4a", "aac", "ogg", "flac") || mime.startsWith("audio/") -> ClestraFileKind.AUDIO
            ext in setOf("mp4", "mkv", "mov", "webm", "avi") || mime.startsWith("video/") -> ClestraFileKind.VIDEO
            ext in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic") || mime.startsWith("image/") -> ClestraFileKind.IMAGE
            ext in textExtensions || mime.startsWith("text/") || mime == "application/json" || mime == "application/xml" -> ClestraFileKind.CODE_OR_TEXT
            else -> ClestraFileKind.UNKNOWN
        }
    }

    fun inferCodeLanguage(fileName: String?): String {
        return when (fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js", "jsx" -> "javascript"
            "ts", "tsx" -> "typescript"
            "html", "htm" -> "html"
            "xml" -> "xml"
            "css", "scss" -> "css"
            "json" -> "json"
            "sql" -> "sql"
            "sh", "bash", "zsh" -> "bash"
            "md", "markdown" -> "markdown"
            "yaml", "yml" -> "yaml"
            "c", "h" -> "c"
            "cpp", "hpp" -> "cpp"
            "rs" -> "rust"
            "go" -> "go"
            "dart" -> "dart"
            "swift" -> "swift"
            else -> "text"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    suspend fun importAndEmbedAttachment(
        context: Context,
        sourceUri: Uri,
        forceImage: Boolean = false
    ): EmbeddedAttachmentResult? = withContext(Dispatchers.IO) {
        try {
            val mediaDir = File(context.filesDir, "clestra_media").apply { mkdirs() }
            val queriedMeta = queryUriMeta(context, sourceUri)
            val mimeType = context.contentResolver.getType(sourceUri)
                ?: queriedMeta.second
                ?: if (forceImage) "image/jpeg" else "application/octet-stream"
            val rawName = queriedMeta.first ?: if (forceImage) "clestra_photo.jpg" else "attachment.bin"
            val safeName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val isImg = forceImage || mimeType.startsWith("image/")

            if (isImg) {
                val input = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
                val rawBytes = input.use { it.readBytes() }
                val destFile = File(mediaDir, "img_${UUID.randomUUID()}_$safeName")

                // Decode & embed compactly so it renders crisp inside chat and transports safely
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                val finalBytes = if (decoded != null) {
                    val scaled = scaleBitmapIfNeeded(decoded, maxDim = 1440)
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
                    if (scaled !== decoded) scaled.recycle()
                    decoded.recycle()
                    out.toByteArray()
                } else {
                    rawBytes
                }

                destFile.writeBytes(finalBytes)
                val localUri = Uri.fromFile(destFile).toString()

                // Embed base64 data URI cleanly when compact (<= 280 KB) for encrypted payload embedding
                val embeddedTransport = if (finalBytes.size <= 280 * 1024) {
                    "data:image/jpeg;base64," + Base64.encodeToString(finalBytes, Base64.NO_WRAP)
                } else {
                    localUri
                }

                return@withContext EmbeddedAttachmentResult(
                    localFileUri = localUri,
                    transportData = embeddedTransport,
                    displayName = safeName,
                    mimeType = "image/jpeg",
                    sizeBytes = finalBytes.size.toLong(),
                    isImage = true
                )
            } else {
                val input = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
                val destFile = File(mediaDir, "file_${UUID.randomUUID()}_$safeName")
                input.use { src ->
                    destFile.outputStream().use { dst -> src.copyTo(dst) }
                }
                val localUri = Uri.fromFile(destFile).toString()
                val fileSize = destFile.length()
                val transportData = if (fileSize in 1L..(2L * 1024L * 1024L)) {
                    val compressed = CompressionUtils.compressBytes(destFile.readBytes())
                    if (compressed.size <= 420 * 1024) {
                        "gzb64:" + Base64.encodeToString(compressed, Base64.NO_WRAP)
                    } else {
                        localUri
                    }
                } else {
                    localUri
                }
                return@withContext EmbeddedAttachmentResult(
                    localFileUri = localUri,
                    transportData = transportData,
                    displayName = rawName,
                    mimeType = mimeType,
                    sizeBytes = fileSize,
                    isImage = false
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun materializeTransportData(
        context: Context,
        transportUri: String?,
        fileNameHint: String? = null
    ): String? {
        if (transportUri.isNullOrBlank()) return null
        if (!transportUri.startsWith("gzb64:")) return transportUri
        return try {
            val encoded = transportUri.removePrefix("gzb64:")
            val compressedBytes = Base64.decode(encoded, Base64.DEFAULT)
            val restoredBytes = CompressionUtils.decompressBytes(compressedBytes)
            val mediaDir = File(context.filesDir, "clestra_media").apply { mkdirs() }
            val safeName = (fileNameHint ?: "attachment.bin")
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "attachment.bin" }
            val outFile = File(mediaDir, "recv_${UUID.randomUUID()}_$safeName")
            outFile.writeBytes(restoredBytes)
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapIfNeeded(bmp: Bitmap, maxDim: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxDim) return bmp
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        return Bitmap.createScaledBitmap(bmp, (w * ratio).toInt().coerceAtLeast(1), (h * ratio).toInt().coerceAtLeast(1), true)
    }

    private fun queryUriMeta(context: Context, uri: Uri): Pair<String?, String?> {
        var name: String? = null
        var mime: String? = null
        try {
            mime = context.contentResolver.getType(uri)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {
        }
        return name to mime
    }

    suspend fun readTextFileContent(context: Context, uriString: String?, maxChars: Int = 120_000): String? =
        withContext(Dispatchers.IO) {
            if (uriString.isNullOrBlank()) return@withContext null
            try {
                openInputStreamAny(context, uriString)?.use { stream ->
                    val bytes = stream.readBytes()
                    val text = bytes.toString(Charsets.UTF_8)
                    if (text.length > maxChars) text.substring(0, maxChars) + "\n\n... (truncated preview)"
                    else text
                }
            } catch (_: Exception) {
                null
            }
        }

    suspend fun saveImageToGallery(
        context: Context,
        uriString: String,
        titleHint: String = "clestra_image"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = readAllBytes(context, uriString) ?: return@withContext false
            val cleanName = titleHint.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32)
            val fileName = "${cleanName}_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Clestra")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@withContext false
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Clestra").apply { mkdirs() }
                File(dir, fileName).writeBytes(bytes)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun saveFileToDownloads(
        context: Context,
        uriString: String,
        fileName: String,
        mimeType: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes = readAllBytes(context, uriString) ?: return@withContext false
            val cleanName = fileName.ifBlank { "clestra_file_${System.currentTimeMillis()}" }
            val resolvedMime = mimeType ?: "application/octet-stream"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, cleanName)
                    put(MediaStore.Downloads.MIME_TYPE, resolvedMime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Clestra")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@withContext false
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Clestra").apply { mkdirs() }
                File(dir, cleanName).writeBytes(bytes)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readAllBytes(context: Context, uriString: String): ByteArray? {
        if (uriString.startsWith("gzb64:")) {
            val b64 = uriString.removePrefix("gzb64:")
            return CompressionUtils.decompressBytes(Base64.decode(b64, Base64.DEFAULT))
        }
        if (uriString.startsWith("data:") && uriString.contains("base64,")) {
            val b64 = uriString.substringAfter("base64,")
            return Base64.decode(b64, Base64.DEFAULT)
        }
        return openInputStreamAny(context, uriString)?.use { it.readBytes() }
    }

    fun toDisplayModel(uriString: String?): Any? {
        if (uriString.isNullOrBlank()) return null
        if (uriString.startsWith("data:") && uriString.contains("base64,")) {
            return try {
                Base64.decode(uriString.substringAfter("base64,"), Base64.DEFAULT)
            } catch (_: Exception) {
                uriString
            }
        }
        return uriString
    }

    private fun openInputStreamAny(context: Context, uriString: String): InputStream? {
        return when {
            uriString.startsWith("http://") || uriString.startsWith("https://") -> URL(uriString).openStream()
            uriString.startsWith("file://") -> FileInputStream(File(Uri.parse(uriString).path ?: return null))
            else -> context.contentResolver.openInputStream(Uri.parse(uriString))
        }
    }
}
