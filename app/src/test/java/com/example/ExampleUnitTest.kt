package com.example

import com.example.data.model.ClestraIdGenerator
import com.example.data.network.CompressionUtils
import com.example.data.network.CryptoUtils
import com.example.data.security.InternalVault
import com.example.ui.components.ClestraFileKind
import com.example.ui.components.EmojiCatalog
import com.example.ui.components.FileAttachmentUtils
import com.example.ui.components.MarkdownParser
import com.example.ui.components.MarkdownSegment
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
        assertTrue(encrypted.startsWith("ENC2:"))
        assertFalse(encrypted.contains(secretMessage))

        val decrypted = CryptoUtils.decryptPayload(encrypted, senderId, recipientId)
        assertEquals(secretMessage, decrypted)
    }

    @Test
    fun testCompressionReducesRepetitiveEncryptedPayloads() {
        val senderId = "dev"
        val recipientId = "CLS-9999-8888-WXYZ"
        val largePayload = ("Clestra E2EE + GZIP compressed message block!\n").repeat(60)
        val rawUtf8Size = largePayload.toByteArray(Charsets.UTF_8).size
        val encrypted = CryptoUtils.encryptPayload(largePayload, senderId, recipientId)
        assertTrue("Expected compressed ciphertext ($encrypted.length) < raw ($rawUtf8Size)", encrypted.length < rawUtf8Size / 2)
        assertEquals(largePayload, CryptoUtils.decryptPayload(encrypted, senderId, recipientId))
    }

    @Test
    fun testBinaryBytesCompressionAndEncryptionRoundTrip() {
        val raw = ("BINARY_FILE_CHUNK_HEADER_0001_").repeat(80).toByteArray(Charsets.UTF_8)
        val sealed = CryptoUtils.encryptBytes(raw, "CLS-AAA1", "CLS-BBB2")
        assertTrue("Expected binary sealed packet smaller than raw", sealed.size < raw.size / 2)
        val opened = CryptoUtils.decryptBytes(sealed, "CLS-AAA1", "CLS-BBB2")
        assertTrue(raw.contentEquals(opened))
    }

    @Test
    fun testEncryptedStickerAndGifTransport() {
        val senderId = "CLS-1234-5678-ABCD"
        val recipientId = "CLS-9999-8888-WXYZ"
        val gifPayload = "GIF::Happy Dance::https://nekos.best/api/v2/dance/example.gif"

        val encrypted = CryptoUtils.encryptPayload(gifPayload, senderId, recipientId)
        assertTrue(encrypted.startsWith("ENC2:"))
        assertEquals(gifPayload, CryptoUtils.decryptPayload(encrypted, senderId, recipientId))
    }

    @Test
    fun testInternalVaultDecoding() {
        assertEquals("https://openrouter.ai/", InternalVault.aiEndpoint())
        assertEquals("openrouter/free", InternalVault.aiModel())
        assertEquals("https://api.giphy.com/", InternalVault.giphyEndpoint())
        assertEquals("https://nekos.best/", InternalVault.nekosEndpoint())
        assertEquals("https://api.otakugifs.xyz/", InternalVault.otakuEndpoint())
        assertEquals("clestra://official-assistant", InternalVault.botAddress())
        assertTrue(InternalVault.kdfSalt().isNotBlank())

        val prompt = InternalVault.buildPrompt("Alice", "CLS-TEST-1234-ABCD")
        assertTrue(prompt.contains("Alice"))
        assertTrue(prompt.contains("CLS-TEST-1234-ABCD"))
        assertTrue(prompt.contains("Clestra"))
    }

    @Test
    fun testMarkdownCodeBlockParsing() {
        val md = """
            Here is a Kotlin sample:
            ```kotlin
            fun greet() = println("Hi Clestra")
            ```
            Done!
        """.trimIndent()
        val blocks = MarkdownParser.parseBlocks(md)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownSegment.TextSegment)
        val codeBlock = blocks[1] as MarkdownSegment.CodeBlockSegment
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.code.contains("fun greet()"))
        assertTrue(blocks[2] is MarkdownSegment.TextSegment)
    }

    @Test
    fun testFileClassificationKinds() {
        assertEquals(ClestraFileKind.APK, FileAttachmentUtils.classifyFileKind("app-release.apk", null))
        assertEquals(ClestraFileKind.CODE_OR_TEXT, FileAttachmentUtils.classifyFileKind("MainActivity.kt", "text/plain"))
        assertEquals(ClestraFileKind.PDF, FileAttachmentUtils.classifyFileKind("report.pdf", "application/pdf"))
        assertEquals(ClestraFileKind.ARCHIVE, FileAttachmentUtils.classifyFileKind("backup.zip", null))
        assertEquals(ClestraFileKind.IMAGE, FileAttachmentUtils.classifyFileKind("photo.jpg", "image/jpeg"))
        assertEquals(ClestraFileKind.AUDIO, FileAttachmentUtils.classifyFileKind("track.mp3", "audio/mpeg"))
        assertEquals(ClestraFileKind.VIDEO, FileAttachmentUtils.classifyFileKind("clip.mp4", "video/mp4"))
        assertEquals(ClestraFileKind.UNKNOWN, FileAttachmentUtils.classifyFileKind("mystery.xyzbin", "application/octet-stream"))
    }

    @Test
    fun testFullKeyboardEmojiCatalog() {
        val total = EmojiCatalog.allEmojis.size
        assertTrue("Expected broad mobile keyboard emoji catalog, got $total", total >= 400)
        assertEquals(10, EmojiCatalog.categories.size)
    }

    @Test
    fun testRedeemVerificationRejectsInvalidCodes() {
        org.junit.Assert.assertNull(InternalVault.verifyRedeemCode(""))
        org.junit.Assert.assertNull(InternalVault.verifyRedeemCode("not-a-valid-secret-code"))
    }

    @Test
    fun testCustomHexTagParsingAndDisplay() {
        val hex6 = com.example.ui.components.CustomTagCodec.parseCustomRedeemTag("tag:OWNER@#FF6B35")
        assertEquals("OWNER|#FF6B35", hex6)
        val (name6, argb6) = com.example.ui.components.CustomTagCodec.parseDisplayTag(hex6!!)
        assertEquals("OWNER", name6)
        assertEquals(0xFFFF6B35L, argb6)

        val hex3 = com.example.ui.components.CustomTagCodec.parseCustomRedeemTag("tag:creator@0bf")
        assertEquals("CREATOR|#00BBFF", hex3)

        val hex8 = com.example.ui.components.CustomTagCodec.parseCustomRedeemTag("tag:Alpha@80112233")
        assertEquals("ALPHA|#80112233", hex8)

        val grad = com.example.ui.components.CustomTagCodec.parseCustomRedeemTag("tag:sunset@#FF6B35-#6C5CE7")
        assertEquals("SUNSET|#FF6B35|#6C5CE7", grad)
        val gradSpec = com.example.ui.components.CustomTagCodec.parseDisplayTagSpec(grad!!)
        assertEquals("SUNSET", gradSpec.name)
        assertEquals(0xFFFF6B35L, gradSpec.primaryArgb)
        assertEquals(0xFF6C5CE7L, gradSpec.secondaryArgb)

        assertTrue(com.example.ui.components.CustomTagCodec.publicStarterTags.size >= 4)
        assertEquals("dev", com.example.ui.components.CustomTagCodec.normalizePeerId(" DEV "))
        assertEquals("CLS-12AB-34CD", com.example.ui.components.CustomTagCodec.normalizePeerId("cls-12ab-34cd"))

        org.junit.Assert.assertNull(com.example.ui.components.CustomTagCodec.parseCustomRedeemTag("tag:INVALID@ZZZZZZ"))
        org.junit.Assert.assertNull(com.example.ui.components.CustomTagCodec.parseCustomRedeemTag("not-a-tag"))
    }

    @Test
    fun testBotActionDirectiveParsingAndStripping() {
        val modelOutput = """
            Done! I sent that message and muted notifications for 1 hour.
            [[CLESTRA_ACTION:{"action":"send_message","peer":"Alex","text":"On my way!"}]]
            [[CLESTRA_ACTION:{"action":"mute_peer","peer":"Alex","minutes":60}]]
        """.trimIndent()

        val parsed = com.example.data.repository.ClestraBotActionEngine.parseModelResponse(modelOutput)
        assertEquals("Done! I sent that message and muted notifications for 1 hour.", parsed.visibleReply)
        assertEquals(2, parsed.actions.size)
        assertEquals("send_message", parsed.actions[0].action)
        assertEquals("Alex", parsed.actions[0].peer)
        assertEquals("On my way!", parsed.actions[0].text)
        assertEquals("mute_peer", parsed.actions[1].action)
        assertEquals(60L, parsed.actions[1].minutes)
    }

    @Test
    fun testOfflineBotCommandParsing() {
        val sendCmd = com.example.data.repository.ClestraBotActionEngine.parseOfflineCommand("send Hello there to Alex")
        assertNotNull(sendCmd)
        assertEquals("send_message", sendCmd!!.second.first().action)
        assertEquals("Alex", sendCmd.second.first().peer)
        assertEquals("Hello there", sendCmd.second.first().text)

        val blockCmd = com.example.data.repository.ClestraBotActionEngine.parseOfflineCommand("block Bob")
        assertEquals("block_peer", blockCmd!!.second.first().action)
        assertEquals("Bob", blockCmd.second.first().peer)

        val muteCmd = com.example.data.repository.ClestraBotActionEngine.parseOfflineCommand("mute Alex for 2h")
        assertEquals("mute_peer", muteCmd!!.second.first().action)
        assertEquals(120L, muteCmd.second.first().minutes)

        val redeemCmd = com.example.data.repository.ClestraBotActionEngine.parseOfflineCommand("redeem tag:VIP@#00C896")
        assertEquals("redeem_tag", redeemCmd!!.second.first().action)
        assertEquals("tag:VIP@#00C896", redeemCmd.second.first().code)
    }

    @Test
    fun testUpdatedInternalVaultPromptContainsActionRules() {
        val prompt = InternalVault.buildPrompt("Creator", "CLS-1111-2222-3333")
        assertTrue(prompt.contains("CLESTRA_ACTION"))
        assertTrue(prompt.contains("block_peer"))
        assertTrue(prompt.contains("send_message"))
        assertFalse(prompt.lowercase().contains("openrouter"))
    }

    @Test
    fun testAudioMetadataEncodingParsingAndDurationFormat() {
        val sampleWaveform = listOf(10, 45, 92, 66, 30, 80, 100, 40)
        val encoded = com.example.ui.components.AudioMetaCodec.encodeMime(
            baseMime = "audio/mp4",
            durationMs = 14_200L,
            waveform = sampleWaveform
        )
        assertTrue(encoded.startsWith("audio/mp4;dur=14200;wf="))

        val parsed = com.example.ui.components.AudioMetaCodec.parseMeta(encoded)
        assertEquals("audio/mp4", parsed.baseMime)
        assertEquals(14_200L, parsed.durationMs)
        assertEquals(24, parsed.waveform.size)
        assertTrue(parsed.waveform.all { it in 12..100 })

        assertEquals("0:00", com.example.ui.components.AudioMetaCodec.formatDuration(0L))
        assertEquals("0:14", com.example.ui.components.AudioMetaCodec.formatDuration(14_200L))
        assertEquals("1:05", com.example.ui.components.AudioMetaCodec.formatDuration(65_000L))
    }

    @Test
    fun testWavAndCompressedAudioWaveformExtraction() {
        // Synthetic RIFF/WAVE header + 16-bit PCM signal bytes
        val wavBytes = ByteArray(1024)
        "RIFF".toByteArray().copyInto(wavBytes, 0)
        "WAVE".toByteArray().copyInto(wavBytes, 8)
        for (i in 44 until wavBytes.size step 2) {
            val amp = ((i * 37) % 12000).toShort()
            wavBytes[i] = (amp.toInt() and 0xFF).toByte()
            wavBytes[i + 1] = ((amp.toInt() ushr 8) and 0xFF).toByte()
        }
        val wavBars = com.example.ui.components.AudioMetaCodec.extractEnergyWaveformFromBytes(wavBytes, bars = 24)
        assertEquals(24, wavBars.size)
        assertTrue(wavBars.all { it in 16..100 })

        // Compressed audio byte fallback (e.g., MP3 frames)
        val mp3Bytes = ByteArray(1024) { idx -> ((idx * 19 + idx * idx) and 0xFF).toByte() }
        val mp3Bars = com.example.ui.components.AudioMetaCodec.extractEnergyWaveformFromBytes(mp3Bytes, bars = 24)
        assertEquals(24, mp3Bars.size)
        assertTrue(mp3Bars.all { it in 16..100 })
    }

    @Test
    fun testVoicePayloadAndSoftUiCssPaletteTokens() {
        val payload = "VOICE::Voice (0:09)::gzb64:H4sIAAAAAAAA/8tIzcnJBwCGphA2BQAAAA==::audio/mp4;dur=9000;wf=20,50,80::4096"
        val sealed = CryptoUtils.encryptPayload(payload, "dev", "CLS-1111-2222-3333")
        assertTrue(sealed.startsWith("ENC2:"))
        assertEquals(payload, CryptoUtils.decryptPayload(sealed, "dev", "CLS-1111-2222-3333"))

        assertEquals(androidx.compose.ui.graphics.Color(0xFFECF0F3), com.example.ui.theme.ClestraCream)
        assertEquals(70, kotlin.math.round(com.example.ui.theme.ClestraTextSoft.red * 255f).toInt())
        assertEquals(70, kotlin.math.round(com.example.ui.theme.ClestraTextSoft.green * 255f).toInt())
        assertEquals(70, kotlin.math.round(com.example.ui.theme.ClestraTextSoft.blue * 255f).toInt())
    }
}
