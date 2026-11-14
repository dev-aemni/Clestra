package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PeerEntity
import com.example.ui.theme.ClestraBubbleRecv
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraSand
import com.example.ui.theme.ClestraTextSoft

@Composable
fun NewChatDialog(
    peers: List<PeerEntity>,
    onDismiss: () -> Unit,
    onSelectPeer: (String) -> Unit,
    onAddNewPeerAndChat: (String, String) -> Unit
) {
    var customPeerId by remember { mutableStateOf("") }
    var customPeerName by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(peers.isEmpty()) }

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
                                elevation = 3.dp,
                                containerColor = ClestraBubbleSent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = ClestraCopper,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Start a Chat",
                        color = ClestraCharcoal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!showCustomInput && peers.isNotEmpty()) {
                    Text(
                        text = "Saved Contacts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ClestraTextSoft
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        items(peers, key = { it.peerId }) { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectPeer(peer.peerId) }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PeerAvatar(
                                    name = peer.displayName,
                                    avatarColorHex = peer.avatarColor,
                                    isOnline = peer.isOnline,
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = peer.displayName,
                                        color = ClestraCharcoal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = peer.peerId,
                                        fontSize = 11.sp,
                                        color = ClestraTextSoft
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ClestraSand.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showCustomInput = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClestraCopper,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enter_custom_peer_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect with Clestra ID", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Enter a user's Clestra ID to start an encrypted chat directly.",
                        fontSize = 13.sp,
                        color = ClestraTextSoft
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ClestraPillInput(
                        value = customPeerId,
                        onValueChange = { customPeerId = it.trim() },
                        hint = "Clestra ID (e.g. CLS-8A2F-9C14-X1P2 or dev)",
                        containerColor = ClestraBubbleRecv,
                        modifier = Modifier.testTag("peer_id_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ClestraPillInput(
                        value = customPeerName,
                        onValueChange = { customPeerName = it },
                        hint = "Name (Optional)",
                        containerColor = ClestraBubbleRecv,
                        modifier = Modifier.testTag("peer_name_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (peers.isNotEmpty()) {
                            TextButton(onClick = { showCustomInput = false }) {
                                Text("Back", color = ClestraTextSoft)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                if (customPeerId.isNotBlank()) {
                                    val name = if (customPeerName.isNotBlank()) customPeerName else customPeerId
                                    onAddNewPeerAndChat(customPeerId.trim(), name.trim())
                                }
                            },
                            enabled = customPeerId.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClestraCopper,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("confirm_chat_with_id_button")
                        ) {
                            Text("Start Chat", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = ClestraTextSoft)
                    }
                }
            }
        }
    }
}
