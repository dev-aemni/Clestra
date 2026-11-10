package com.example

import com.example.data.model.ClestraIdGenerator
import com.example.data.network.CompressionUtils
import org.junit.Assert.assertEquals
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
        assertEquals(18, id1.length) // CLS-XXXX-XXXX-XXXX (3 + 1 + 4 + 1 + 4 + 1 + 4 = 18)
        assertNotEquals(id1, id2) // Non-colliding due to high-entropy salt
        assertNotEquals(id1, id3)
    }
}
