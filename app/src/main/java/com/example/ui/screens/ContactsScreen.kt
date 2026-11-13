package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PeerEntity
import com.example.ui.ChatViewModel
import com.example.ui.components.ClestraPillInput
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraSand
import com.example.ui.theme.ClestraTextSoft

@Composable
fun ContactsScreen(
    viewModel: ChatViewModel,
    onChatWithPeer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var peerIdInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var statusError by remember { mutableStateOf<String?>(null) }
    val peers by viewModel.peers.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClestraCream)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Direct Chat",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ClestraCharcoal
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter any user's Clestra ID to start chatting immediately",
            fontSize = 14.sp,
            color = ClestraTextSoft
        )

        Spacer(modifier = Modifier.height(12.dp))

        ClestraPillInput(
            value = peerIdInput,
            onValueChange = {
                peerIdInput = it.uppercase()
                statusError = null
            },
            hint = "Enter Clestra ID (e.g. CLS-...)",
            modifier = Modifier.testTag("direct_chat_id_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        ClestraPillInput(
            value = displayNameInput,
            onValueChange = { displayNameInput = it },
            hint = "Nickname (Optional)",
            modifier = Modifier.testTag("direct_chat_name_input")
        )

        if (statusError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusError ?: "",
                color = ClestraError,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val trimmed = peerIdInput.trim()
                if (trimmed.isBlank()) {
                    statusError = "Please enter a user's Clestra ID."
                } else {
                    viewModel.startDirectChatById(trimmed, displayNameInput) { peerId ->
                        peerIdInput = ""
                        displayNameInput = ""
                        onChatWithPeer(peerId)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ClestraCopper,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("start_direct_chat_button")
        ) {
            Text(
                text = "Start Chat",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = ClestraSand.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Contacts & Privacy Controls",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ClestraCharcoal
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(peers, key = { it.peerId }) { peer ->
                ContactControlItem(
                    peer = peer,
                    onToggleOnline = { viewModel.togglePeerOnline(peer.peerId) },
                    onToggleMute = { viewModel.toggleMutePeer(peer.peerId) },
                    onToggleBlock = { viewModel.toggleBlockPeer(peer.peerId) },
                    onDelete = { viewModel.deletePeerAndConversation(peer.peerId) },
                    onChat = { onChatWithPeer(peer.peerId) }
                )
            }
        }
    }
}

@Composable
private fun ContactControlItem(
    peer: PeerEntity,
    onToggleOnline: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleBlock: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onChat() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ClestraBubbleSent)
                    .then(
                        if (peer.isOfficial) Modifier.border(1.5.dp, ClestraCopper, RoundedCornerShape(14.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (peer.isOfficial) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Clestra Official",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = peer.displayName.firstOrNull()?.uppercase() ?: "?",
                        color = ClestraCharcoal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peer.displayName,
                        color = ClestraCharcoal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (peer.isOfficial) {
                        Spacer(modifier = Modifier.width(6.dp))
                        OfficialTag()
                    }
                    if (peer.isMuted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = ClestraTextSoft,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (peer.isBlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ClestraError.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Blocked",
                                color = ClestraError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (peer.isOfficial) "Official Clestra Assistant" else peer.peerId,
                    fontFamily = if (peer.isOfficial) FontFamily.Default else FontFamily.Monospace,
                    color = ClestraTextSoft,
                    fontSize = 11.sp
                )

                if (!peer.isOfficial) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (peer.isOnline) ClestraOnline else ClestraCopper)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (peer.isOnline) "P2P Online" else "Offline • Relay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (peer.isOnline) ClestraOnline else ClestraCopperDark
                        )
                    }
                }
            }

            if (!peer.isOfficial) {
                Switch(
                    checked = peer.isOnline,
                    onCheckedChange = { onToggleOnline() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ClestraOnline,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = ClestraCopper
                    )
                )
            }

            IconButton(onClick = onChat) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat with ${peer.displayName}",
                    tint = ClestraCopper
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Contact options",
                        tint = ClestraTextSoft
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (peer.isMuted) "Unmute Notifications" else "Mute Notifications") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (peer.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onToggleMute()
                            showMenu = false
                        }
                    )

                    if (!peer.isOfficial) {
                        DropdownMenuItem(
                            text = { Text(if (peer.isBlocked) "Unblock User" else "Block User") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (peer.isBlocked) ClestraOnline else ClestraError
                                )
                            },
                            onClick = {
                                onToggleBlock()
                                showMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Remove Contact", color = ClestraError) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = ClestraError
                                )
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfficialTag(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = ClestraCopper,
        modifier = modifier
    ) {
        Text(
            text = "OFFICIAL",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
