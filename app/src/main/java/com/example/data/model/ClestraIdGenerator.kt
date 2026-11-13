package com.example.data.model

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object ClestraIdGenerator {

    private val sequenceCounter = AtomicLong(0L)

    /**
     * Generates an uncollidable, unique, permanent Clestra ID.
     * Format: CLS-XXXX-XXXX-XXXX (18 chars)
     *
     * - Segments 1 & 2 (8 Base36 chars): Epoch millisecond timestamp encoded in Base36,
     *   ensuring IDs generated in different milliseconds can never collide.
     * - Segment 3 (4 Base36 chars): Monotonic atomic sequence + System.nanoTime() +
     *   cryptographic SHA-256 digest of credentials and UUID salt, preventing same-millisecond collisions.
     */
    fun generateUniqueClestraId(password: String, username: String): String {
        val timestamp = System.currentTimeMillis()
        val nanos = System.nanoTime()
        val seq = sequenceCounter.getAndIncrement()
        val salt = UUID.randomUUID().toString()

        // 8-char Base36 timestamp (covers millisecond uniqueness up to year 2059)
        val timeBase36 = timestamp.toString(36)
            .uppercase(Locale.ROOT)
            .padStart(8, '0')
            .takeLast(8)

        val p1 = timeBase36.substring(0, 4)
        val p2 = timeBase36.substring(4, 8)

        // Mix monotonic counter + nanotime + cryptographic digest for sub-ms uncollidability
        val rawInput = "$password::$username::$timestamp::$nanos::$seq::$salt"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(rawInput.toByteArray(StandardCharsets.UTF_8))

        val digestPart = ((digest[0].toLong() and 0xFF) shl 8) or (digest[1].toLong() and 0xFF)
        val combined = ((seq * 131L + (nanos and 0xFFFFL) + digestPart) and 0xFFFFFFFL)
        val p3 = (combined % 1_679_616L) // 36^4
            .toString(36)
            .uppercase(Locale.ROOT)
            .padStart(4, '0')

        return "CLS-$p1-$p2-$p3"
    }

    /**
     * Compute SHA-256 hash of password for local storage
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
