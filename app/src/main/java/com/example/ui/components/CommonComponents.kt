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
    val (displayName, customArgb) = CustomTagCodec.parseDisplayTag(label)
    val (bgColor, textColor, accentRim) = if (customArgb != null) {
        val bg = Color(customArgb)
        val r = bg.red
        val g = bg.green
        val b = bg.blue
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        val ink = if (luminance > 0.58f) Color(0xFF14110F) else Color.White
        val rim = Color(
            red = (r * 0.82f).coerceIn(0f, 1f),
            green = (g * 0.82f).coerceIn(0f, 1f),
            blue = (b * 0.82f).coerceIn(0f, 1f),
            alpha = 0.65f
        )
        Triple(bg, ink, rim)
    } else {
        when (displayName) {
            "DEV" -> Triple(Color(0xFFBDF7CA), Color(0xFF0E2914), Color(0xFF6CD984))
            "OFFICIAL" -> Triple(Color.White, Color(0xFF14110F), null)
            "BOT" -> Triple(Color(0xFFEEF3FB), Color(0xFF1A2538), Color(0xFFBDCEF0))
            "VIP", "FOUNDER" -> Triple(Color(0xFFFFF4DC), Color(0xFF2B1C0E), Color(0xFFE7C780))
            "PIONEER" -> Triple(Color(0xFFE3F4FF), Color(0xFF102A3A), Color(0xFFA7D4F5))
            else -> Triple(Color.White, Color(0xFF14110F), null)
        }
    }

    Box(
        modifier = modifier
            .neumorphicRaised(
                cornerRadius = 50.dp,
                elevation = 2.5.dp,
                containerColor = bgColor,
                accentBorderColor = accentRim
            )
            .padding(horizontal = 8.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.7.sp,
            maxLines = 1
        )
    }
}

object CustomTagCodec {
    private val customTagRegex = Regex(
        pattern = "^tag:([^@|,]{1,18})@#?([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$",
        option = RegexOption.IGNORE_CASE
    )

    /**
     * Parses `tag:name@hexcolor` or `tag:name@#hexcolor` into `"NAME|#RRGGBB"` (or `"NAME|#AARRGGBB"`).
     * Returns null if input does not match the custom hex tag syntax.
     */
    fun parseCustomRedeemTag(raw: String): String? {
        val match = customTagRegex.matchEntire(raw.trim()) ?: return null
        val name = match.groupValues[1].trim().uppercase()
        if (name.isEmpty()) return null
        val rawHex = match.groupValues[2].uppercase()
        val normalizedHex = when (rawHex.length) {
            3 -> "${rawHex[0]}${rawHex[0]}${rawHex[1]}${rawHex[1]}${rawHex[2]}${rawHex[2]}"
            6, 8 -> rawHex
            else -> return null
        }
        return "$name|#$normalizedHex"
    }

    fun extractTagName(stored: String): String =
        stored.substringBefore('|').trim().uppercase()

    fun parseDisplayTag(stored: String): Pair<String, Long?> {
        val parts = stored.split('|', limit = 2)
        val name = parts[0].trim().uppercase()
        val hexPart = parts.getOrNull(1)?.trim()?.removePrefix("#")
        val argb = if (hexPart != null) {
            when (hexPart.length) {
                6 -> hexPart.toLongOrNull(16)?.or(0xFF000000L)
                8 -> hexPart.toLongOrNull(16)
                else -> null
            }
        } else null
        return name to argb
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

