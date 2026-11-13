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
import androidx.compose.material3.Surface
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
import com.example.ui.theme.ClestraBubbleRecv
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft
import com.example.ui.theme.ClestraWarmBeige

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
    containerColor: Color = ClestraBubbleRecv
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
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = ClestraCharcoal,
            unfocusedTextColor = ClestraCharcoal,
            cursorColor = ClestraCopper
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    )
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
                .clip(RoundedCornerShape(16.dp))
                .background(ClestraBubbleSent),
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
    val bgColor = if (isP2P) ClestraOnline.copy(alpha = 0.14f) else ClestraWarmBeige
    val contentColor = if (isP2P) ClestraOnline else ClestraCopperDark
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
    val bg = if (isOnline) ClestraOnline.copy(alpha = 0.14f) else ClestraBubbleSent
    val fg = if (isOnline) ClestraOnline else ClestraCopperDark
    val text = if (isOnline) "P2P Online" else "Relay Mode"

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
