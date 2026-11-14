package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.CompressionUtils
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraTextSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AudioMetadata(
    val baseMime: String,
    val durationMs: Long,
    val waveform: List<Int>
)

data class RecordedVoiceNote(
    val file: File,
    val durationMs: Long,
    val waveform: List<Int>
)

object AudioMetaCodec {
    private val defaultWaveform = listOf(
        24, 42, 68, 52, 85, 92, 60, 38, 72, 95, 76, 48,
        34, 66, 88, 74, 50, 82, 96, 62, 44, 70, 52, 30
    )

    fun encodeMime(baseMime: String, durationMs: Long, waveform: List<Int>): String {
        val cleanMime = baseMime.substringBefore(';').trim().ifBlank { "audio/mp4" }
        val safeDuration = durationMs.coerceAtLeast(500L)
        val normalizedWf = downsampleWaveform(waveform, targetBars = 24)
            .joinToString(",")
        return "$cleanMime;dur=$safeDuration;wf=$normalizedWf"
    }

    fun parseMeta(mimeType: String?): AudioMetadata {
        if (mimeType.isNullOrBlank()) {
            return AudioMetadata("audio/mp4", 0L, defaultWaveform)
        }
        val segments = mimeType.split(';')
        val baseMime = segments.firstOrNull()?.trim().orEmpty().ifBlank { "audio/mp4" }
        var duration = 0L
        var waveform: List<Int>? = null
        segments.drop(1).forEach { seg ->
            val trimmed = seg.trim()
            when {
                trimmed.startsWith("dur=") -> {
                    duration = trimmed.removePrefix("dur=").toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                }
                trimmed.startsWith("wf=") -> {
                    val parsed = trimmed.removePrefix("wf=")
                        .split(',')
                        .mapNotNull { it.trim().toIntOrNull()?.coerceIn(10, 100) }
                    if (parsed.isNotEmpty()) {
                        waveform = downsampleWaveform(parsed, 24)
                    }
                }
            }
        }
        return AudioMetadata(
            baseMime = baseMime,
            durationMs = duration,
            waveform = waveform ?: defaultWaveform
        )
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = ((durationMs.coerceAtLeast(0L) + 450L) / 1000L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun downsampleWaveform(rawSamples: List<Int>, targetBars: Int = 24): List<Int> {
        if (rawSamples.isEmpty()) return defaultWaveform.take(targetBars)
        if (rawSamples.size == targetBars) {
            return rawSamples.map { it.coerceIn(12, 100) }
        }
        val maxSample = (rawSamples.maxOrNull() ?: 100).coerceAtLeast(1)
        return List(targetBars) { idx ->
            val start = (idx * rawSamples.size) / targetBars
            val end = (((idx + 1) * rawSamples.size) / targetBars).coerceAtLeast(start + 1)
            val slice = rawSamples.subList(start, end.coerceAtMost(rawSamples.size))
            val peak = slice.maxOrNull() ?: 20
            val scaled = ((peak.toFloat() / maxSample.toFloat()) * 88f + 12f).roundToInt()
            scaled.coerceIn(12, 100)
        }
    }

    /**
     * Reads true duration & waveform energy bars for MP3, WAV, M4A, AAC, OGG, FLAC files.
     */
    suspend fun inspectAudioFile(
        context: Context,
        uriString: String?,
        fileName: String? = null,
        bars: Int = 24
    ): Pair<Long, List<Int>> = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext 0L to defaultWaveform.take(bars)
        val resolvedUri = FileAttachmentUtils.materializeTransportData(context, uriString, fileName) ?: uriString
        var durationMs = 0L
        val retriever = MediaMetadataRetriever()
        try {
            when {
                resolvedUri.startsWith("file://") -> {
                    val path = Uri.parse(resolvedUri).path
                    if (!path.isNullOrBlank()) retriever.setDataSource(path)
                }
                resolvedUri.startsWith("content://") -> {
                    retriever.setDataSource(context, Uri.parse(resolvedUri))
                }
                resolvedUri.startsWith("http://") || resolvedUri.startsWith("https://") -> {
                    retriever.setDataSource(resolvedUri, emptyMap())
                }
            }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L) ?: 0L
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val waveform = try {
            val bytes = readAudioSampleBytes(context, resolvedUri)
            if (bytes != null && bytes.size > 64) {
                extractEnergyWaveformFromBytes(bytes, bars)
            } else {
                deterministicWaveformForSeed(fileName ?: resolvedUri, bars)
            }
        } catch (_: Exception) {
            deterministicWaveformForSeed(fileName ?: resolvedUri, bars)
        }

        durationMs to waveform
    }

    private fun readAudioSampleBytes(context: Context, uriString: String): ByteArray? {
        return try {
            val maxRead = 320 * 1024
            when {
                uriString.startsWith("gzb64:") -> {
                    val compressed = Base64.decode(uriString.removePrefix("gzb64:"), Base64.DEFAULT)
                    CompressionUtils.decompressBytes(compressed).take(maxRead).toByteArray()
                }
                uriString.startsWith("file://") -> {
                    val path = Uri.parse(uriString).path ?: return null
                    FileInputStream(File(path)).use { input ->
                        val buf = ByteArray(maxRead)
                        val count = input.read(buf)
                        if (count > 0) buf.copyOf(count) else null
                    }
                }
                uriString.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                        val buf = ByteArray(maxRead)
                        val count = input.read(buf)
                        if (count > 0) buf.copyOf(count) else null
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun extractEnergyWaveformFromBytes(bytes: ByteArray, bars: Int = 24): List<Int> {
        val isWav = bytes.size > 44 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'A'.code.toByte()

        val startOffset = if (isWav) 44 else (bytes.size / 12).coerceAtMost(512)
        val usableLen = (bytes.size - startOffset).coerceAtLeast(bars)
        val bucketSize = (usableLen / bars).coerceAtLeast(1)

        val rawEnergies = if (isWav) {
            // Compute 16-bit PCM sample peak energy per bucket
            List(bars) { i ->
                val from = startOffset + i * bucketSize
                val to = (from + bucketSize).coerceAtMost(bytes.size - 1)
                var sum = 0L
                var count = 0
                var idx = from
                while (idx + 1 < to) {
                    val sample = (bytes[idx].toInt() and 0xFF) or (bytes[idx + 1].toInt() shl 8)
                    sum += abs(sample.toShort().toInt())
                    count++
                    idx += 4
                }
                if (count > 0) (sum / count).toInt() else 200
            }
        } else {
            // Compressed frame entropy variance across MP3 / AAC / OGG / FLAC stream
            List(bars) { i ->
                val from = startOffset + i * bucketSize
                val to = (from + bucketSize).coerceAtMost(bytes.size)
                var energy = 0L
                var count = 0
                var idx = from + 1
                while (idx < to) {
                    val delta = abs((bytes[idx].toInt() and 0xFF) - (bytes[idx - 1].toInt() and 0xFF))
                    energy += delta
                    count++
                    idx += 2
                }
                if (count > 0) (energy / count).toInt() else 30
            }
        }

        val minE = rawEnergies.minOrNull() ?: 0
        val maxE = (rawEnergies.maxOrNull() ?: 1).coerceAtLeast(minE + 1)
        return rawEnergies.map { e ->
            val ratio = (e - minE).toFloat() / (maxE - minE).toFloat()
            (18f + ratio * 78f).roundToInt().coerceIn(16, 100)
        }
    }

    private fun deterministicWaveformForSeed(seed: String, bars: Int): List<Int> {
        var h = seed.hashCode().toLong()
        return List(bars) { idx ->
            h = h * 6364136223846793005L + 1442695040888963407L + idx
            val v = (((h ushr 16) and 0x7FFL).toInt() % 76) + 18
            v.coerceIn(16, 98)
        }
    }
}

class ClestraVoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L
    private val amplitudeHistory = mutableListOf<Int>()

    fun start(): Boolean {
        return try {
            val mediaDir = File(context.filesDir, "clestra_media").apply { mkdirs() }
            val file = File(mediaDir, "voice_${UUID.randomUUID()}.m4a")
            outputFile = file
            amplitudeHistory.clear()

            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = r
            startedAtMs = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            cancel()
            false
        }
    }

    fun pollAmplitudeNormalized(): Int {
        val rawAmp = try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
        val normalized = if (rawAmp <= 0) {
            16
        } else {
            val root = sqrt(rawAmp.toDouble() / 32767.0)
            (16.0 + root * 84.0).roundToInt().coerceIn(14, 100)
        }
        amplitudeHistory.add(normalized)
        return normalized
    }

    fun currentElapsedMs(): Long {
        if (startedAtMs <= 0L) return 0L
        return (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
    }

    fun stopAndSave(): RecordedVoiceNote? {
        val file = outputFile ?: return null
        val elapsed = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(500L)
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile = null
            if (!file.exists() || file.length() <= 0L) {
                file.delete()
                null
            } else {
                val waveform = AudioMetaCodec.downsampleWaveform(amplitudeHistory.toList(), 24)
                RecordedVoiceNote(file = file, durationMs = elapsed, waveform = waveform)
            }
        } catch (_: Exception) {
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
            recorder = null
            outputFile = null
            file.delete()
            null
        }
    }

    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
        startedAtMs = 0L
        amplitudeHistory.clear()
    }
}

class ClestraAudioPlaybackController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    var activeUri by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentPositionMs by mutableLongStateOf(0L)
        private set
    var totalDurationMs by mutableLongStateOf(0L)
        private set

    fun playOrToggle(uriString: String) {
        val resolvedUri = FileAttachmentUtils.materializeTransportData(context, uriString, "audio_preview.m4a") ?: uriString
        if (activeUri == uriString && mediaPlayer != null) {
            val mp = mediaPlayer ?: return
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
                currentPositionMs = mp.currentPosition.toLong()
            } else {
                mp.start()
                isPlaying = true
            }
            return
        }

        releasePlayerOnly()
        activeUri = uriString
        currentPositionMs = 0L

        val mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { prepared ->
                totalDurationMs = prepared.duration.toLong().coerceAtLeast(0L)
                prepared.start()
                this@ClestraAudioPlaybackController.isPlaying = true
            }
            setOnCompletionListener {
                this@ClestraAudioPlaybackController.isPlaying = false
                currentPositionMs = 0L
                try {
                    it.seekTo(0)
                } catch (_: Exception) {
                }
            }
            setOnErrorListener { _, _, _ ->
                this@ClestraAudioPlaybackController.isPlaying = false
                activeUri = null
                true
            }
        }

        try {
            when {
                resolvedUri.startsWith("file://") -> {
                    val path = Uri.parse(resolvedUri).path ?: throw IllegalArgumentException("Missing path")
                    mp.setDataSource(path)
                }
                resolvedUri.startsWith("content://") -> {
                    mp.setDataSource(context, Uri.parse(resolvedUri))
                }
                else -> {
                    mp.setDataSource(resolvedUri)
                }
            }
            mediaPlayer = mp
            mp.prepareAsync()
        } catch (_: Exception) {
            try {
                mp.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
            activeUri = null
            isPlaying = false
        }
    }

    fun seekFraction(uriString: String, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        if (activeUri != uriString || mediaPlayer == null) {
            playOrToggle(uriString)
            return
        }
        val mp = mediaPlayer ?: return
        val dur = totalDurationMs.takeIf { it > 0L } ?: mp.duration.toLong().coerceAtLeast(0L)
        if (dur > 0L) {
            val targetMs = (dur * clamped).roundToInt()
            try {
                mp.seekTo(targetMs)
                currentPositionMs = targetMs.toLong()
            } catch (_: Exception) {
            }
        }
    }

    internal fun syncProgress() {
        val mp = mediaPlayer ?: return
        if (isPlaying) {
            try {
                currentPositionMs = mp.currentPosition.toLong().coerceAtLeast(0L)
                val dur = mp.duration.toLong()
                if (dur > 0L) totalDurationMs = dur
            } catch (_: Exception) {
            }
        }
    }

    private fun releasePlayerOnly() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        isPlaying = false
    }

    fun release() {
        releasePlayerOnly()
        activeUri = null
        currentPositionMs = 0L
        totalDurationMs = 0L
    }
}

val LocalClestraAudioController = compositionLocalOf<ClestraAudioPlaybackController?> { null }

/**
 * Compact packed Soft UI Voice & Audio Visualizer Bar with Play/Pause, scrubbing waveform, and tabular time.
 */
@Composable
fun ClestraAudioPlayerBar(
    uriString: String?,
    waveform: List<Int>,
    fallbackDurationMs: Long,
    modifier: Modifier = Modifier,
    accentColor: Color = ClestraCopper,
    surfaceColor: Color = ClestraNeuSurface
) {
    val controller = LocalClestraAudioController.current
    val isThisActive = !uriString.isNullOrBlank() && controller?.activeUri == uriString
    val isPlaying = isThisActive && (controller?.isPlaying == true)

    LaunchedEffect(isThisActive, isPlaying) {
        while (isThisActive && isPlaying) {
            controller?.syncProgress()
            delay(80L)
        }
    }

    val effectiveDurationMs = if (isThisActive && (controller?.totalDurationMs ?: 0L) > 0L) {
        controller!!.totalDurationMs
    } else {
        fallbackDurationMs.coerceAtLeast(0L)
    }

    val currentPosMs = if (isThisActive) (controller?.currentPositionMs ?: 0L) else 0L
    val progress = if (effectiveDurationMs > 0L) {
        (currentPosMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val bars = if (waveform.isNotEmpty()) waveform else AudioMetaCodec.parseMeta(null).waveform
    val displayedTime = if (isThisActive && (isPlaying || currentPosMs > 0L)) {
        AudioMetaCodec.formatDuration(currentPosMs)
    } else {
        AudioMetaCodec.formatDuration(effectiveDurationMs)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    if (isPlaying) {
                        Modifier.neumorphicPressed(
                            cornerRadius = 17.dp,
                            containerColor = surfaceColor
                        )
                    } else {
                        Modifier.neumorphicRaised(
                            cornerRadius = 17.dp,
                            elevation = 3.5.dp,
                            containerColor = surfaceColor
                        )
                    }
                )
                .clip(CircleShape)
                .clickable(enabled = !uriString.isNullOrBlank()) {
                    if (!uriString.isNullOrBlank()) {
                        controller?.playOrToggle(uriString)
                    }
                }
                .testTag("audio_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .pointerInput(uriString) {
                    detectTapGestures { offset ->
                        if (!uriString.isNullOrBlank() && size.width > 0) {
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            controller?.seekFraction(uriString, fraction)
                        }
                    }
                }
        ) {
            val count = bars.size.coerceAtLeast(1)
            val totalW = size.width
            val totalH = size.height
            val slotW = totalW / count.toFloat()
            val barW = (slotW * 0.56f).coerceIn(2.dp.toPx(), 4.dp.toPx())
            val playedThreshold = progress * count

            bars.forEachIndexed { i, amp ->
                val ratio = (amp.coerceIn(12, 100) / 100f)
                val barH = (totalH * ratio).coerceAtLeast(4.dp.toPx())
                val x = i * slotW + (slotW - barW) / 2f
                val y = (totalH - barH) / 2f
                val isPlayedBar = isThisActive && (i <= playedThreshold)
                val barColor = if (isPlayedBar) {
                    accentColor
                } else {
                    ClestraTextSoft.copy(alpha = 0.34f)
                }
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = displayedTime,
            color = ClestraTextSoft,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            maxLines = 1
        )
    }
}

/**
 * Compact Soft UI voice-note live recording strip inside the composer dock.
 */
@Composable
fun ClestraLiveRecordingStrip(
    elapsedMs: Long,
    liveAmplitudes: List<Int>,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.dp)
                .neumorphicRaised(
                    cornerRadius = 14.dp,
                    elevation = 4.dp,
                    containerColor = ClestraNeuSurface
                )
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onCancel)
                .testTag("voice_cancel_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Discard voice message",
                tint = ClestraError,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .neumorphicPressed(
                    cornerRadius = 18.dp,
                    containerColor = ClestraNeuWell
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ClestraError)
            )

            Text(
                text = AudioMetaCodec.formatDuration(elapsedMs),
                color = ClestraCharcoal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
            ) {
                val maxBars = 22
                val recent = liveAmplitudes.takeLast(maxBars)
                val padded = List(maxBars - recent.size) { 16 } + recent
                val totalW = size.width
                val totalH = size.height
                val slotW = totalW / maxBars.toFloat()
                val barW = (slotW * 0.56f).coerceIn(2.dp.toPx(), 3.8.dp.toPx())

                padded.forEachIndexed { idx, amp ->
                    val ratio = amp.coerceIn(14, 100) / 100f
                    val barH = (totalH * ratio).coerceAtLeast(3.dp.toPx())
                    val x = idx * slotW + (slotW - barW) / 2f
                    val y = (totalH - barH) / 2f
                    drawRoundRect(
                        color = ClestraCopper,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(42.dp)
                .neumorphicRaised(
                    cornerRadius = 15.dp,
                    elevation = 4.dp,
                    containerColor = ClestraCopper
                )
                .clip(RoundedCornerShape(15.dp))
                .clickable(onClick = onSend)
                .testTag("voice_send_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send voice message",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
