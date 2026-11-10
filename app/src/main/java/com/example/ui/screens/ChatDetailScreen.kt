package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.ui.ChatViewModel
import com.example.ui.components.PeerAvatar
import com.example.ui.components.RouteBadge
import com.example.ui.components.RouteToggleChip
import com.example.ui.theme.PeachBubbleReceivedDark
import com.example.ui.theme.PeachBubbleReceivedLight
import com.example.ui.theme.PeachBubbleSent
import com.example.ui.theme.PeachBubbleSentGradientEnd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    peerId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle back button presses correctly
    BackHandler {
        viewModel.closeConversation()
        onBack()
    }

    LaunchedEffect(peerId) {
        viewModel.selectConversation(peerId)
    }

    val conversation by viewModel.activeConversation.collectAsState()
    val peer by viewModel.activePeer.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isPeerOnline = peer?.isOnline ?: conversation?.isPeerOnline ?: false
    val peerName = peer?.displayName ?: conversation?.peerName ?: "Peer $peerId"
    val avatarColor = peer?.avatarColor ?: conversation?.peerAvatarColor ?: 0xFFFF7043

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PeerAvatar(
                            name = peerName,
                            avatarColorHex = avatarColor,
                            isOnline = isPeerOnline,
                            avatarIcon = peer?.avatarIcon ?: conversation?.peerAvatarIcon ?: "🍑",
                            size = 40.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = if (isPeerOnline) "Direct P2P Ready" else "Offline • Relay Active",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (isPeerOnline) Color(0xFF2E7D32) else Color(0xFFD84315)
                                )
                            )
                        }

                        // Direct Toggle button for testing P2P vs Serverless switching!
                        RouteToggleChip(
                            isOnline = isPeerOnline,
                            onToggle = { viewModel.togglePeerOnline(peerId) }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.closeConversation()
                            onBack()
                        },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Channel Architecture Status Banner
            Surface(
                color = if (isPeerOnline) Color(0xFFE8F5E9) else Color(0xFFFFECE2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPeerOnline)
                            "⚡ Direct P2P Channel: Recipient is online. Packets stream zero-hop."
                        else
                            "☁️ Offline Relay: Recipient is offline. Messages queued to MongoDB Atlas and auto-cleaned on delivery.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPeerOnline) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                    )
                }
            }

            // Messages LazyColumn
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("messages_list")
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onReact = { emoji -> viewModel.reactToMessage(message.id, emoji) },
                        onDelete = { viewModel.deleteMessage(message.id) }
                    )
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                if (isPeerOnline) "Send directly via P2P..." else "Send via Serverless Relay..."
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PeachBubbleSent, PeachBubbleSentGradientEnd)
                                )
                            )
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }

    val isSent = message.isOutgoing
    val alignment = if (isSent) Alignment.End else Alignment.Start

    val bubbleShape = if (isSent) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box {
            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSent) {
                        PeachBubbleSent
                    } else {
                        if (isDark) PeachBubbleReceivedDark else PeachBubbleReceivedLight
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(bubbleShape)
                    .clickable { showMenu = true }
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Content
                    Text(
                        text = message.content,
                        color = if (isSent) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                    )

                    Spacer(Modifier.height(4.dp))

                    // Meta footer: Route pill + Timestamp + Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RouteBadge(
                            deliveryRoute = message.deliveryRoute,
                            isCompact = true
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = formatTimeSimple(message.timestamp),
                            fontSize = 10.sp,
                            color = if (isSent) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                        )

                        if (isSent) {
                            Spacer(Modifier.width(4.dp))
                            val statusIcon = when (message.status) {
                                "DELIVERED" -> Icons.Default.DoneAll
                                "SENT" -> Icons.Default.Check
                                else -> Icons.Default.Schedule
                            }
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = message.status,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Quick Menu (Reactions, Copy, Delete)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Emoji Reaction Row
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    listOf("❤️", "👍", "🔥", "😂", "🍑", "⚡").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clickable {
                                    onReact(emoji)
                                    showMenu = false
                                }
                                .padding(horizontal = 6.dp)
                        )
                    }
                }

                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    onClick = {
                        clipboard.setText(AnnotatedString(message.content))
                        showMenu = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete Message") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }

        // Reaction bubble display
        if (!message.reaction.isNullOrEmpty()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .padding(top = 2.dp, end = if (isSent) 8.dp else 0.dp, start = if (!isSent) 8.dp else 0.dp)
                    .clickable { onReact("") } // remove reaction on tap
            ) {
                Text(
                    text = message.reaction,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

fun formatTimeSimple(millis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
