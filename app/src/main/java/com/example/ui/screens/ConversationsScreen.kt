package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ConversationEntity
import com.example.ui.ChatViewModel
import com.example.ui.components.ClestraWallpaperBox
import com.example.ui.components.MuteDurationDialog
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicCard
import com.example.ui.components.SoftUiBadgeTag
import com.example.ui.components.formatMuteRemaining
import com.example.ui.components.neumorphicPressed
import com.example.ui.components.neumorphicRaised
import com.example.ui.components.peerAvatarInk
import com.example.ui.components.peerAvatarTint
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft
import com.example.ui.theme.ClestraWarmBeige
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationsScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.filteredConversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilterTab by viewModel.selectedFilterTab.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var muteTargetConvo by remember { mutableStateOf<ConversationEntity?>(null) }

    ClestraWallpaperBox(
        intensePattern = true,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Crisp Pure-White Header with subtle curved accent waves (no illogical subtitle line)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicRaised(
                            cornerRadius = 28.dp,
                            elevation = 5.dp,
                            containerColor = ClestraNeuSurface
                        )
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Subtle decorative organic curves inside the pure-white header
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ClestraBubbleSent.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(w * 0.85f, h * 0.25f),
                                radius = w * 0.34f
                            ),
                            center = Offset(w * 0.85f, h * 0.25f),
                            radius = w * 0.34f
                        )

                        val wave1 = Path().apply {
                            moveTo(w * 0.45f, h * 0.92f)
                            cubicTo(
                                w * 0.62f, h * 0.35f,
                                w * 0.80f, h * 0.95f,
                                w * 1.02f, h * 0.28f
                            )
                        }
                        drawPath(
                            path = wave1,
                            color = ClestraCopper.copy(alpha = 0.14f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val wave2 = Path().apply {
                            moveTo(w * 0.56f, h * 1.05f)
                            cubicTo(
                                w * 0.72f, h * 0.52f,
                                w * 0.88f, h * 0.90f,
                                w * 1.02f, h * 0.55f
                            )
                        }
                        drawPath(
                            path = wave2,
                            color = ClestraCopper.copy(alpha = 0.08f),
                            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .neumorphicRaised(
                                    cornerRadius = 12.dp,
                                    elevation = 3.dp,
                                    containerColor = ClestraBubbleSent
                                )
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Clestra",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(9.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Clestra",
                            color = ClestraCharcoal,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (showSearch) {
                                    viewModel.searchQuery.value = ""
                                }
                                showSearch = !showSearch
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(42.dp)
                                .then(
                                    if (showSearch) {
                                        Modifier.neumorphicPressed(
                                            cornerRadius = 14.dp,
                                            containerColor = ClestraNeuWell
                                        )
                                    } else {
                                        Modifier.neumorphicRaised(
                                            cornerRadius = 21.dp,
                                            elevation = 4.dp,
                                            containerColor = ClestraNeuSurface
                                        )
                                    }
                                )
                                .testTag("btn_search")
                        ) {
                            Icon(
                                imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search chats",
                                tint = if (showSearch) ClestraCopper else ClestraCharcoal,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showSearch) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = {
                            Text(
                                text = "Search by name, message, or Clestra ID...",
                                color = ClestraTextSoft,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ClestraCharcoal,
                            unfocusedTextColor = ClestraCharcoal,
                            cursorColor = ClestraCopper
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .neumorphicPressed(
                                cornerRadius = 26.dp,
                                containerColor = ClestraNeuWell
                            )
                            .testTag("search_field")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All" to 0, "Online" to 1, "Relay" to 2).forEach { (label, tabIdx) ->
                        val isSelected = selectedFilterTab == tabIdx
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isSelected) {
                                        Modifier.neumorphicRaised(
                                            cornerRadius = 50.dp,
                                            elevation = 4.dp,
                                            containerColor = ClestraNeuSurface,
                                            accentBorderColor = ClestraCopper.copy(alpha = 0.45f)
                                        )
                                    } else {
                                        Modifier.neumorphicPressed(
                                            cornerRadius = 50.dp,
                                            containerColor = ClestraNeuWell
                                        )
                                    }
                                )
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.selectedFilterTab.value = tabIdx }
                                .padding(horizontal = 15.dp, vertical = 7.dp)
                                .testTag("filter_tab_$tabIdx")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) ClestraCopper else ClestraTextSoft,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeumorphicCard(
                        cornerRadius = 28.dp,
                        elevation = 6.dp,
                        containerColor = ClestraNeuSurface,
                        contentPadding = PaddingValues(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .neumorphicRaised(
                                        cornerRadius = 24.dp,
                                        elevation = 6.dp,
                                        containerColor = ClestraBubbleSent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Clestra Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching chats" else "No conversations yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClestraCharcoal
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Enter a user's Clestra ID to start chatting right away.",
                                fontSize = 14.sp,
                                color = ClestraTextSoft,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            NeumorphicButton(
                                onClick = onOpenNewChat,
                                cornerRadius = 14.dp,
                                containerColor = ClestraCopper,
                                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("empty_start_chat_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Start New Chat",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 118.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversations_list")
                ) {
                    items(conversations, key = { it.peerId }) { convo ->
                        ConversationItem(
                            conversation = convo,
                            onClick = { onOpenChat(convo.peerId) },
                            onTogglePin = { viewModel.togglePinConversation(convo.peerId) },
                            onOpenMuteOptions = { muteTargetConvo = convo },
                            onUnmute = { viewModel.unmutePeer(convo.peerId) },
                            onToggleBlock = { viewModel.toggleBlockPeer(convo.peerId) },
                            onDelete = { viewModel.deletePeerAndConversation(convo.peerId) }
                        )
                    }
                }
            }
        }
    }

    muteTargetConvo?.let { convo ->
        MuteDurationDialog(
            targetName = convo.peerName,
            isCurrentlyMuted = convo.isMuted,
            onDismiss = { muteTargetConvo = null },
            onSelectDuration = { duration -> viewModel.mutePeer(convo.peerId, duration) },
            onUnmute = { viewModel.unmutePeer(convo.peerId) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit = {},
    onOpenMuteOptions: () -> Unit = {},
    onUnmute: () -> Unit = {},
    onToggleBlock: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val muteLabel = remember(conversation.isMuted, conversation.mutedUntil) {
        formatMuteRemaining(conversation.isMuted, conversation.mutedUntil)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicRaised(
                    cornerRadius = 24.dp,
                    elevation = 5.dp,
                    containerColor = if (conversation.isOfficial) Color(0xFFE7EDF2) else ClestraNeuSurface,
                    accentBorderColor = if (conversation.isOfficial) ClestraCopper.copy(alpha = 0.25f) else null
                )
                .clip(RoundedCornerShape(22.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .testTag("conversation_item_${conversation.peerId}")
        ) {
            // Subtle curved sheen inside each conversation row
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val accent = if (conversation.isOfficial) ClestraCopper else ClestraBubbleSent
                val curve = Path().apply {
                    moveTo(w * 0.65f, 0f)
                    quadraticTo(w * 0.82f, h * 0.55f, w, h * 0.25f)
                    lineTo(w, 0f)
                    close()
                }
                drawPath(path = curve, color = accent.copy(alpha = if (conversation.isOfficial) 0.10f else 0.28f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(52.dp)) {
                    val avatarBg = if (conversation.isOfficial) ClestraBubbleSent else peerAvatarTint(conversation.peerId)
                    val avatarInk = if (conversation.isOfficial) ClestraCharcoal else peerAvatarInk(conversation.peerId)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .neumorphicRaised(
                                cornerRadius = 16.dp,
                                elevation = 4.dp,
                                containerColor = avatarBg
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (conversation.isOfficial) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Clestra Official",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Text(
                                text = conversation.peerName.firstOrNull()?.uppercase() ?: "?",
                                color = avatarInk,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(
                                when {
                                    conversation.isBlocked -> ClestraError
                                    conversation.isPeerOnline || conversation.isOfficial -> ClestraOnline
                                    else -> ClestraCopper
                                }
                            )
                            .border(2.dp, ClestraNeuSurface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = conversation.peerName,
                                color = ClestraCharcoal,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (conversation.isOfficial) {
                                Spacer(modifier = Modifier.width(6.dp))
                                OfficialTag()
                            } else if (conversation.isPinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                SoftUiBadgeTag(label = "PINNED")
                            }
                            if (muteLabel != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = muteLabel,
                                    tint = ClestraTextSoft,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            if (conversation.isBlocked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Blocked",
                                    color = ClestraError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatTime(conversation.lastTimestamp),
                            color = ClestraTextSoft,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conversation.lastMessage,
                            color = ClestraTextSoft,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (conversation.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .neumorphicRaised(
                                        cornerRadius = 11.dp,
                                        elevation = 2.dp,
                                        containerColor = ClestraCopper,
                                        accentBorderColor = Color.White
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conversation.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (!conversation.isOfficial) {
                DropdownMenuItem(
                    text = { Text(if (conversation.isPinned) "Unpin Chat" else "Pin Chat") },
                    leadingIcon = {
                        Icon(Icons.Default.PushPin, contentDescription = null)
                    },
                    onClick = {
                        onTogglePin()
                        showMenu = false
                    }
                )
            }

            if (conversation.isMuted) {
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
                text = { Text(if (conversation.isMuted) "Change Mute Timer..." else "Mute Notifications...") },
                leadingIcon = {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null)
                },
                onClick = {
                    showMenu = false
                    onOpenMuteOptions()
                }
            )

            if (!conversation.isOfficial) {
                DropdownMenuItem(
                    text = { Text(if (conversation.isBlocked) "Unblock User" else "Block User") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = if (conversation.isBlocked) ClestraOnline else ClestraError
                        )
                    },
                    onClick = {
                        onToggleBlock()
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete Chat", color = ClestraError) },
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

fun formatTime(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf.format(Date(millis))
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(millis))
        }
    }
}
