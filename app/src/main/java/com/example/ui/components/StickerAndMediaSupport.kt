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
        BuiltInStickerMeta("stk_builtin_lock", "builtin://clestra_lock", "Private Lock Safe", "🔐", "SAFE", 0xFF2C2018),
        BuiltInStickerMeta("stk_builtin_bolt", "builtin://clestra_bolt", "Fast P2P Direct Bolt", "⚡", "FAST!", 0xFFC4763A),
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

/**
 * Full standard mobile-keyboard Emoji catalog spanning Smileys, People, Animals, Food,
 * Travel, Activities, Objects, Symbols, and Flags (over 1,000 searchable Unicode emojis).
 */
object EmojiCatalog {
    data class EmojiEntry(
        val symbol: String,
        val keywords: String,
        val category: String
    )

    val categories = listOf(
        "All",
        "Smileys",
        "People",
        "Animals",
        "Food",
        "Travel",
        "Activities",
        "Objects",
        "Symbols",
        "Flags"
    )

    private val keywordOverrides = mapOf(
        "😀" to "grin happy smile face",
        "😃" to "happy big smile",
        "😄" to "laugh smile joy",
        "😁" to "beam grin teeth",
        "😆" to "haha lol xd laugh",
        "😅" to "sweat nervous relief",
        "🤣" to "rofl lol funny laughing",
        "😂" to "lol tears joy funny",
        "🙂" to "slight smile ok",
        "🙃" to "upside down silly",
        "😉" to "wink flirt",
        "😊" to "blush warm happy cute",
        "😇" to "angel halo innocent",
        "🥰" to "love hearts adore",
        "😍" to "heart eyes love crush",
        "🤩" to "star struck excited wow",
        "😘" to "kiss love heart",
        "😜" to "wink tongue playful",
        "🤪" to "crazy goofy wild",
        "😎" to "cool sunglasses chill",
        "🥳" to "party birthday celebrate",
        "🤔" to "think wonder hmm",
        "🤫" to "quiet shh secret",
        "🤗" to "hug cozy",
        "😭" to "cry sob sad tears",
        "😢" to "sad tear",
        "😡" to "angry mad rage",
        "🤬" to "angry swear furious",
        "🤯" to "mind blown shock wow",
        "😴" to "sleep zzz tired",
        "🤮" to "sick vomit",
        "🤧" to "sneeze cold",
        "🫡" to "salute respect yes",
        "🫠" to "melting shy",
        "❤️" to "red heart love",
        "🧡" to "orange copper heart clestra",
        "💛" to "yellow gold heart",
        "💚" to "green heart",
        "💙" to "blue heart",
        "🩵" to "light blue heart",
        "💜" to "purple heart",
        "🖤" to "black heart",
        "🤍" to "white heart",
        "🤎" to "brown heart",
        "💔" to "broken heart sad",
        "💖" to "sparkle heart love",
        "💕" to "two hearts love",
        "💯" to "100 percent facts",
        "👋" to "wave hi hello bye",
        "👍" to "thumbs up yes ok good like",
        "👎" to "thumbs down no dislike",
        "👏" to "clap applause bravo",
        "🙌" to "celebrate hands yay",
        "🤝" to "handshake deal agree",
        "🙏" to "please thanks pray",
        "✌️" to "peace two victory",
        "🤞" to "luck crossed fingers",
        "🫶" to "heart hands love",
        "💪" to "muscle strong flex workout",
        "🔥" to "fire lit hot flame",
        "⚡" to "bolt lightning fast electric",
        "✨" to "sparkles magic shine",
        "🎉" to "party popper celebrate confetti",
        "🚀" to "rocket launch space",
        "🍑" to "peach fruit clestra",
        "☕" to "coffee tea morning espresso",
        "🍕" to "pizza food",
        "🍔" to "burger food",
        "🔐" to "lock key security encrypted",
        "💻" to "laptop computer code programming",
        "📱" to "phone mobile device"
    )

    private fun cp(codePoint: Int): String = String(Character.toChars(codePoint))

    private fun expandRange(start: Int, end: Int, category: String, tag: String): List<EmojiEntry> {
        val list = ArrayList<EmojiEntry>(end - start + 1)
        for (c in start..end) {
            if (!Character.isDefined(c)) continue
            val sym = cp(c)
            val nameHint = runCatching { Character.getName(c)?.lowercase() ?: "" }.getOrDefault("")
            val kw = buildString {
                keywordOverrides[sym]?.let { append(it).append(' ') }
                append(nameHint).append(' ').append(tag).append(' ').append(category)
            }
            list.add(EmojiEntry(sym, kw, category))
        }
        return list
    }

    private fun listFromSymbols(category: String, tag: String, symbols: List<String>): List<EmojiEntry> {
        return symbols.map { sym ->
            val firstCp = sym.codePointAt(0)
            val nameHint = runCatching { Character.getName(firstCp)?.lowercase() ?: "" }.getOrDefault("")
            val kw = buildString {
                keywordOverrides[sym]?.let { append(it).append(' ') }
                append(nameHint).append(' ').append(tag).append(' ').append(category)
            }
            EmojiEntry(sym, kw, category)
        }
    }

    val allEmojis: List<EmojiEntry> by lazy {
        buildList {
            // Smileys & Emotion
            addAll(expandRange(0x1F600, 0x1F644, "Smileys", "face emotion expression"))
            addAll(expandRange(0x1F910, 0x1F92F, "Smileys", "face emotion expression"))
            addAll(expandRange(0x1F970, 0x1F97A, "Smileys", "face love party emotion"))
            addAll(
                listFromSymbols(
                    "Smileys",
                    "face heart emotion",
                    listOf(
                        "🫠", "🫢", "🫣", "🫡", "🫥", "🫤", "🥹", "😮‍💨", "😵‍💫", "😶‍🌫️",
                        "😈", "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖",
                        "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾",
                        "🙈", "🙉", "🙊",
                        "💋", "💌", "💘", "💝", "💖", "💗", "💓", "💞", "💕", "💟", "❣️", "💔",
                        "❤️‍🔥", "❤️‍🩹", "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜", "🤎", "🖤", "🩶", "🤍",
                        "💯", "💢", "💥", "💫", "💦", "💨", "🕳️", "💬", "👁️‍🗨️", "🗨️", "🗯️", "💭", "💤"
                    )
                )
            )

            // People & Hands & Body
            addAll(
                listFromSymbols(
                    "People",
                    "hand gesture body people person",
                    listOf(
                        "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌", "🤌", "🤏",
                        "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "🫵",
                        "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏",
                        "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅", "👄", "🫦",
                        "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩", "🧓", "👴", "👵",
                        "🙍", "🙎", "🙅", "🙆", "💁", "🙋", "🧏", "🙇", "🤦", "🤷",
                        "👮", "🕵️", "💂", "🥷", "👷", "🫅", "🤴", "👸", "👳", "👲", "🧕", "🤵", "👰", "🤰", "🤱", "👼", "🎅", "🤶", "🦸", "🦹", "🧙", "🧚", "🧛", "🧜", "🧝", "🧞", "🧟",
                        "💆", "💇", "🚶", "🧍", "🧎", "🏃", "💃", "🕺", "🕴️", "👯", "🧖", "🧗",
                        "🫂", "👥", "👤", "🗣️", "👣"
                    )
                )
            )

            // Animals & Nature
            addAll(expandRange(0x1F400, 0x1F43E, "Animals", "animal pet wildlife nature"))
            addAll(expandRange(0x1F980, 0x1F9AE, "Animals", "animal bird bug wildlife"))
            addAll(
                listFromSymbols(
                    "Animals",
                    "plant flower weather sky nature",
                    listOf(
                        "💐", "🌸", "💮", "🪷", "🏵️", "🌹", "🥀", "🌺", "🌻", "🌼", "🌷", "🪻",
                        "🌱", "🪴", "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "☘️", "🍀", "🍁", "🍂", "🍃", "🪹", "🪺", "🍄",
                        "🪨", "🌍", "🌎", "🌏", "🌐", "🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘", "🌙", "🌚", "🌛", "🌜", "☀️", "🌝", "🌞", "⭐", "🌟", "🌠", "☁️", "⛅", "⛈️", "🌤️", "🌥️", "🌦️", "🌧️", "🌨️", "🌩️", "🌪️", "🌫️", "🌬️", "🌀", "🌈", "🌂", "☂️", "☔", "⚡", "❄️", "☃️", "⛄", "☄️", "🔥", "💧", "🌊"
                    )
                )
            )

            // Food & Drink
            addAll(expandRange(0x1F345, 0x1F37F, "Food", "food meal fruit drink snack"))
            addAll(expandRange(0x1F950, 0x1F96F, "Food", "food meal drink breakfast"))
            addAll(expandRange(0x1F9C0, 0x1F9CB, "Food", "food drink sweet"))
            addAll(
                listFromSymbols(
                    "Food",
                    "fruit vegetable drink dessert",
                    listOf(
                        "🍇", "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🥭", "🍎", "🍏", "🍐", "🍑", "🍒", "🍓", "🫐", "🥝", "🫒", "🥥",
                        "🫓", "🫔", "🫕", "🫖", "🧋", "🫘", "🫗", "🫙", "🍽️", "🍴", "🥄", "🔪", "🥢", "🧂"
                    )
                )
            )

            // Travel & Places
            addAll(expandRange(0x1F680, 0x1F6C5, "Travel", "travel transport car plane rocket"))
            addAll(
                listFromSymbols(
                    "Travel",
                    "building map city nature place",
                    listOf(
                        "🗺️", "🧭", "🏔️", "⛰️", "🌋", "🗻", "🏕️", "🏖️", "🏜️", "🏝️", "🏞️", "🏟️", "🏛️", "🏗️", "🧱", "🪨", "🪵", "🛖",
                        "🏘️", "🏚️", "🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰", "💒", "🗼", "🗽",
                        "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋", "⛲", "⛺", "🌁", "🌃", "🏙️", "🌄", "🌅", "🌆", "🌇", "🌉", "♨️", "🎠", "🛝", "🎡", "🎢",
                        "🛢️", "⛽", "🛞", "🚨", "🚥", "🚦", "🛑", "🚧", "⚓", "🛟", "⛵", "🛶", "🚤", "🛳️", "⛴️", "🛥️", "✈️", "🛩️", "🛫", "🛬", "🪂", "💺", "🚁", "🚟", "🚠", "🚡", "🛰️", "🛸",
                        "🛎️", "🧳", "⌛", "⏳", "⌚", "⏰", "⏱️", "⏲️", "🕰️"
                    )
                )
            )

            // Activities & Sports & Arts
            addAll(expandRange(0x1F3A0, 0x1F3CA, "Activities", "sport game music art fun"))
            addAll(
                listFromSymbols(
                    "Activities",
                    "celebration sport music trophy game",
                    listOf(
                        "🎃", "🎄", "🎆", "🎇", "🧨", "✨", "🎈", "🎉", "🎊", "🎋", "🎍", "🎎", "🎏", "🎐", "🎑", "🧧", "🎀", "🎁", "🎗️", "🎟️", "🎫",
                        "🎖️", "🏆", "🏅", "🥇", "🥈", "🥉", "⚽", "⚾", "🥎", "🏀", "🏐", "🏈", "🏉", "🎾", "🥏", "🎳", "🏏", "🏑", "🏒", "🥍", "🏓", "🏸", "🥊", "🥋", "🥅", "⛳", "⛸️", "🎣", "🤿", "🎽", "🎿", "🛷", "🥌",
                        "🎯", "🪀", "🪁", "🎱", "🔮", "🪄", "🎮", "🕹️", "🎰", "🎲", "🧩", "🧸", "🪅", "🪩", "🪆", "♠️", "♥️", "♦️", "♣️", "♟️", "🃏", "🀄", "🎴",
                        "🎭", "🖼️", "🎨", "🧵", "🪡", "🧶", "🪢"
                    )
                )
            )

            // Objects & Tech
            addAll(expandRange(0x1F4BB, 0x1F4FF, "Objects", "tech computer phone office tool"))
            addAll(expandRange(0x1F50D, 0x1F52E, "Objects", "search lock key fire tool"))
            addAll(
                listFromSymbols(
                    "Objects",
                    "clothing tech book tool home",
                    listOf(
                        "👓", "🕶️", "🥽", "🥼", "🦺", "👔", "👕", "👖", "🧣", "🧤", "🧥", "🧦", "👗", "👘", "🥻", "🩱", "🩲", "🩳", "👙", "👚", "👛", "👜", "👝", "🛍️", "🎒", "🩴", "👞", "👟", "🥾", "🥿", "👠", "👡", "🩰", "👢", "👑", "👒", "🎩", "🎓", "🧢", "🪖", "⛑️", "💍", "💎",
                        "🔇", "🔈", "🔉", "🔊", "📢", "📣", "📯", "🔔", "🔕", "🎼", "🎵", "🎶", "🎙️", "🎚️", "🎛️", "🎤", "🎧", "📻", "🎷", "🪗", "🎸", "🎹", "🎺", "🎻", "🪕", "🥁", "🪘",
                        "📱", "📲", "☎️", "📞", "📟", "📠", "🔋", "🪫", "🔌", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "🖲️", "💽", "💾", "💿", "📀", "🧮", "🎥", "🎞️", "📽️", "🎬", "📺", "📷", "📸", "📹", "📼",
                        "🕯️", "💡", "🔦", "🏮", "🪔", "📔", "📕", "📖", "📗", "📘", "📙", "📚", "📓", "📒", "📃", "📜", "📄", "📰", "🗞️", "📑", "🔖", "🏷️", "💰", "🪙", "💴", "💵", "💶", "💷", "💸", "💳", "🧾",
                        "✉️", "📧", "📨", "📩", "📤", "📥", "📦", "📫", "📪", "📬", "📭", "📮", "🗳️", "✏️", "✒️", "🖋️", "🖊️", "🖌️", "🖍️", "📝", "💼", "📁", "📂", "🗂️", "📅", "📆", "🗒️", "🗓️", "📇", "📈", "📉", "📊", "📋", "📌", "📍", "📎", "🖇️", "📏", "📐", "✂️", "🗃️", "🗄️", "🗑️",
                        "🔒", "🔓", "🔏", "🔐", "🔑", "🗝️", "🔨", "🪓", "⛏️", "⚒️", "🛠️", "🗡️", "⚔️", "💣", "🪃", "🏹", "🛡️", "🪚", "🔧", "🪛", "🔩", "⚙️", "🗜️", "⚖️", "🦯", "🔗", "⛓️", "🪝", "🧰", "🧲", "🪜",
                        "⚗️", "🧪", "🧫", "🧬", "🔬", "🔭", "📡", "💉", "🩸", "💊", "🩹", "🩺", "🩻", "🚪", "🛗", "🪞", "🪟", "🛏️", "🛋️", "🪑", "🚽", "🪠", "🚿", "🛁", "🪤", "🪒", "🧴", "🧷", "🧹", "🧺", "🧻", "🪣", "🧼", "🫧", "🪥", "🧽", "🧯", "🛒"
                    )
                )
            )

            // Symbols & Signs & Zodiac & Clocks
            addAll(
                listFromSymbols(
                    "Symbols",
                    "symbol arrow sign check math clock zodiac",
                    listOf(
                        "💘", "❤️", "🆘", "🆔", "⚛️", "🛐", "🕉️", "✡️", "☸️", "☯️", "✝️", "☦️", "☪️", "☮️", "🕎", "🔯", "🪯",
                        "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓", "⛎",
                        "🔀", "🔁", "🔂", "▶️", "⏩", "⏭️", "⏯️", "◀️", "⏪", "⏮️", "🔼", "⏫", "🔽", "⏬", "⏸️", "⏹️", "⏺️", "⏏️", "🎦", "🔅", "🔆", "📶", "📳", "📴",
                        "♀️", "♂️", "⚧️", "✖️", "➕", "➖", "➗", "🟰", "♾️", "‼️", "⁉️", "❓", "❔", "❕", "❗", "〰️", "💱", "💲",
                        "⚕️", "♻️", "⚜️", "🔱", "📛", "🔰", "⭕", "✅", "☑️", "✔️", "❌", "❎", "➰", "➿", "〽️", "✳️", "✴️", "❇️", "©️", "®️", "™️",
                        "#️⃣", "*️⃣", "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟",
                        "🔠", "🔡", "🔢", "🔣", "🔤", "🅰️", "🆎", "🅱️", "🆑", "🆒", "🆓", "ℹ️", "🆔", "Ⓜ️", "🆕", "🆖", "🅾️", "🆗", "🅿️", "🆘", "🆙", "🆚",
                        "🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "🟤", "⚫", "⚪", "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "🟫", "⬛", "⬜", "◼️", "◻️", "◾", "◽", "▪️", "▫️", "🔶", "🔷", "🔸", "🔹", "🔺", "🔻", "💠", "🔘", "🔳", "🔲",
                        "⬆️", "↗️", "➡️", "↘️", "⬇️", "↙️", "⬅️", "↖️", "↕️", "↔️", "↩️", "↪️", "⤴️", "⤵️", "🔃", "🔄", "🔙", "🔚", "🔛", "🔜", "🔝",
                        "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛", "🕜", "🕝", "🕞", "🕟", "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦", "🕧"
                    )
                )
            )

            // Flags
            addAll(
                listFromSymbols(
                    "Flags",
                    "flag country world nation",
                    listOf(
                        "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️",
                        "🇺🇸", "🇬🇧", "🇨🇦", "🇦🇺", "🇩🇪", "🇫🇷", "🇪🇸", "🇮🇹", "🇯🇵", "🇰🇷", "🇮🇳", "🇧🇷", "🇲🇽", "🇨🇳",
                        "🇷🇺", "🇺🇦", "🇵🇱", "🇳🇱", "🇸🇪", "🇳🇴", "🇩🇰", "🇫🇮", "🇨🇭", "🇦🇹", "🇧🇪", "🇵🇹", "🇮🇪", "🇬🇷",
                        "🇹🇷", "🇸🇦", "🇦🇪", "🇪🇬", "🇿🇦", "🇳🇬", "🇰🇪", "🇦🇷", "🇨🇴", "🇨🇱", "🇵🇪", "🇻🇪",
                        "🇮🇩", "🇲🇾", "🇸🇬", "🇹🇭", "🇻🇳", "🇵🇭", "🇵🇰", "🇧🇩", "🇱🇰", "🇳🇵", "🇳🇿", "🇮🇱"
                    )
                )
            )
        }.distinctBy { it.symbol }
    }
}
