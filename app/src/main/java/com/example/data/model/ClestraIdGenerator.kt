package com.example.data.model

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object ClestraIdGenerator {

    /**
     * Generates a collision-resistant, unique, permanent Clestra ID
     * cryptographically derived from the user's password, username, and high-entropy salt.
     * Format: CLS-XXXX-XXXX-XXXX
     */
    fun generateUniqueClestraId(password: String, username: String): String {
        val salt = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val rawInput = "$password::$username::$salt::$timestamp"

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawInput.toByteArray(StandardCharsets.UTF_8))
        val hex = hashBytes.joinToString("") { "%02x".format(it) }

        val p1 = hex.substring(0, 4).uppercase(Locale.ROOT)
        val p2 = hex.substring(4, 8).uppercase(Locale.ROOT)
        val p3 = hex.substring(8, 12).uppercase(Locale.ROOT)

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
