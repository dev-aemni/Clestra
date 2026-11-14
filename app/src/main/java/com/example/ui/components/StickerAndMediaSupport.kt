package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.data.model.CustomStickerEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object CoilGifSupport {
    @Volatile
    private var cachedLoader: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return cachedLoader ?: synchronized(this) {
            cachedLoader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .crossfade(true)
                .build()
                .also { cachedLoader = it }
        }
    }
}

object StickerStorageUtils {

    data class BuiltInStickerMeta(
        val id: String,
        val uri: String,
        val label: String,
        val emoji: String,
        val caption: String,
        val bgHex: Long
    )

    val builtInStickers = listOf(
        BuiltInStickerMeta("stk_builtin_wave", "builtin://clestra_wave", "Wave Hello Hi", "👋", "HEY!", 0xFFF5D5B8),
        BuiltInStickerMeta("stk_builtin_love", "builtin://clestra_love", "Love Heart Peach", "🧡", "LOVE", 0xFFFFE0CE),
        BuiltInStickerMeta("stk_builtin_lock", "builtin://clestra_lock", "Encrypted Lock Safe", "🔐", "E2EE", 0xFF2C2018),
        BuiltInStickerMeta("stk_builtin_bolt", "builtin://clestra_bolt", "Fast P2P Direct Bolt", "⚡", "P2P!", 0xFFC4763A),
        BuiltInStickerMeta("stk_builtin_party", "builtin://clestra_party", "Party Celebrate Congrats", "🎉", "WOO!", 0xFFF2EAE0),
        BuiltInStickerMeta("stk_builtin_lol", "builtin://clestra_lol", "Laugh Funny LOL", "😂", "LOL", 0xFFFFE8B8),
        BuiltInStickerMeta("stk_builtin_coffee", "builtin://clestra_coffee", "Coffee Morning Vibe", "☕", "CHILL", 0xFFE8D5C0),
        BuiltInStickerMeta("stk_builtin_fire", "builtin://clestra_fire", "Fire Lit Hot", "🔥", "LIT!", 0xFFFFD8C2),
        BuiltInStickerMeta("stk_builtin_star", "builtin://clestra_star", "Star Super Great", "⭐", "NICE!", 0xFFFDF0C8),
        BuiltInStickerMeta("stk_builtin_hug", "builtin://clestra_hug", "Hug Warm Cozy", "🤗", "HUG", 0xFFF5D5B8),
        BuiltInStickerMeta("stk_builtin_100", "builtin://clestra_100", "100 Agree Facts", "💯", "100%", 0xFFE8D5C0),
        BuiltInStickerMeta("stk_builtin_peach", "builtin://clestra_peach", "Clestra Peach Mascot", "🍑", "CLESTRA", 0xFFFFDFC8)
    )

    fun defaultStickerEntities(): List<CustomStickerEntity> {
        val now = System.currentTimeMillis()
        return builtInStickers.mapIndexed { idx, meta ->
            CustomStickerEntity(
                id = meta.id,
                label = meta.label,
                uri = meta.uri,
                packName = "Clestra Pack",
                isCustom = false,
                styleTag = "BADGE",
                createdAt = now - idx
            )
        }
    }

    fun findBuiltInMeta(uri: String?): BuiltInStickerMeta? {
        if (uri == null || !uri.startsWith("builtin://")) return null
        return builtInStickers.firstOrNull { it.uri == uri }
    }

    /**
     * Decodes a picked image URI, center-crops it to 512x512, applies the selected sticker cutout/border style,
     * and saves it as a local PNG file so it persists indefinitely without URI permission loss.
     */
    fun createCustomStickerFile(
        context: Context,
        sourceUri: Uri,
        styleTag: String = "OUTLINE"
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val decoded = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null

            val targetSize = 512
            val srcSize = minOf(decoded.width, decoded.height)
            val startX = (decoded.width - srcSize) / 2
            val startY = (decoded.height - srcSize) / 2
            val square = Bitmap.createBitmap(decoded, startX, startY, srcSize, srcSize)
            val scaled = Bitmap.createScaledBitmap(square, targetSize, targetSize, true)

            val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val pad = if (styleTag == "CLEAN") 8f else 22f
            val corner = 96f
            val rect = RectF(pad, pad, targetSize - pad, targetSize - pad)

            if (styleTag == "OUTLINE" || styleTag == "BADGE") {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = if (styleTag == "BADGE") android.graphics.Color.parseColor("#C4763A") else android.graphics.Color.WHITE
                }
                val outerRect = RectF(4f, 4f, targetSize - 4f, targetSize - 4f)
                canvas.drawRoundRect(outerRect, corner + 12f, corner + 12f, borderPaint)

                if (styleTag == "BADGE") {
                    val innerRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = android.graphics.Color.WHITE
                    }
                    val midRect = RectF(14f, 14f, targetSize - 14f, targetSize - 14f)
                    canvas.drawRoundRect(midRect, corner + 6f, corner + 6f, innerRim)
                }
            }

            val clipPath = Path().apply {
                addRoundRect(rect, corner, corner, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(scaled, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()

            if (square != decoded) square.recycle()
            if (scaled != square) scaled.recycle()
            decoded.recycle()

            val stickersDir = File(context.filesDir, "stickers").apply { mkdirs() }
            val outFile = File(stickersDir, "stk_${UUID.randomUUID()}.png")
            FileOutputStream(outFile).use { fos ->
                output.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            output.recycle()

            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        }
    }
}

object EmojiCatalog {
    data class EmojiEntry(
        val symbol: String,
        val keywords: String,
        val category: String
    )

    val categories = listOf(
        "All",
        "Smileys",
        "Hearts",
        "Gestures",
        "Party",
        "Nature",
        "Food"
    )

    val allEmojis = listOf(
        // Smileys
        EmojiEntry("😀", "grin happy smile face", "Smileys"),
        EmojiEntry("😂", "laugh cry funny lol joy", "Smileys"),
        EmojiEntry("🥹", "teary happy proud cute", "Smileys"),
        EmojiEntry("😊", "blush warm smile happy", "Smileys"),
        EmojiEntry("😍", "heart eyes love crush", "Smileys"),
        EmojiEntry("🤩", "star struck wow excited", "Smileys"),
        EmojiEntry("😘", "kiss love blow", "Smileys"),
        EmojiEntry("😜", "wink tongue silly playful", "Smileys"),
        EmojiEntry("😎", "cool sunglasses chill", "Smileys"),
        EmojiEntry("🥳", "party celebrate birthday", "Smileys"),
        EmojiEntry("🤔", "think hmm wonder", "Smileys"),
        EmojiEntry("🤫", "shh secret quiet", "Smileys"),
        EmojiEntry("🤗", "hug warm cuddle", "Smileys"),
        EmojiEntry("😴", "sleep tired night zzz", "Smileys"),
        EmojiEntry("😭", "cry sad sob", "Smileys"),
        EmojiEntry("🤯", "mind blown shock wow", "Smileys"),
        EmojiEntry("😇", "angel innocent halo", "Smileys"),
        EmojiEntry("😏", "smirk sly", "Smileys"),
        EmojiEntry("🫡", "salute respect yes", "Smileys"),
        EmojiEntry("🫠", "melt shy oops", "Smileys"),

        // Hearts
        EmojiEntry("❤️", "red heart love", "Hearts"),
        EmojiEntry("🧡", "orange copper heart clestra", "Hearts"),
        EmojiEntry("💛", "yellow heart gold", "Hearts"),
        EmojiEntry("💚", "green heart", "Hearts"),
        EmojiEntry("🩵", "blue heart light", "Hearts"),
        EmojiEntry("💜", "purple heart", "Hearts"),
        EmojiEntry("🤎", "brown heart warm", "Hearts"),
        EmojiEntry("🤍", "white heart pure", "Hearts"),
        EmojiEntry("💖", "sparkle heart love", "Hearts"),
        EmojiEntry("💕", "two hearts love", "Hearts"),
        EmojiEntry("🫶", "heart hands love", "Hearts"),
        EmojiEntry("💯", "100 percent facts real", "Hearts"),

        // Gestures
        EmojiEntry("👋", "wave hi hello bye", "Gestures"),
        EmojiEntry("👍", "thumbs up yes ok good", "Gestures"),
        EmojiEntry("👎", "thumbs down no bad", "Gestures"),
        EmojiEntry("👏", "clap applause bravo", "Gestures"),
        EmojiEntry("🙌", "hands up praise yay", "Gestures"),
        EmojiEntry("🤝", "handshake deal agree", "Gestures"),
        EmojiEntry("🙏", "please thanks pray namaste", "Gestures"),
        EmojiEntry("✌️", "peace victory two", "Gestures"),
        EmojiEntry("🤞", "fingers crossed luck", "Gestures"),
        EmojiEntry("💪", "muscle strong flex", "Gestures"),
        EmojiEntry("👀", "eyes look see watch", "Gestures"),
        EmojiEntry("✍️", "write note sign", "Gestures"),

        // Party & Tech
        EmojiEntry("🎉", "party popper celebrate", "Party"),
        EmojiEntry("🎊", "confetti celebration", "Party"),
        EmojiEntry("🔥", "fire lit hot flame", "Party"),
        EmojiEntry("⚡", "bolt lightning fast p2p", "Party"),
        EmojiEntry("✨", "sparkles magic clean", "Party"),
        EmojiEntry("🔐", "lock key encrypted secure", "Party"),
        EmojiEntry("🚀", "rocket launch fast", "Party"),
        EmojiEntry("🎵", "music note song", "Party"),
        EmojiEntry("🏆", "trophy win champion", "Party"),
        EmojiEntry("🎯", "target bullseye goal", "Party"),
        EmojiEntry("💬", "chat bubble message", "Party"),
        EmojiEntry("📸", "camera photo pic", "Party"),

        // Nature
        EmojiEntry("🍑", "peach clestra fruit", "Nature"),
        EmojiEntry("🌸", "cherry blossom flower", "Nature"),
        EmojiEntry("🌻", "sunflower sun", "Nature"),
        EmojiEntry("🌿", "leaf plant green", "Nature"),
        EmojiEntry("🌈", "rainbow", "Nature"),
        EmojiEntry("☀️", "sun bright sunny", "Nature"),
        EmojiEntry("🌙", "moon night crescent", "Nature"),
        EmojiEntry("⭐", "star favorite", "Nature"),
        EmojiEntry("🐱", "cat kitty meow", "Nature"),
        EmojiEntry("🐶", "dog puppy woof", "Nature"),
        EmojiEntry("🦋", "butterfly", "Nature"),
        EmojiEntry("🐾", "paw pet", "Nature"),

        // Food
        EmojiEntry("☕", "coffee espresso morning", "Food"),
        EmojiEntry("🧋", "boba tea bubble", "Food"),
        EmojiEntry("🍕", "pizza slice", "Food"),
        EmojiEntry("🍔", "burger food", "Food"),
        EmojiEntry("🍩", "donut sweet", "Food"),
        EmojiEntry("🍰", "cake dessert birthday", "Food"),
        EmojiEntry("🍓", "strawberry berry", "Food"),
        EmojiEntry("🥑", "avocado", "Food"),
        EmojiEntry("🍜", "ramen noodles", "Food"),
        EmojiEntry("🍪", "cookie snack", "Food")
    )
}
