package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft

@Composable
fun ClestraPillInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisible: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    containerColor: Color = ClestraNeuWell
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .neumorphicPressed(
                cornerRadius = 16.dp,
                containerColor = containerColor
            )
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = hint,
                    color = ClestraTextSoft,
                    fontSize = 15.sp
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType
            ),
            trailingIcon = if (isPassword && onTogglePasswordVisible != null) {
                {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = ClestraTextSoft
                        )
                    }
                }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = ClestraCharcoal,
                unfocusedTextColor = ClestraCharcoal,
                cursorColor = ClestraCopper
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        )
    }
}

@Composable
fun PeerAvatar(
    name: String,
    avatarColorHex: Long,
    isOnline: Boolean,
    avatarIcon: String = "🍑",
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .testTag("avatar_$name")
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .neumorphicRaised(
                    cornerRadius = 16.dp,
                    elevation = 4.dp,
                    containerColor = ClestraBubbleSent
                )
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val initial = name.firstOrNull()?.uppercase() ?: "?"
            Text(
                text = initial,
                color = ClestraCharcoal,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }

        val statusColor = if (isOnline) ClestraOnline else ClestraCopper
        val dotSize = size * 0.26f

        Box(
            modifier = Modifier
                .size(dotSize)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(statusColor)
                .border(2.dp, ClestraCream, CircleShape)
        )
    }
}

@Composable
fun RouteBadge(
    deliveryRoute: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val isP2P = deliveryRoute == "P2P_DIRECT"
    val bgColor = if (isP2P) ClestraOnline.copy(alpha = 0.14f) else ClestraNeuWell
    val contentColor = if (isP2P) ClestraOnline else ClestraCopperDark
    val label = if (isP2P) "P2P Direct" else "Relay Store"
    val icon = if (isP2P) Icons.Default.FlashOn else Icons.Default.Cloud

    Box(
        modifier = modifier
            .neumorphicPressed(cornerRadius = 12.dp, containerColor = bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
            )
            if (!isCompact) {
                Text(
                    text = " $label",
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RouteToggleChip(
    isOnline: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isOnline) ClestraNeuSurface else ClestraBubbleSent
    val fg = if (isOnline) ClestraOnline else ClestraCopperDark
    val text = if (isOnline) "P2P Online" else "Relay Mode"

    Box(
        modifier = modifier
            .neumorphicRaised(
                cornerRadius = 14.dp,
                elevation = 3.dp,
                containerColor = bg,
                accentBorderColor = if (isOnline) ClestraOnline.copy(alpha = 0.45f) else ClestraCopper.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("route_toggle_chip")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SoftUiBadgeTag(
    label: String,
    modifier: Modifier = Modifier
) {
    val spec = CustomTagCodec.parseDisplayTagSpec(label)
    val displayName = spec.name

    val defaultPalette: Triple<Color, Color?, Color> = when (displayName) {
        "DEV" -> Triple(Color(0xFFBDF7CA), Color(0xFF8BE8A2), Color(0xFF0E2914))
        "OFFICIAL" -> Triple(Color(0xFFFFFFFF), null, Color(0xFF14110F))
        "BOT" -> Triple(Color(0xFFEEF3FB), null, Color(0xFF1A2538))
        "VIP", "FOUNDER" -> Triple(Color(0xFFFFF4DC), Color(0xFFF6DFA9), Color(0xFF2B1C0E))
        "PIONEER" -> Triple(Color(0xFFE3F4FF), Color(0xFFBFE2FA), Color(0xFF102A3A))
        else -> Triple(Color(0xFFFFFFFF), null, Color(0xFF14110F))
    }

    val startColor = spec.primaryArgb?.let { Color(it) } ?: defaultPalette.first
    val endColor = spec.secondaryArgb?.let { Color(it) } ?: defaultPalette.second
    val textColor = if (spec.primaryArgb != null) {
        val avgR = if (endColor != null) (startColor.red + endColor.red) * 0.5f else startColor.red
        val avgG = if (endColor != null) (startColor.green + endColor.green) * 0.5f else startColor.green
        val avgB = if (endColor != null) (startColor.blue + endColor.blue) * 0.5f else startColor.blue
        val luminance = 0.299f * avgR + 0.587f * avgG + 0.114f * avgB
        if (luminance > 0.58f) Color(0xFF14110F) else Color.White
    } else {
        defaultPalette.third
    }

    val pillShape = RoundedCornerShape(50)
    val bgModifier = if (endColor != null) {
        Modifier.background(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(startColor, endColor)),
            shape = pillShape
        )
    } else {
        Modifier.background(color = startColor, shape = pillShape)
    }

    Box(
        modifier = modifier
            .neumorphicRaised(
                cornerRadius = 50.dp,
                elevation = 2.dp,
                containerColor = startColor,
                accentBorderColor = (endColor ?: startColor).copy(alpha = 0.45f)
            )
            .clip(pillShape)
            .then(bgModifier)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName,
            color = textColor,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.65.sp,
            maxLines = 1
        )
    }
}

data class TagVisualSpec(
    val name: String,
    val primaryArgb: Long?,
    val secondaryArgb: Long? = null
)

object CustomTagCodec {
    val publicStarterTags: List<String> = listOf(
        "MEMBER|#ECE5DD",
        "CHILL|#DCEEE2",
        "NIGHT|#2B2D42",
        "AURORA|#FF8A5B|#845EC2",
        "OCEAN|#00B4D8|#0077B6"
    )

    private val customSolidRegex = Regex(
        pattern = "^tag:([^@|,]{1,18})@#?([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$",
        option = RegexOption.IGNORE_CASE
    )

    private val customGradientRegex = Regex(
        pattern = "^tag:([^@|,]{1,18})@#?([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})[-,>]#?([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$",
        option = RegexOption.IGNORE_CASE
    )

    private fun normalizeHex(rawHex: String): String? {
        val clean = rawHex.trim().removePrefix("#").uppercase()
        return when (clean.length) {
            3 -> "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            6, 8 -> clean
            else -> null
        }
    }

    private fun hexToArgbLong(hex: String?): Long? {
        val clean = hex?.trim()?.removePrefix("#") ?: return null
        return when (clean.length) {
            6 -> clean.toLongOrNull(16)?.or(0xFF000000L)
            8 -> clean.toLongOrNull(16)
            else -> null
        }
    }

    /**
     * Parses `tag:name@#HEX` (solid) or `tag:name@#HEX1-#HEX2` (gradient)
     * into `"NAME|#RRGGBB"` or `"NAME|#RRGGBB|#RRGGBB"`.
     */
    fun parseCustomRedeemTag(raw: String): String? {
        val trimmed = raw.trim()
        customGradientRegex.matchEntire(trimmed)?.let { m ->
            val name = m.groupValues[1].trim().uppercase()
            if (name.isEmpty()) return null
            val h1 = normalizeHex(m.groupValues[2]) ?: return null
            val h2 = normalizeHex(m.groupValues[3]) ?: return null
            return "$name|#$h1|#$h2"
        }
        customSolidRegex.matchEntire(trimmed)?.let { m ->
            val name = m.groupValues[1].trim().uppercase()
            if (name.isEmpty()) return null
            val h1 = normalizeHex(m.groupValues[2]) ?: return null
            return "$name|#$h1"
        }
        return null
    }

    fun extractTagName(stored: String): String =
        stored.substringBefore('|').trim().uppercase()

    fun parseDisplayTag(stored: String): Pair<String, Long?> {
        val spec = parseDisplayTagSpec(stored)
        return spec.name to spec.primaryArgb
    }

    fun parseDisplayTagSpec(stored: String): TagVisualSpec {
        val parts = stored.split('|')
        val name = parts[0].trim().uppercase()
        val primary = hexToArgbLong(parts.getOrNull(1))
        val secondary = hexToArgbLong(parts.getOrNull(2))
        return TagVisualSpec(name = name, primaryArgb = primary, secondaryArgb = secondary)
    }

    fun normalizePeerId(rawId: String): String {
        val clean = rawId.trim()
        return if (clean.equals("dev", ignoreCase = true)) "dev" else clean.uppercase()
    }
}

private val warmAvatarBgPalette = listOf(
    Color(0xFFF6DFC9), // Warm Apricot
    Color(0xFFDCEEE2), // Soft Sage Mint
    Color(0xFFE3EAF6), // Powder Periwinkle
    Color(0xFFF6E2E8), // Soft Rose Quartz
    Color(0xFFF4ECC9), // Sunlit Buttercream
    Color(0xFFE6E1F5)  // Soft Lavender
)

private val warmAvatarInkPalette = listOf(
    Color(0xFF6B3A18),
    Color(0xFF1F5436),
    Color(0xFF233B66),
    Color(0xFF6B2840),
    Color(0xFF5D4B16),
    Color(0xFF3E2D6B)
)

fun peerAvatarTint(peerId: String): Color {
    val idx = kotlin.math.abs(peerId.hashCode()) % warmAvatarBgPalette.size
    return warmAvatarBgPalette[idx]
}

fun peerAvatarInk(peerId: String): Color {
    val idx = kotlin.math.abs(peerId.hashCode()) % warmAvatarInkPalette.size
    return warmAvatarInkPalette[idx]
}

