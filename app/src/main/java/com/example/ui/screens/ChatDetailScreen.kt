package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MessageEntity
import com.example.ui.ChatViewModel
import com.example.ui.components.RouteToggleChip
import com.example.ui.theme.ClestraBubbleRecv
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraCream
import com.example.ui.theme.ClestraTextSoft
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
    var inputText by remember { mutableStateOf("") }
    var previewMediaUri by remember { mutableStateOf<String?>(null) }
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

    val isPeerOnline = peer?.isOnline ?: conversation?.isPeerOnline ?: false
    val peerName = peer?.displayName ?: conversation?.peerName ?: "Peer $peerId"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClestraCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = peerName,
                        color = ClestraCharcoal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    RouteToggleChip(
                        isOnline = isPeerOnline,
                        onToggle = { viewModel.togglePeerOnline(peerId) }
                    )
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
                        onReact = { emoji -> viewModel.reactToMessage(message.id, emoji) },
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onOpenMedia = { uri -> previewMediaUri = uri }
                    )
                }
            }

            // Bottom Input Area matching activity_chat_62498139.xml
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
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
                                text = "Message...",
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
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    val isSent = message.isOutgoing

    // Matches item_msg_sent_fe308c85.xml & item_msg_recv_dfaf37bd.xml
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
                color = if (isSent) ClestraBubbleSent else ClestraBubbleRecv,
                shadowElevation = if (isSent) 1.dp else 0.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showMenu = true }
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
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

        // Timestamp matching tvTime (10sp, textSoft, marginTop 2dp)
        val routeLabel = if (message.deliveryRoute == "P2P_DIRECT") "P2P" else "Relay"
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
