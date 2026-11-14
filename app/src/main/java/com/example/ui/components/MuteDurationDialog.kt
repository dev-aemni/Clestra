package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraTextSoft

@Composable
fun MuteDurationDialog(
    targetName: String,
    isCurrentlyMuted: Boolean,
    onDismiss: () -> Unit,
    onSelectDuration: (Long?) -> Unit,
    onUnmute: () -> Unit
) {
    val options = listOf(
        "1 Hour" to 60L * 60L * 1000L,
        "8 Hours" to 8L * 60L * 60L * 1000L,
        "24 Hours" to 24L * 60L * 60L * 1000L,
        "Always" to null
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .neumorphicRaised(
                    cornerRadius = 24.dp,
                    elevation = 8.dp,
                    containerColor = ClestraCream
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .neumorphicRaised(
                                cornerRadius = 12.dp,
                                elevation = 4.dp,
                                containerColor = ClestraNeuSurface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFF14110F),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mute Notifications",
                            color = ClestraCharcoal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = targetName,
                            color = ClestraTextSoft,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { (label, duration) ->
                        NeumorphicButton(
                            onClick = {
                                onSelectDuration(duration)
                                onDismiss()
                            },
                            cornerRadius = 14.dp,
                            containerColor = ClestraNeuSurface,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (duration == null) "Mute Always" else "Mute for $label",
                                color = ClestraCharcoal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isCurrentlyMuted) {
                        NeumorphicButton(
                            onClick = {
                                onUnmute()
                                onDismiss()
                            },
                            cornerRadius = 14.dp,
                            containerColor = ClestraCopper,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Unmute Notifications",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ClestraTextSoft)
                    }
                }
            }
        }
    }
}

fun formatMuteRemaining(isMuted: Boolean, mutedUntil: Long?): String? {
    if (!isMuted) return null
    if (mutedUntil == null) return "Muted"
    val remaining = mutedUntil - System.currentTimeMillis()
    if (remaining <= 0L) return null
    val mins = (remaining / 60_000L).coerceAtLeast(1L)
    return if (mins >= 60L) "Muted ${mins / 60L}h" else "Muted ${mins}m"
}
