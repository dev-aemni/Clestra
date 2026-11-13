package com.example

import com.example.data.model.ClestraIdGenerator
import com.example.data.network.CompressionUtils
import com.example.data.network.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCompressionRoundTrip() {
        val originalText = "Hello from Clestra! Testing P2P direct vs Serverless MongoDB relay routing."
        val compressed = CompressionUtils.compress(originalText)
        assertNotNull(compressed)
        assertTrue(compressed.isNotEmpty())

        val decompressed = CompressionUtils.decompress(compressed)
        assertEquals(originalText, decompressed)
    }

    @Test
    fun testEmptyCompression() {
        val compressed = CompressionUtils.compress("")
        assertEquals("", compressed)
        val decompressed = CompressionUtils.decompress("")
        assertEquals("", decompressed)
    }

    @Test
    fun testUniqueClestraIdGeneration() {
        val id1 = ClestraIdGenerator.generateUniqueClestraId("securePass123", "Alice")
        val id2 = ClestraIdGenerator.generateUniqueClestraId("securePass123", "Alice")
        val id3 = ClestraIdGenerator.generateUniqueClestraId("otherPass456", "Bob")

        assertTrue(id1.startsWith("CLS-"))
        assertEquals(18, id1.length)
        assertNotEquals(id1, id2)
        assertNotEquals(id1, id3)
    }

    @Test
    fun testEndToEndEncryptionRoundTrip() {
        val senderId = "CLS-1234-5678-ABCD"
        val recipientId = "CLS-9999-8888-WXYZ"
        val secretMessage = "Meet at 5pm — encrypted for P2P and MongoDB relay!"

        val encrypted = CryptoUtils.encryptPayload(secretMessage, senderId, recipientId)
        assertTrue(encrypted.startsWith("ENC1:"))
        assertFalse(encrypted.contains(secretMessage))

        val decrypted = CryptoUtils.decryptPayload(encrypted, senderId, recipientId)
        assertEquals(secretMessage, decrypted)
    }
}
