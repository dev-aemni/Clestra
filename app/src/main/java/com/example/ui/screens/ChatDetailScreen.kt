package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MessageEntity
import com.example.data.repository.ChatRepository
import com.example.ui.ChatViewModel
import com.example.ui.components.RouteToggleChip
import com.example.ui.theme.ClestraBubbleRecv
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft
import com.example.ui.theme.ClestraWarmBeige
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    peerId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closeConversation()
        onBack()
    }

    LaunchedEffect(peerId) {
        viewModel.selectConversation(peerId)
    }

    val context = LocalContext.current
    val conversation by viewModel.activeConversation.collectAsState()
    val peer by viewModel.activePeer.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val isBotTyping by viewModel.isBotTyping.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var previewMediaUri by remember { mutableStateOf<String?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.sendMessage(inputText, mediaUri = uri.toString())
            inputText = ""
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isOfficial = peer?.isOfficial == true || peerId == ChatRepository.OFFICIAL_BOT_ID
    val isPeerOnline = peer?.isOnline ?: conversation?.isPeerOnline ?: false
    val isMuted = peer?.isMuted ?: conversation?.isMuted ?: false
    val isBlocked = peer?.isBlocked ?: conversation?.isBlocked ?: false
    val peerName = peer?.displayName ?: conversation?.peerName ?: "Peer $peerId"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClestraCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                )
        ) {
            // Header matching activity_chat_62498139.xml
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.closeConversation()
                            onBack()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ClestraCopper,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (isOfficial) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Clestra Official",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = peerName,
                                color = ClestraCharcoal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isOfficial) {
                                Spacer(modifier = Modifier.width(6.dp))
                                OfficialTag()
                            }
                            if (isMuted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = "Muted",
                                    tint = ClestraTextSoft,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = ClestraTextSoft,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isOfficial) {
                                    "Official Clestra Assistant"
                                } else if (isBlocked) {
                                    "Blocked"
                                } else {
                                    "AES-256 Encrypted"
                                },
                                color = if (isBlocked) ClestraError else ClestraTextSoft,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!isOfficial) {
                        RouteToggleChip(
                            isOnline = isPeerOnline,
                            onToggle = { viewModel.togglePeerOnline(peerId) }
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("chat_more_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Chat options",
                                tint = ClestraCharcoal
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isMuted) "Unmute Notifications" else "Mute Notifications") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    viewModel.toggleMutePeer(peerId)
                                    showOverflowMenu = false
                                }
                            )

                            if (!isOfficial) {
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) "Unblock User" else "Block User") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = if (isBlocked) ClestraOnline else ClestraError
                                        )
                                    },
                                    onClick = {
                                        viewModel.toggleBlockPeer(peerId)
                                        showOverflowMenu = false
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Clear Chat History") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    viewModel.clearConversationHistory(peerId)
                                    showOverflowMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Messages List matching activity_chat_62498139.xml
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("messages_list")
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isOfficialBotChat = isOfficial,
                        onReact = { emoji -> viewModel.reactToMessage(message.id, emoji) },
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onOpenMedia = { uri -> previewMediaUri = uri }
                    )
                }

                if (isOfficial && isBotTyping) {
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ClestraWarmBeige,
                                border = BorderStroke(1.dp, ClestraCopper.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "Clestra is thinking...",
                                    color = ClestraTextSoft,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Area or Blocked Banner
            if (isBlocked) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "You blocked this user.",
                            color = ClestraError,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = { viewModel.toggleBlockPeer(peerId) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClestraCopper,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Unblock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("attach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach media",
                                tint = ClestraTextSoft,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (isOfficial) "Ask Clestra anything..." else "Message...",
                                    color = ClestraTextSoft,
                                    fontSize = 14.sp
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ClestraCream,
                                unfocusedContainerColor = ClestraCream,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = ClestraCharcoal,
                                unfocusedTextColor = ClestraCharcoal,
                                cursorColor = ClestraCopper
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("chat_input_field")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ClestraBubbleSent)
                                .clickable {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                }
                                .testTag("send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = ClestraCopperDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen Media Preview matching activity_media_preview_68f3b2cc.xml
        if (previewMediaUri != null) {
            BackHandler { previewMediaUri = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                AsyncImage(
                    model = previewMediaUri,
                    contentDescription = "Media preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { previewMediaUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isOfficialBotChat: Boolean = false,
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    val isSent = message.isOutgoing
    val isBotIncoming = !isSent && isOfficialBotChat

    val bubbleBg = when {
        isSent -> ClestraBubbleSent
        isBotIncoming -> ClestraWarmBeige
        else -> ClestraBubbleRecv
    }
    val bubbleBorder = if (isBotIncoming) {
        BorderStroke(1.dp, ClestraCopper.copy(alpha = 0.35f))
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isSent) 60.dp else 12.dp,
                end = if (isSent) 12.dp else 60.dp,
                bottom = 8.dp
            ),
        horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bubbleBg,
                border = bubbleBorder,
                shadowElevation = if (isSent || isBotIncoming) 1.dp else 0.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showMenu = true }
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (isBotIncoming) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "Clestra",
                                color = ClestraCopperDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OfficialTag()
                        }
                    }

                    if (!message.mediaUri.isNullOrBlank()) {
                        AsyncImage(
                            model = message.mediaUri,
                            contentDescription = "Image attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenMedia(message.mediaUri) }
                        )
                        if (message.content.isNotBlank() && message.content != "Photo attachment") {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    if (message.content.isNotBlank() &&
                        !(message.mediaUri != null && message.content == "Photo attachment")
                    ) {
                        Text(
                            text = message.content,
                            color = ClestraCharcoal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
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

        if (!message.reaction.isNullOrEmpty()) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { onReact("") }
            ) {
                Text(
                    text = message.reaction,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        val routeLabel = if (isOfficialBotChat) {
            "Official • E2EE"
        } else if (message.deliveryRoute == "P2P_DIRECT") {
            "P2P • E2EE"
        } else {
            "Relay • E2EE"
        }
        Text(
            text = "${formatTimeSimple(message.timestamp)} • $routeLabel",
            color = ClestraTextSoft,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp)
        )
    }
}

fun formatTimeSimple(millis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
