package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.example.ui.components.AudioMetaCodec
import com.example.ui.components.ClestraAudioPlaybackController
import com.example.ui.components.ClestraAudioPlayerBar
import com.example.ui.components.ClestraLiveRecordingStrip
import com.example.ui.components.ClestraVoiceRecorder
import com.example.ui.components.ClestraWallpaperBox
import com.example.ui.components.CoilGifSupport
import com.example.ui.components.ExpressionPanel
import com.example.ui.components.FileAttachmentCard
import com.example.ui.components.FileAttachmentUtils
import com.example.ui.components.FullScreenCodeViewerModal
import com.example.ui.components.FullScreenDocumentData
import com.example.ui.components.LocalClestraAudioController
import com.example.ui.components.MarkdownMessageContent
import com.example.ui.components.MuteDurationDialog
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.RouteToggleChip
import com.example.ui.components.StickerVisual
import com.example.ui.components.formatMuteRemaining
import com.example.ui.components.neumorphicBubble
import com.example.ui.components.neumorphicPressed
import com.example.ui.components.neumorphicRaised
import com.example.ui.components.peerAvatarInk
import com.example.ui.components.peerAvatarTint
import com.example.ui.theme.ClestraBubbleSent
import com.example.ui.theme.ClestraCharcoal
import com.example.ui.theme.ClestraCopper
import com.example.ui.theme.ClestraCopperDark
import com.example.ui.theme.ClestraError
import com.example.ui.theme.ClestraNeuSurface
import com.example.ui.theme.ClestraNeuWell
import com.example.ui.theme.ClestraOnline
import com.example.ui.theme.ClestraTextSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val gifImageLoader = remember(context) { CoilGifSupport.getImageLoader(context) }

    val conversation by viewModel.activeConversation.collectAsState()
    val peer by viewModel.activePeer.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val userAccount by viewModel.userAccount.collectAsState()
    val isBotTyping by viewModel.isBotTyping.collectAsState()
    val stickers by viewModel.stickers.collectAsState()
    val gifItems by viewModel.gifItems.collectAsState()
    val selectedGifProvider by viewModel.selectedGifProvider.collectAsState()
    val gifQuery by viewModel.gifQuery.collectAsState()
    val isLoadingGifs by viewModel.isLoadingGifs.collectAsState()
    val gifNotice by viewModel.gifNotice.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var previewMediaUri by remember { mutableStateOf<String?>(null) }
    var fullScreenDoc by remember { mutableStateOf<FullScreenDocumentData?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showExpressionPanel by remember { mutableStateOf(false) }
    var showAttachTray by remember { mutableStateOf(false) }
    var showMuteDialog by remember { mutableStateOf(false) }
    var showChatSearch by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    var liveVoiceAmplitudes by remember { mutableStateOf<List<Int>>(emptyList()) }

    val voiceRecorder = remember(context) { ClestraVoiceRecorder(context) }
    val audioPlaybackController = remember(context) { ClestraAudioPlaybackController(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.cancel()
            audioPlaybackController.release()
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            liveVoiceAmplitudes = emptyList()
            while (isRecordingVoice) {
                val amp = voiceRecorder.pollAmplitudeNormalized()
                recordingElapsedMs = voiceRecorder.currentElapsedMs()
                liveVoiceAmplitudes = (liveVoiceAmplitudes + amp).takeLast(28)
                delay(65L)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showAttachTray = false
            showExpressionPanel = false
            focusManager.clearFocus()
            keyboardController?.hide()
            if (voiceRecorder.start()) {
                recordingElapsedMs = 0L
                isRecordingVoice = true
            } else {
                Toast.makeText(context, "Unable to start microphone", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission needed for voice messages", Toast.LENGTH_SHORT).show()
        }
    }

    val listState = rememberLazyListState()

    BackHandler(enabled = showExpressionPanel && previewMediaUri == null && fullScreenDoc == null) {
        showExpressionPanel = false
    }

    val isOfficial = peer?.isOfficial == true || peerId == ChatRepository.OFFICIAL_BOT_ID
    val isPeerOnline = peer?.isOnline ?: conversation?.isPeerOnline ?: false
    val isMuted = peer?.isMuted ?: conversation?.isMuted ?: false
    val mutedUntil = peer?.mutedUntil ?: conversation?.mutedUntil
    val isPinned = conversation?.isPinned ?: false
    val isBlocked = peer?.isBlocked ?: conversation?.isBlocked ?: false
    val peerName = peer?.displayName ?: conversation?.peerName ?: "Peer $peerId"
    val muteLabel = remember(isMuted, mutedUntil) { formatMuteRemaining(isMuted, mutedUntil) }

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
            val caption = inputText
            val reply = replyingToMessage
            inputText = ""
            replyingToMessage = null
            showAttachTray = false
            viewModel.sendAttachment(
                sourceUri = uri,
                forceImage = true,
                caption = caption,
                replyTo = reply,
                replySenderName = if (reply?.isOutgoing == true) "You" else peerName
            )
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val reply = replyingToMessage
            replyingToMessage = null
            showAttachTray = false
            viewModel.sendAttachment(
                sourceUri = uri,
                forceImage = false,
                replyTo = reply,
                replySenderName = if (reply?.isOutgoing == true) "You" else peerName
            )
        }
    }

    val displayedMessages = remember(messages, chatSearchQuery) {
        val q = chatSearchQuery.trim()
        if (q.isBlank()) messages
        else messages.filter {
            it.content.contains(q, ignoreCase = true) ||
                (it.attachmentName?.contains(q, ignoreCase = true) == true)
        }
    }

    LaunchedEffect(displayedMessages.size) {
        if (displayedMessages.isNotEmpty() && chatSearchQuery.isBlank()) {
            listState.animateScrollToItem(displayedMessages.size - 1)
        }
    }

    CompositionLocalProvider(LocalClestraAudioController provides audioPlaybackController) {
    ClestraWallpaperBox(
        intensePattern = true,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.ime.union(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                )
        ) {
            // Exact CSS #ECF0F3 Soft UI Chat Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphicRaised(
                            cornerRadius = 28.dp,
                            elevation = 5.dp,
                            containerColor = ClestraNeuSurface
                        )
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val curve = Path().apply {
                            moveTo(w * 0.55f, h * 0.95f)
                            cubicTo(w * 0.72f, h * 0.35f, w * 0.85f, h * 0.90f, w, h * 0.25f)
                        }
                        drawPath(
                            path = curve,
                            color = ClestraCopper.copy(alpha = 0.12f),
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.closeConversation()
                                onBack()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .neumorphicRaised(
                                    cornerRadius = 13.dp,
                                    elevation = 4.dp,
                                    containerColor = ClestraNeuSurface
                                )
                                .testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ClestraCopper,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val headerAvatarBg = if (isOfficial) ClestraBubbleSent else peerAvatarTint(peerId)
                        val headerAvatarInk = if (isOfficial) ClestraCharcoal else peerAvatarInk(peerId)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .neumorphicRaised(
                                    cornerRadius = 12.dp,
                                    elevation = 4.dp,
                                    containerColor = headerAvatarBg
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isOfficial) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Clestra Official",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Text(
                                    text = peerName.firstOrNull()?.uppercase() ?: "?",
                                    color = headerAvatarInk,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = peerName,
                                    color = ClestraCharcoal,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isOfficial) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OfficialTag()
                                }
                                if (muteLabel != null) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = muteLabel,
                                        tint = ClestraTextSoft,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            val statusLine = buildString {
                                append(
                                    when {
                                        isOfficial -> "Official Assistant"
                                        isBlocked -> "Blocked"
                                        isPeerOnline -> "Online"
                                        else -> "Offline"
                                    }
                                )
                                if (muteLabel != null) {
                                    append(" • ")
                                    append(muteLabel)
                                }
                            }
                            Text(
                                text = statusLine,
                                color = when {
                                    isBlocked -> ClestraError
                                    isPeerOnline || isOfficial -> ClestraOnline
                                    else -> ClestraTextSoft
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isOfficial) {
                            RouteToggleChip(
                                isOnline = isPeerOnline,
                                onToggle = { viewModel.togglePeerOnline(peerId) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = {
                                if (showChatSearch) chatSearchQuery = ""
                                showChatSearch = !showChatSearch
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(38.dp)
                                .then(
                                    if (showChatSearch) {
                                        Modifier.neumorphicPressed(
                                            cornerRadius = 12.dp,
                                            containerColor = ClestraNeuWell
                                        )
                                    } else {
                                        Modifier.neumorphicRaised(
                                            cornerRadius = 12.dp,
                                            elevation = 4.dp,
                                            containerColor = ClestraNeuSurface
                                        )
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = if (showChatSearch) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search in chat",
                                tint = if (showChatSearch) ClestraCopper else ClestraCharcoal,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(38.dp)
                                    .neumorphicRaised(
                                        cornerRadius = 12.dp,
                                        elevation = 4.dp,
                                        containerColor = ClestraNeuSurface
                                    )
                                    .testTag("chat_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Chat options",
                                    tint = ClestraCharcoal,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                if (isMuted) {
                                    DropdownMenuItem(
                                        text = { Text("Unmute Notifications") },
                                        leadingIcon = {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                        },
                                        onClick = {
                                            viewModel.unmutePeer(peerId)
                                            showOverflowMenu = false
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text(if (isMuted) "Change Mute Timer..." else "Mute Notifications...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.NotificationsOff, contentDescription = null)
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showMuteDialog = true
                                    }
                                )

                                if (!isOfficial) {
                                    DropdownMenuItem(
                                        text = { Text(if (isPinned) "Unpin Chat" else "Pin Chat") },
                                        leadingIcon = {
                                            Icon(Icons.Default.PushPin, contentDescription = null)
                                        },
                                        onClick = {
                                            viewModel.togglePinConversation(peerId)
                                            showOverflowMenu = false
                                        }
                                    )

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
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                    },
                                    onClick = {
                                        viewModel.clearConversationHistory(peerId)
                                        showOverflowMenu = false
                                    }
                                )

                                if (!isOfficial) {
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
                                            showOverflowMenu = false
                                            viewModel.deletePeerAndConversation(peerId)
                                            viewModel.closeConversation()
                                            onBack()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showChatSearch) {
                    TextField(
                        value = chatSearchQuery,
                        onValueChange = { chatSearchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search messages & files (${displayedMessages.size} shown)...",
                                color = ClestraTextSoft,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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
                            .padding(top = 8.dp)
                            .neumorphicPressed(
                                cornerRadius = 20.dp,
                                containerColor = ClestraNeuWell
                            )
                    )
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("messages_list")
            ) {
                items(displayedMessages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isOfficialBotChat = isOfficial,
                        senderActiveTag = userAccount?.activeTag,
                        onReact = { emoji -> viewModel.reactToMessage(message.id, emoji) },
                        onReply = {
                            editingMessage = null
                            replyingToMessage = message
                        },
                        onToggleStar = { viewModel.toggleStarMessage(message.id, message.isStarred) },
                        onStartEdit = {
                            replyingToMessage = null
                            editingMessage = message
                            inputText = message.content
                        },
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onOpenMedia = { uri -> previewMediaUri = uri },
                        onOpenFullScreenDoc = { doc -> fullScreenDoc = doc },
                        onSaveImage = { uri, title ->
                            scope.launch {
                                val ok = FileAttachmentUtils.saveImageToGallery(context, uri, title)
                                Toast.makeText(
                                    context,
                                    if (ok) "Saved to Pictures/Clestra" else "Unable to save image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onSaveFile = { uri, fileName, mimeType ->
                            scope.launch {
                                val ok = FileAttachmentUtils.saveFileToDownloads(context, uri, fileName, mimeType)
                                Toast.makeText(
                                    context,
                                    if (ok) "Saved to Downloads/Clestra" else "Unable to save file",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                if (isOfficial && isBotTyping) {
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.neumorphicBubble(
                                    topStart = 5.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 18.dp,
                                    elevation = 4.dp,
                                    topColor = ClestraNeuSurface,
                                    bottomColor = Color(0xFFE4EAEF)
                                )
                            ) {
                                Text(
                                    text = "Clestra is thinking...",
                                    color = ClestraTextSoft,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Soft UI Composer Dock or Blocked Banner
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .neumorphicRaised(
                            cornerRadius = 28.dp,
                            elevation = 5.dp,
                            containerColor = ClestraNeuSurface
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "You blocked this user.",
                            color = ClestraError,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        NeumorphicButton(
                            onClick = { viewModel.toggleBlockPeer(peerId) },
                            cornerRadius = 14.dp,
                            containerColor = ClestraCopper,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("Unblock", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .neumorphicRaised(
                            cornerRadius = 28.dp,
                            elevation = 5.dp,
                            containerColor = ClestraNeuSurface
                        )
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    // Reply / Edit active strip
                    val activeReply = replyingToMessage
                    val activeEdit = editingMessage
                    AnimatedVisibility(visible = activeReply != null || activeEdit != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 10.dp, top = 10.dp)
                                .neumorphicPressed(
                                    cornerRadius = 14.dp,
                                    containerColor = ClestraNeuWell
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(ClestraCopper)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (activeEdit != null) {
                                        "Editing message"
                                    } else {
                                        "Replying to ${if (activeReply?.isOutgoing == true) "yourself" else peerName}"
                                    },
                                    color = ClestraCopperDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = (activeEdit?.content ?: activeReply?.content).orEmpty()
                                        .ifBlank { activeReply?.attachmentName ?: "Attachment" },
                                    color = ClestraTextSoft,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    replyingToMessage = null
                                    if (editingMessage != null) {
                                        editingMessage = null
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = ClestraTextSoft,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Expandable Attachment Action Tray (+ Photo / File / GIFs)
                    AnimatedVisibility(visible = showAttachTray) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AttachOptionPill(
                                icon = Icons.Default.Image,
                                label = "Photo",
                                tint = ClestraCopper,
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            AttachOptionPill(
                                icon = Icons.Default.AttachFile,
                                label = "File",
                                tint = ClestraOnline,
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            AttachOptionPill(
                                icon = Icons.Default.EmojiEmotions,
                                label = "GIF / Sticker",
                                tint = ClestraCharcoal,
                                onClick = {
                                    showAttachTray = false
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showExpressionPanel = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (isRecordingVoice) {
                        ClestraLiveRecordingStrip(
                            elapsedMs = recordingElapsedMs,
                            liveAmplitudes = liveVoiceAmplitudes,
                            onCancel = {
                                voiceRecorder.cancel()
                                isRecordingVoice = false
                                recordingElapsedMs = 0L
                            },
                            onSend = {
                                val note = voiceRecorder.stopAndSave()
                                isRecordingVoice = false
                                recordingElapsedMs = 0L
                                if (note != null) {
                                    val reply = replyingToMessage
                                    viewModel.sendVoiceMessage(
                                        recorded = note,
                                        replyTo = reply,
                                        replySenderName = if (reply?.isOutgoing == true) "You" else peerName
                                    )
                                    replyingToMessage = null
                                } else {
                                    Toast.makeText(context, "Voice note too short", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    showAttachTray = !showAttachTray
                                    if (showAttachTray) {
                                        showExpressionPanel = false
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp)
                                    .then(
                                        if (showAttachTray) {
                                            Modifier.neumorphicPressed(
                                                cornerRadius = 20.dp,
                                                containerColor = ClestraNeuWell
                                            )
                                        } else {
                                            Modifier.neumorphicRaised(
                                                cornerRadius = 20.dp,
                                                elevation = 4.dp,
                                                containerColor = ClestraNeuSurface
                                            )
                                        }
                                    )
                                    .testTag("attach_button")
                            ) {
                                Icon(
                                    imageVector = if (showAttachTray) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = "Attach photo or file",
                                    tint = if (showAttachTray) ClestraCopper else ClestraCharcoal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    showAttachTray = false
                                    if (!showExpressionPanel) {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        showExpressionPanel = true
                                    } else {
                                        showExpressionPanel = false
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp)
                                    .then(
                                        if (showExpressionPanel) {
                                            Modifier.neumorphicPressed(
                                                cornerRadius = 20.dp,
                                                containerColor = ClestraNeuWell
                                            )
                                        } else {
                                            Modifier.neumorphicRaised(
                                                cornerRadius = 20.dp,
                                                elevation = 4.dp,
                                                containerColor = ClestraNeuSurface
                                            )
                                        }
                                    )
                                    .testTag("expression_panel_toggle")
                            ) {
                                Icon(
                                    imageVector = if (showExpressionPanel) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                                    contentDescription = if (showExpressionPanel) "Hide expressions" else "Emojis, Stickers & GIFs",
                                    tint = if (showExpressionPanel) ClestraCopper else ClestraCharcoal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = when {
                                            editingMessage != null -> "Update message..."
                                            isOfficial -> "Ask Clestra anything..."
                                            else -> "Message..."
                                        },
                                        color = ClestraTextSoft,
                                        fontSize = 14.sp
                                    )
                                },
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp),
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
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .neumorphicPressed(
                                        cornerRadius = 22.dp,
                                        containerColor = ClestraNeuWell
                                    )
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            showExpressionPanel = false
                                            showAttachTray = false
                                        }
                                    }
                                    .testTag("chat_input_field")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            val showSendButton = inputText.isNotBlank() || editingMessage != null
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(42.dp)
                                    .neumorphicRaised(
                                        cornerRadius = 21.dp,
                                        elevation = 4.dp,
                                        containerColor = if (showSendButton) ClestraCopper else ClestraNeuSurface
                                    )
                                    .clip(CircleShape)
                                    .clickable {
                                        if (showSendButton) {
                                            if (inputText.isNotBlank()) {
                                                val editTarget = editingMessage
                                                if (editTarget != null) {
                                                    viewModel.editMessage(editTarget, inputText)
                                                    editingMessage = null
                                                } else {
                                                    val reply = replyingToMessage
                                                    viewModel.sendMessage(
                                                        content = inputText,
                                                        replyTo = reply,
                                                        replySenderName = if (reply?.isOutgoing == true) "You" else peerName
                                                    )
                                                    replyingToMessage = null
                                                }
                                                inputText = ""
                                            }
                                        } else {
                                            val hasMic = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasMic) {
                                                showAttachTray = false
                                                showExpressionPanel = false
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                if (voiceRecorder.start()) {
                                                    recordingElapsedMs = 0L
                                                    isRecordingVoice = true
                                                } else {
                                                    Toast.makeText(context, "Unable to start microphone", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                    .testTag(if (showSendButton) "send_button" else "record_voice_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        editingMessage != null -> Icons.Default.Check
                                        showSendButton -> Icons.AutoMirrored.Filled.Send
                                        else -> Icons.Default.Mic
                                    },
                                    contentDescription = when {
                                        editingMessage != null -> "Save Edit"
                                        showSendButton -> "Send Message"
                                        else -> "Record Voice Message"
                                    },
                                    tint = if (showSendButton) Color.White else ClestraCopperDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showExpressionPanel) {
                        ExpressionPanel(
                            stickers = stickers,
                            gifItems = gifItems,
                            selectedGifProvider = selectedGifProvider,
                            gifQuery = gifQuery,
                            isLoadingGifs = isLoadingGifs,
                            gifNotice = gifNotice,
                            onSelectEmoji = { emoji ->
                                inputText += emoji
                            },
                            onBackspaceEmoji = {
                                if (inputText.isNotEmpty()) {
                                    val bi = java.text.BreakIterator.getCharacterInstance()
                                    bi.setText(inputText)
                                    bi.last()
                                    val prev = bi.previous()
                                    if (prev != java.text.BreakIterator.DONE) {
                                        inputText = inputText.substring(0, prev)
                                    }
                                }
                            },
                            onSendSticker = { sticker ->
                                val reply = replyingToMessage
                                viewModel.sendMessage(
                                    content = sticker.label,
                                    mediaUri = sticker.uri,
                                    messageType = "STICKER",
                                    replyTo = reply,
                                    replySenderName = if (reply?.isOutgoing == true) "You" else peerName
                                )
                                replyingToMessage = null
                            },
                            onCreateCustomSticker = { label, uri, styleTag ->
                                viewModel.createCustomSticker(label, uri, styleTag)
                            },
                            onDeleteSticker = { stickerId ->
                                viewModel.deleteSticker(stickerId)
                            },
                            onSelectGifProvider = { provider ->
                                viewModel.selectGifProvider(provider)
                            },
                            onGifQueryChange = { query ->
                                viewModel.updateGifQuery(query)
                            },
                            onSendGif = { gif ->
                                val reply = replyingToMessage
                                viewModel.sendMessage(
                                    content = gif.title,
                                    mediaUri = gif.fullUrl,
                                    messageType = "GIF",
                                    replyTo = reply,
                                    replySenderName = if (reply?.isOutgoing == true) "You" else peerName
                                )
                                replyingToMessage = null
                                showExpressionPanel = false
                            }
                        )
                    }
                }
            }
        }

        // Full-Screen Media / Embedded Image Preview with Download
        if (previewMediaUri != null) {
            val activePreview = previewMediaUri!!
            val displayModel = remember(activePreview) { FileAttachmentUtils.toDisplayModel(activePreview) }
            BackHandler { previewMediaUri = null }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                AsyncImage(
                    model = displayModel,
                    imageLoader = gifImageLoader,
                    contentDescription = "Media preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val ok = FileAttachmentUtils.saveImageToGallery(context, activePreview, "clestra_photo")
                                Toast.makeText(
                                    context,
                                    if (ok) "Saved to Pictures/Clestra" else "Unable to save photo",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save image",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { previewMediaUri = null },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
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

        // Full-Screen Code & Text Document Viewer
        fullScreenDoc?.let { doc ->
            FullScreenCodeViewerModal(
                doc = doc,
                onClose = { fullScreenDoc = null },
                onDownloadFile = { uri, fileName, mimeType ->
                    scope.launch {
                        val ok = FileAttachmentUtils.saveFileToDownloads(context, uri, fileName, mimeType)
                        Toast.makeText(
                            context,
                            if (ok) "Saved to Downloads/Clestra" else "Unable to save file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
    }

    if (showMuteDialog) {
        MuteDurationDialog(
            targetName = peerName,
            isCurrentlyMuted = isMuted,
            onDismiss = { showMuteDialog = false },
            onSelectDuration = { duration -> viewModel.mutePeer(peerId, duration) },
            onUnmute = { viewModel.unmutePeer(peerId) }
        )
    }
}

@Composable
private fun AttachOptionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .neumorphicRaised(
                cornerRadius = 14.dp,
                elevation = 3.dp,
                containerColor = ClestraNeuSurface
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = ClestraCharcoal,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isOfficialBotChat: Boolean = false,
    senderActiveTag: String? = null,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onToggleStar: () -> Unit,
    onStartEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenFullScreenDoc: (FullScreenDocumentData) -> Unit,
    onSaveImage: (uri: String, title: String) -> Unit,
    onSaveFile: (uri: String, fileName: String, mimeType: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gifImageLoader = remember(context) { CoilGifSupport.getImageLoader(context) }
    val clipboard = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    val isSent = message.isOutgoing
    val isBotIncoming = !isSent && isOfficialBotChat
    val isSticker = message.messageType == "STICKER"
    val isGif = message.messageType == "GIF"
    val isVoice = message.messageType == "VOICE"
    val isImage = message.messageType == "IMAGE"
    val isFile = message.messageType == "FILE"

    val topStartCorner = if (isSent) 22.dp else 6.dp
    val topEndCorner = if (isSent) 6.dp else 22.dp
    val bubbleShape = RoundedCornerShape(
        topStart = topStartCorner,
        topEnd = topEndCorner,
        bottomStart = 22.dp,
        bottomEnd = 22.dp
    )

    // Balanced #ECF0F3 / warm Soft UI tonal surfaces — no blown-out top white sheen
    val (topColor, bottomColor) = when {
        isSent -> Color(0xFFF3E6D8) to Color(0xFFEBDBC9)
        isBotIncoming -> ClestraNeuSurface to Color(0xFFE5EAEF)
        else -> ClestraNeuSurface to Color(0xFFE6ECF1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isSent) 52.dp else 14.dp,
                end = if (isSent) 14.dp else 52.dp,
                bottom = 10.dp
            ),
        horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
    ) {
        Box {
            if (isSticker) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(
                            onClick = { showMenu = true },
                            onLongClick = { showMenu = true }
                        )
                        .testTag("message_bubble_${message.id}")
                ) {
                    StickerVisual(
                        uri = message.mediaUri,
                        label = message.content,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(min = if (isVoice) 198.dp else 96.dp, max = if (isVoice) 252.dp else 312.dp)
                        .neumorphicBubble(
                            topStart = topStartCorner,
                            topEnd = topEndCorner,
                            bottomStart = 22.dp,
                            bottomEnd = 22.dp,
                            elevation = 4.dp,
                            topColor = topColor,
                            bottomColor = bottomColor,
                            accentBorderColor = if (isBotIncoming) ClestraCopper.copy(alpha = 0.28f) else null
                        )
                        .clip(bubbleShape)
                        .combinedClickable(
                            onClick = { showMenu = true },
                            onLongClick = { showMenu = true }
                        )
                        .testTag("message_bubble_${message.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = when {
                                isGif -> 6.dp
                                isVoice -> 10.dp
                                else -> 13.dp
                            },
                            vertical = when {
                                isGif -> 6.dp
                                isVoice -> 8.dp
                                else -> 10.dp
                            }
                        )
                    ) {
                        if (isBotIncoming) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
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

                        // Reply / Quote preview strip inside bubble
                        if (!message.replyToPreview.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.055f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(ClestraCopper)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.replyToSenderName ?: "Reply",
                                        color = ClestraCopperDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = message.replyToPreview,
                                        color = ClestraTextSoft,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        when {
                            isVoice -> {
                                val audioMeta = remember(message.attachmentMimeType) {
                                    AudioMetaCodec.parseMeta(message.attachmentMimeType)
                                }
                                ClestraAudioPlayerBar(
                                    uriString = message.mediaUri,
                                    waveform = audioMeta.waveform,
                                    fallbackDurationMs = audioMeta.durationMs,
                                    surfaceColor = topColor
                                )
                            }

                            isFile -> {
                                FileAttachmentCard(
                                    message = message,
                                    onOpenFullScreenFile = onOpenFullScreenDoc,
                                    onDownloadFile = onSaveFile
                                )
                            }

                            !message.mediaUri.isNullOrBlank() && (isImage || isGif || message.messageType == "IMAGE") -> {
                                val displayModel = remember(message.mediaUri) {
                                    FileAttachmentUtils.toDisplayModel(message.mediaUri)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(195.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenMedia(message.mediaUri) }
                                ) {
                                    AsyncImage(
                                        model = displayModel,
                                        imageLoader = gifImageLoader,
                                        contentDescription = if (isGif) "GIF: ${message.content}" else "Photo attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isGif) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ClestraCharcoal.copy(alpha = 0.72f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = "GIF",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        // Embedded image quick Save-to-Gallery button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.92f))
                                                .clickable {
                                                    onSaveImage(
                                                        message.mediaUri,
                                                        message.attachmentName ?: "clestra_photo"
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Save photo",
                                                tint = Color(0xFF14110F),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (!isGif && message.content.isNotBlank() && message.content != "Photo attachment") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    MarkdownMessageContent(
                                        rawText = message.content,
                                        onOpenFullScreenCode = onOpenFullScreenDoc
                                    )
                                }
                            }

                            message.content.isNotBlank() -> {
                                MarkdownMessageContent(
                                    rawText = message.content,
                                    onOpenFullScreenCode = onOpenFullScreenDoc
                                )
                            }
                        }
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
                    text = { Text("Reply") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onReply()
                    }
                )

                DropdownMenuItem(
                    text = { Text(if (message.isStarred) "Unstar Message" else "Star Message") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (message.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (message.isStarred) ClestraCopper else ClestraCharcoal
                        )
                    },
                    onClick = {
                        showMenu = false
                        onToggleStar()
                    }
                )

                if (isSent && message.messageType == "TEXT") {
                    DropdownMenuItem(
                        text = { Text("Edit Message") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onStartEdit()
                        }
                    )
                }

                if (message.content.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Copy Text") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            clipboard.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                            showMenu = false
                        }
                    )
                }

                if (!message.mediaUri.isNullOrBlank() && (isImage || isFile)) {
                    DropdownMenuItem(
                        text = { Text(if (isImage) "Save Photo to Gallery" else "Save File to Downloads") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            if (isImage) {
                                onSaveImage(message.mediaUri, message.attachmentName ?: "clestra_photo")
                            } else {
                                onSaveFile(
                                    message.mediaUri,
                                    message.attachmentName ?: message.content,
                                    message.attachmentMimeType
                                )
                            }
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Delete Message", color = ClestraError) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ClestraError)
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }

        if (!message.reaction.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .neumorphicRaised(
                        cornerRadius = 14.dp,
                        elevation = 3.dp,
                        containerColor = ClestraNeuSurface
                    )
                    .clip(CircleShape)
                    .clickable { onReact("") }
            ) {
                Text(
                    text = message.reaction,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Clean timestamp + sender tag + star + edited + delivery checkmark indicators (NO E2EE text)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
        ) {
            if (isSent && !senderActiveTag.isNullOrBlank()) {
                com.example.ui.components.SoftUiBadgeTag(label = senderActiveTag)
                Spacer(modifier = Modifier.width(5.dp))
            }
            if (message.isStarred) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Starred",
                    tint = ClestraCopper,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = formatTimeSimple(message.timestamp) + if (message.isEdited) " • Edited" else "",
                color = ClestraTextSoft,
                fontSize = 10.sp
            )
            if (isSent) {
                Spacer(modifier = Modifier.width(4.dp))
                val isDelivered = message.status == "DELIVERED" || message.status == "READ"
                Icon(
                    imageVector = if (isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                    contentDescription = if (isDelivered) "Delivered" else "Sent",
                    tint = if (message.status == "READ") ClestraOnline else ClestraTextSoft,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

fun formatTimeSimple(millis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
