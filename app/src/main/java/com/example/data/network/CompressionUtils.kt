package com.example.data.network

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Lean Compression & Decompression utilities for Clestra messages.
 * Compresses message payloads into compact GZIP + Base64 strings for minimal
 * bandwidth and zero storage bloat on serverless MongoDB Atlas free tier.
 */
object CompressionUtils {

    fun compress(rawText: String): String {
        if (rawText.isEmpty()) return ""
        return try {
            val byteOut = ByteArrayOutputStream()
            GZIPOutputStream(byteOut).use { gzip ->
                gzip.write(rawText.toByteArray(StandardCharsets.UTF_8))
            }
            Base64.getEncoder().encodeToString(byteOut.toByteArray())
        } catch (e: Exception) {
            // Fallback to simple Base64 encoding if GZIP fails
            try {
                Base64.getEncoder().encodeToString(rawText.toByteArray(StandardCharsets.UTF_8))
            } catch (fallbackEx: Exception) {
                rawText
            }
        }
    }

    fun decompress(compressedBase64: String): String {
        if (compressedBase64.isEmpty()) return ""
        return try {
            val bytes = Base64.getDecoder().decode(compressedBase64)
            // Attempt GZIP decode
            ByteArrayInputStream(bytes).use { byteIn ->
                GZIPInputStream(byteIn).use { gzip ->
                    gzip.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                        reader.readText()
                    }
                }
            }
        } catch (e: Exception) {
            // If not GZIP, try direct Base64 UTF-8 decode
            try {
                val bytes = Base64.getDecoder().decode(compressedBase64)
                String(bytes, StandardCharsets.UTF_8)
            } catch (fallbackError: Exception) {
                compressedBase64 // return as is if both fail
            }
        }
    }
}
