package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusP2POnline
import com.example.ui.theme.StatusServerlessRelay

@Composable
fun PeerAvatar(
    name: String,
    avatarColorHex: Long,
    isOnline: Boolean,
    avatarIcon: String = "🍑",
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(avatarColorHex)

    Box(
        modifier = modifier
            .size(size)
            .testTag("avatar_$name")
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(baseColor, baseColor.copy(alpha = 0.85f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarIcon.isNotBlank()) {
                Text(
                    text = avatarIcon,
                    fontSize = (size.value * 0.5f).sp
                )
            } else {
                val initials = name.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "?" }
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }

        // Online / Relay Indicator Dot
        val statusColor = if (isOnline) StatusP2POnline else StatusServerlessRelay
        val dotSize = size * 0.28f

        Box(
            modifier = Modifier
                .size(dotSize)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(statusColor)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
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
    val bgColor = if (isP2P) {
        Color(0xFFE8F5E9)
    } else {
        Color(0xFFFFEFE6)
    }
    val contentColor = if (isP2P) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFFE65100)
    }
    val label = if (isP2P) "P2P Direct" else "Relay Store"
    val icon = if (isP2P) Icons.Default.FlashOn else Icons.Default.Cloud

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    val isP2P = isOnline
    val bg = if (isP2P) Color(0xFFE8F5E9) else Color(0xFFFFECE2)
    val fg = if (isP2P) Color(0xFF2E7D32) else Color(0xFFD84315)
    val text = if (isP2P) "⚡ Online (P2P)" else "☁️ Offline (Relay)"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .testTag("route_toggle_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
