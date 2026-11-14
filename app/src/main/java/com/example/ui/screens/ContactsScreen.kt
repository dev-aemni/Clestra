package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.ui.components.ClestraWallpaperBox
import com.example.ui.components.MuteDurationDialog
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.SoftUiBadgeTag
import com.example.ui.components.formatMuteRemaining
import com.example.ui.components.neumorphicRaised
import com.example.ui.components.peerAvatarInk
import com.example.ui.components.peerAvatarTint
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft
import com.example.ui.theme.ClestraWarmBeige

@Composable
fun ContactsScreen(
    viewModel: ChatViewModel,
    onChatWithPeer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var peerIdInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var statusError by remember { mutableStateOf<String?>(null) }
    var muteDialogPeer by remember { mutableStateOf<PeerEntity?>(null) }
    val peers by viewModel.peers.collectAsState()

    ClestraWallpaperBox(
        intensePattern = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 14.dp)
        ) {
        NeumorphicCard(
            cornerRadius = 28.dp,
            elevation = 5.dp,
            containerColor = ClestraNeuSurface,
            contentPadding = PaddingValues(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Direct Chat",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClestraCharcoal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter any user's Clestra ID to start chatting immediately",
                    fontSize = 13.sp,
                    color = ClestraTextSoft
                )

                Spacer(modifier = Modifier.height(14.dp))

                ClestraPillInput(
                    value = peerIdInput,
                    onValueChange = {
                        peerIdInput = it
                        statusError = null
                    },
                    hint = "Enter Clestra ID (e.g. CLS-... or dev)",
                    modifier = Modifier.testTag("direct_chat_id_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                ClestraPillInput(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    hint = "Nickname (Optional)",
                    modifier = Modifier.testTag("direct_chat_name_input")
                )

                if (statusError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusError ?: "",
                        color = ClestraError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                NeumorphicButton(
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
                    cornerRadius = 14.dp,
                    containerColor = ClestraCopper,
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_direct_chat_button")
                ) {
                    Text(
                        text = "Start Chat",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Contacts & Privacy Controls",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ClestraCharcoal,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(peers, key = { it.peerId }) { peer ->
                ContactControlItem(
                    peer = peer,
                    onToggleOnline = { viewModel.togglePeerOnline(peer.peerId) },
                    onOpenMuteOptions = { muteDialogPeer = peer },
                    onUnmute = { viewModel.unmutePeer(peer.peerId) },
                    onToggleBlock = { viewModel.toggleBlockPeer(peer.peerId) },
                    onDelete = { viewModel.deletePeerAndConversation(peer.peerId) },
                    onChat = { onChatWithPeer(peer.peerId) }
                )
            }
        }
    }
    }

    muteDialogPeer?.let { peer ->
        MuteDurationDialog(
            targetName = peer.displayName,
            isCurrentlyMuted = peer.isMuted,
            onDismiss = { muteDialogPeer = null },
            onSelectDuration = { duration -> viewModel.mutePeer(peer.peerId, duration) },
            onUnmute = { viewModel.unmutePeer(peer.peerId) }
        )
    }
}

@Composable
private fun ContactControlItem(
    peer: PeerEntity,
    onToggleOnline: () -> Unit,
    onOpenMuteOptions: () -> Unit,
    onUnmute: () -> Unit,
    onToggleBlock: () -> Unit,
    onDelete: () -> Unit,
    onChat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val muteLabel = remember(peer.isMuted, peer.mutedUntil) {
        formatMuteRemaining(peer.isMuted, peer.mutedUntil)
    }

    NeumorphicCard(
        cornerRadius = 24.dp,
        elevation = 5.dp,
        containerColor = if (peer.isOfficial) Color(0xFFE7EDF2) else ClestraNeuSurface,
        accentBorderColor = if (peer.isOfficial) ClestraCopper.copy(alpha = 0.25f) else null,
        contentPadding = PaddingValues(14.dp),
        onClick = onChat,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarBg = if (peer.isOfficial) ClestraBubbleSent else peerAvatarTint(peer.peerId)
            val avatarInk = if (peer.isOfficial) ClestraCharcoal else peerAvatarInk(peer.peerId)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .neumorphicRaised(
                        cornerRadius = 14.dp,
                        elevation = 4.dp,
                        containerColor = avatarBg
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (peer.isOfficial) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Clestra Official",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Text(
                        text = peer.displayName.firstOrNull()?.uppercase() ?: "?",
                        color = avatarInk,
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
                    if (muteLabel != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SoftUiBadgeTag(label = muteLabel.uppercase())
                    }
                    if (peer.isBlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ClestraError.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Blocked",
                                color = ClestraError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                            text = if (peer.isOnline) "Direct Online" else "Offline • Relay",
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
                        uncheckedThumbColor = ClestraNeuSurface,
                        uncheckedTrackColor = ClestraNeuWell
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
                        text = { Text("Open Chat") },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onChat()
                        }
                    )

                    if (peer.isMuted) {
                        DropdownMenuItem(
                            text = { Text("Unmute Notifications") },
                            leadingIcon = {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            },
                            onClick = {
                                onUnmute()
                                showMenu = false
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(if (peer.isMuted) "Change Mute Timer..." else "Mute Notifications...") },
                        leadingIcon = {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onOpenMuteOptions()
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
                            text = { Text("Delete Contact & Chat", color = ClestraError) },
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
    SoftUiBadgeTag(
        label = "OFFICIAL",
        modifier = modifier
    )
}
