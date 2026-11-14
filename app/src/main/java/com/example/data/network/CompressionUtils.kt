package com.example.data.network

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * High-ratio binary & text compression utilities for Clestra messages, media, and file transfers.
 * Uses GZIP with BEST_COMPRESSION to shrink both text payloads and files before E2EE wrapping
 * for P2P, Serverless MongoDB Relay, and local/network file storage.
 */
object CompressionUtils {

    private class BestGzipOutputStream(out: OutputStream) : GZIPOutputStream(out) {
        init {
            def.setLevel(Deflater.BEST_COMPRESSION)
        }
    }

    fun compressBytes(rawBytes: ByteArray): ByteArray {
        if (rawBytes.isEmpty()) return rawBytes
        return try {
            val byteOut = ByteArrayOutputStream()
            BestGzipOutputStream(byteOut).use { gzip ->
                gzip.write(rawBytes)
            }
            byteOut.toByteArray()
        } catch (_: Exception) {
            rawBytes
        }
    }

    fun decompressBytes(compressedBytes: ByteArray): ByteArray {
        if (compressedBytes.isEmpty()) return compressedBytes
        if (!isGzipCompressed(compressedBytes)) return compressedBytes
        return try {
            ByteArrayInputStream(compressedBytes).use { byteIn ->
                GZIPInputStream(byteIn).use { gzip ->
                    gzip.readBytes()
                }
            }
        } catch (_: Exception) {
            compressedBytes
        }
    }

    fun isGzipCompressed(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()

    fun compress(rawText: String): String {
        if (rawText.isEmpty()) return ""
        return try {
            val compressed = compressBytes(rawText.toByteArray(StandardCharsets.UTF_8))
            Base64.getEncoder().encodeToString(compressed)
        } catch (_: Exception) {
            try {
                Base64.getEncoder().encodeToString(rawText.toByteArray(StandardCharsets.UTF_8))
            } catch (_: Exception) {
                rawText
            }
        }
    }

    fun decompress(compressedBase64: String): String {
        if (compressedBase64.isEmpty()) return ""
        return try {
            val bytes = Base64.getDecoder().decode(compressedBase64)
            String(decompressBytes(bytes), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            try {
                val bytes = Base64.getDecoder().decode(compressedBase64)
                String(bytes, StandardCharsets.UTF_8)
            } catch (_: Exception) {
                compressedBase64
            }
        }
    }
}
