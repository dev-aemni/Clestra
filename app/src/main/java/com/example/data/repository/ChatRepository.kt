package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import com.example.data.model.ClestraIdGenerator
import com.example.data.model.ConversationEntity
import com.example.data.model.CustomStickerEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PeerEntity
import com.example.data.model.SendMessagePayload
import com.example.data.model.UserAccountEntity
import com.example.data.network.CryptoUtils
import com.example.data.network.NetworkClient
import com.example.data.network.NotificationHelper
import com.example.data.network.OpenRouterClient
import com.example.data.network.OpenRouterMessage
import com.example.data.network.RelayKeepaliveJobService
import com.example.data.security.InternalVault
import com.example.ui.components.CustomTagCodec
import com.example.ui.components.StickerStorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        const val OFFICIAL_BOT_ID = "CLS-OFFICIAL-CLESTRA"
        const val OFFICIAL_BOT_NAME = "Clestra"
    }

    private val database = AppDatabase.getInstance(context)
    private val accountDao = database.accountDao()
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val peerDao = database.peerDao()
    private val stickerDao = database.stickerDao()

    // Account State
    val accountFlow: Flow<UserAccountEntity?> = accountDao.getAccountFlow()
    val isAccountSetup: Flow<Boolean> = accountFlow.map { it != null }

    val currentAccountState: StateFlow<UserAccountEntity?> = accountFlow
        .stateIn(scope, SharingStarted.Eagerly, null)

    // Check secrets injected via BuildConfig
    val mongoDbSecret: String = try {
        BuildConfig.MONGODB_URI
    } catch (e: Exception) {
        ""
    }

    private val openRouterApiKey: String = try {
        BuildConfig.OPENROUTER_API_KEY
    } catch (e: Exception) {
        ""
    }

    val serverlessUrl = MutableStateFlow(RelayKeepaliveJobService.getRelayUrl(context))
    val isSimulationFallbackEnabled = MutableStateFlow(true)

    // Foreground & active conversation tracking for out-of-app notifications
    private val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    private val _activeConversationPeerId = MutableStateFlow<String?>(null)

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    // Sync status reporting (never expose raw API errors)
    private val _syncStatus = MutableStateFlow("Ready")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncCount = MutableStateFlow(0)
    val lastSyncCount: StateFlow<Int> = _lastSyncCount.asStateFlow()

    // In-memory serverless store for local offline testing (stores encrypted payloads only)
    private val mockServerlessPendingMessages = mutableListOf<SendMessagePayload>()

    init {
        NotificationHelper.ensureChannel(context)
        RelayKeepaliveJobService.saveRelayConfig(context, serverlessUrl.value)
        RelayKeepaliveJobService.scheduleKeepalive(context)
        scope.launch {
            ensureOfficialClestraBotSeeded()
            ensureDefaultStickersSeeded()
            getCurrentAccount()?.let { acct ->
                RelayKeepaliveJobService.saveRelayConfig(context, serverlessUrl.value, acct.id)
            }
            RelayKeepaliveJobService.sendInstantDeletePulseIfDue(context)
        }
        // Background polling for queued relay messages while online
        scope.launch {
            while (isActive) {
                delay(20_000)
                if (getCurrentAccount() != null && NotificationHelper.isDeviceOnline(context)) {
                    syncOfflineMessagesForUser()
                }
            }
        }
    }

    fun updateServerlessUrl(newUrl: String) {
        val clean = newUrl.trim().ifBlank { "https://clestra-relay.vercel.app" }
        serverlessUrl.value = clean
        RelayKeepaliveJobService.saveRelayConfig(context, clean)
        RelayKeepaliveJobService.scheduleKeepalive(context)
        scope.launch {
            RelayKeepaliveJobService.sendInstantDeletePulseIfDue(context, force = true)
        }
    }

    fun setAppInForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
    }

    fun setActiveConversationPeerId(peerId: String?) {
        _activeConversationPeerId.value = peerId
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    fun getAllPeers(): Flow<List<PeerEntity>> = peerDao.getAllPeers()
    fun getMessagesForConversation(convoId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(convoId)

    fun getRelayMessagesCount(): Flow<Int> = messageDao.getRelayMessagesCount()
    fun getP2PMessagesCount(): Flow<Int> = messageDao.getP2PMessagesCount()
    fun getAllStickers(): Flow<List<CustomStickerEntity>> = stickerDao.getAllStickers()

    suspend fun ensureDefaultStickersSeeded() {
        if (stickerDao.getStickerCount() == 0) {
            stickerDao.insertStickers(StickerStorageUtils.defaultStickerEntities())
        }
    }

    suspend fun addCustomSticker(label: String, uri: String, styleTag: String = "OUTLINE") {
        val sticker = CustomStickerEntity(
            id = "stk_custom_${UUID.randomUUID()}",
            label = label.trim().ifBlank { "Custom Sticker" },
            uri = uri,
            packName = "My Stickers",
            isCustom = true,
            styleTag = styleTag,
            createdAt = System.currentTimeMillis()
        )
        stickerDao.insertSticker(sticker)
    }

    suspend fun deleteSticker(id: String) {
        stickerDao.deleteSticker(id)
    }

    suspend fun getConversation(peerId: String): ConversationEntity? =
        conversationDao.getConversation(peerId)

    suspend fun getPeer(peerId: String): PeerEntity? = peerDao.getPeer(peerId)

    suspend fun clearUnread(peerId: String) = conversationDao.clearUnread(peerId)

    suspend fun ensureOfficialClestraBotSeeded() {
        val existingBot = peerDao.getPeer(OFFICIAL_BOT_ID)
        if (existingBot == null) {
            val botPeer = PeerEntity(
                peerId = OFFICIAL_BOT_ID,
                displayName = OFFICIAL_BOT_NAME,
                bio = "Official Clestra Assistant",
                avatarColor = 0xFFC4763A,
                avatarIcon = "C",
                isOnline = true,
                directAddress = InternalVault.botAddress(),
                isOfficial = true
            )
            peerDao.upsertPeer(botPeer)
        }

        val existingConvo = conversationDao.getConversation(OFFICIAL_BOT_ID)
        if (existingConvo == null) {
            val welcomeText = "Hi! I'm Clestra, your official built-in assistant. Ask me anything about your Clestra ID, code snippets, files, or app features."
            val encryptedWelcome = CryptoUtils.encryptPayload(welcomeText, OFFICIAL_BOT_ID, "CLS-LOCAL")
            val welcomeMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = OFFICIAL_BOT_ID,
                senderId = OFFICIAL_BOT_ID,
                recipientId = "CLS-LOCAL",
                content = welcomeText,
                compressedContent = encryptedWelcome,
                timestamp = System.currentTimeMillis(),
                deliveryRoute = "P2P_DIRECT",
                status = "DELIVERED",
                isOutgoing = false
            )
            messageDao.insertMessage(welcomeMsg)
            conversationDao.upsertConversation(
                ConversationEntity(
                    peerId = OFFICIAL_BOT_ID,
                    peerName = OFFICIAL_BOT_NAME,
                    peerAvatarColor = 0xFFC4763A,
                    peerAvatarIcon = "C",
                    lastMessage = welcomeText,
                    lastTimestamp = welcomeMsg.timestamp,
                    unreadCount = 1,
                    isPeerOnline = true,
                    lastDeliveryRoute = "P2P_DIRECT",
                    isOfficial = true,
                    isPinned = true
                )
            )
        }
    }

    /**
     * Create permanent user account with timestamp-anchored, uncollidable Clestra ID.
     */
    suspend fun setupAccount(
        name: String,
        avatarIcon: String = "🍑",
        avatarColor: Long = 0xFFC4763A,
        password: String,
        username: String = ""
    ): UserAccountEntity {
        val cleanName = name.trim()
        val cleanUsername = username.trim().ifBlank { cleanName.lowercase().replace(" ", "_") }
        val uniqueId = ClestraIdGenerator.generateUniqueClestraId(password, cleanUsername)
        val passwordHash = ClestraIdGenerator.hashPassword(password)
        val account = UserAccountEntity(
            id = uniqueId,
            displayName = cleanName,
            username = cleanUsername,
            bio = "Private. Secure. Yours.",
            avatarIcon = avatarIcon,
            avatarColor = avatarColor,
            passwordHash = passwordHash,
            createdAt = System.currentTimeMillis()
        )
        accountDao.saveAccount(account)
        RelayKeepaliveJobService.saveRelayConfig(context, serverlessUrl.value, uniqueId)
        ensureOfficialClestraBotSeeded()
        return account
    }

    suspend fun updateProfilePhoto(photoUri: String?) {
        accountDao.updatePhotoUri(photoUri)
    }

    suspend fun updateProfileBio(bio: String) {
        accountDao.updateBio(bio)
    }

    suspend fun updateDisplayName(displayName: String) {
        val clean = displayName.trim()
        if (clean.isNotBlank()) {
            accountDao.updateDisplayName(clean)
        }
    }

    suspend fun redeemTagCode(rawCode: String): Pair<Boolean, String> {
        val account = accountDao.getAccount() ?: return false to "Account not ready"
        val unlocked = InternalVault.verifyRedeemCode(rawCode)
            ?: CustomTagCodec.parseCustomRedeemTag(rawCode)
            ?: return false to "Invalid redeem code or tag:NAME@#HEX format"

        val targetName = CustomTagCodec.extractTagName(unlocked)
        val updatedList = account.unlockedTags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && CustomTagCodec.extractTagName(it) != targetName }
            .toMutableList()
        updatedList.add(unlocked)

        accountDao.updateAccountTags(
            activeTag = unlocked,
            unlockedTags = updatedList.joinToString(",")
        )
        return true to "$targetName tag unlocked and activated!"
    }

    suspend fun selectActiveTag(tag: String?) {
        val account = accountDao.getAccount() ?: return
        val currentList = account.unlockedTags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val target = tag?.takeIf { it in currentList }
        accountDao.updateAccountTags(
            activeTag = target,
            unlockedTags = currentList.joinToString(",")
        )
    }

    suspend fun getCurrentAccount(): UserAccountEntity? = accountDao.getAccount()

    /**
     * Send encrypted message (supports Text, Markdown/Code, Embedded Images, Files, GIFs, Stickers, and Replies).
     */
    suspend fun sendMessage(
        recipientPeerId: String,
        content: String,
        mediaUri: String? = null,
        messageType: String = "TEXT",
        attachmentName: String? = null,
        attachmentMimeType: String? = null,
        attachmentSizeBytes: Long = 0L,
        transportMediaUri: String? = null,
        replyToMessageId: String? = null,
        replyToPreview: String? = null,
        replyToSenderName: String? = null
    ): MessageEntity? {
        val peer = peerDao.getPeer(recipientPeerId)
        if (peer?.isBlocked == true) {
            return null
        }

        val myAccount = getCurrentAccount()
        val myId = myAccount?.id ?: "CLS-ANONYMOUS"
        val isOnline = peer?.isOnline ?: false
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val effectiveType = when {
            messageType != "TEXT" -> messageType
            mediaUri != null -> "IMAGE"
            else -> "TEXT"
        }
        val effectiveContent = content.ifBlank {
            when (effectiveType) {
                "GIF" -> "GIF"
                "STICKER" -> "Sticker"
                "IMAGE" -> "Photo attachment"
                "FILE" -> attachmentName ?: "File attachment"
                else -> ""
            }
        }

        val embeddedUri = transportMediaUri ?: mediaUri.orEmpty()
        val transportPlaintext = when (effectiveType) {
            "TEXT" -> effectiveContent
            "FILE" -> "FILE::$effectiveContent::$embeddedUri::${attachmentMimeType.orEmpty()}::$attachmentSizeBytes"
            else -> "$effectiveType::$effectiveContent::$embeddedUri"
        }

        // Encrypt payload with AES-256-GCM for both P2P and MongoDB relay transport
        val encryptedPayload = CryptoUtils.encryptPayload(transportPlaintext, myId, recipientPeerId)

        val route = if (isOnline || recipientPeerId == OFFICIAL_BOT_ID) "P2P_DIRECT" else "SERVERLESS_RELAY"
        val initialStatus = if (route == "P2P_DIRECT") "DELIVERED" else "SENT"

        val message = MessageEntity(
            id = messageId,
            conversationId = recipientPeerId,
            senderId = myId,
            recipientId = recipientPeerId,
            content = effectiveContent,
            compressedContent = encryptedPayload,
            timestamp = now,
            deliveryRoute = route,
            status = initialStatus,
            isOutgoing = true,
            mediaUri = mediaUri ?: transportMediaUri,
            messageType = effectiveType,
            attachmentName = attachmentName,
            attachmentMimeType = attachmentMimeType,
            attachmentSizeBytes = attachmentSizeBytes,
            replyToMessageId = replyToMessageId,
            replyToPreview = replyToPreview,
            replyToSenderName = replyToSenderName
        )

        messageDao.insertMessage(message)

        val previewText = when (effectiveType) {
            "GIF" -> "GIF"
            "STICKER" -> "Sticker"
            "IMAGE" -> if (content.isNotBlank() && content != "Photo attachment") "Photo: $content" else "Photo"
            "FILE" -> "File: ${attachmentName ?: effectiveContent}"
            else -> effectiveContent
        }

        val existingConvo = conversationDao.getConversation(recipientPeerId)
        val peerName = peer?.displayName ?: existingConvo?.peerName ?: "Clestra Peer"
        val peerAvatarColor = peer?.avatarColor ?: 0xFFC4763A
        val peerAvatarIcon = peer?.avatarIcon ?: "🍑"
        val updatedConvo = ConversationEntity(
            peerId = recipientPeerId,
            peerName = peerName,
            peerAvatarColor = peerAvatarColor,
            peerAvatarIcon = peerAvatarIcon,
            lastMessage = previewText,
            lastTimestamp = now,
            unreadCount = 0,
            isPeerOnline = isOnline || recipientPeerId == OFFICIAL_BOT_ID,
            lastDeliveryRoute = route,
            isOfficial = peer?.isOfficial ?: false,
            isMuted = peer?.isMuted ?: false,
            mutedUntil = peer?.mutedUntil,
            isPinned = existingConvo?.isPinned ?: (peer?.isOfficial == true),
            isBlocked = peer?.isBlocked ?: false
        )
        conversationDao.upsertConversation(updatedConvo)

        when {
            recipientPeerId == OFFICIAL_BOT_ID -> {
                val botPromptText = when (effectiveType) {
                    "GIF" -> "[User sent a GIF: $effectiveContent]"
                    "STICKER" -> "[User sent a sticker: $effectiveContent]"
                    "IMAGE" -> "[User sent an embedded photo]"
                    "FILE" -> "[User shared file: ${attachmentName ?: effectiveContent}]"
                    else -> effectiveContent
                }
                scope.launch {
                    replyAsClestraBot(myAccount, botPromptText)
                }
            }
            isOnline -> {
                scope.launch {
                    delay(900)
                    simulatePeerP2PReply(recipientPeerId, encryptedPayload, myId)
                }
            }
            else -> {
                scope.launch {
                    dispatchToServerlessRelay(message)
                }
            }
        }

        return message
    }

    private suspend fun replyAsClestraBot(
        userAccount: UserAccountEntity?,
        userMessage: String
    ) {
        _isBotTyping.value = true
        try {
            val freshAccount = getCurrentAccount() ?: userAccount
            val userName = freshAccount?.displayName ?: "Friend"
            val userId = freshAccount?.id ?: "CLS-USER"

            val recent = messageDao.getRecentMessages(OFFICIAL_BOT_ID, 12)
                .reversed()
                .map { msg ->
                    OpenRouterMessage(
                        role = if (msg.isOutgoing) "user" else "assistant",
                        content = msg.content
                    )
                }

            val workspaceContext = buildLiveWorkspaceSnapshot(freshAccount)
            val systemPrompt = buildString {
                append(OpenRouterClient.buildClestraSystemPrompt(userName, userId))
                append("\n\n")
                append(workspaceContext)
            }

            val requestMessages = buildList {
                add(OpenRouterMessage(role = "system", content = systemPrompt))
                if (recent.isEmpty()) {
                    add(OpenRouterMessage(role = "user", content = userMessage))
                } else {
                    addAll(recent)
                }
            }

            val rawModelReply: String? = if (openRouterApiKey.isBlank() || openRouterApiKey == "MY_OPENROUTER_API_KEY") {
                null
            } else {
                try {
                    val response = OpenRouterClient.service.chatCompletion(
                        authorization = "Bearer $openRouterApiKey",
                        referer = InternalVault.refererHeader(),
                        request = com.example.data.network.OpenRouterRequest(
                            model = OpenRouterClient.MODEL_ID,
                            messages = requestMessages
                        )
                    )
                    response.choices
                        ?.firstOrNull()
                        ?.message
                        ?.content
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    null
                }
            }

            val replyText = if (rawModelReply != null) {
                val parsed = ClestraBotActionEngine.parseModelResponse(rawModelReply)
                val execLogs = parsed.actions.map { executeBotAction(it) }
                val baseProse = parsed.visibleReply
                when {
                    execLogs.isEmpty() -> baseProse
                    baseProse.isBlank() -> execLogs.joinToString("\n")
                    else -> "$baseProse\n\n${execLogs.joinToString("\n")}"
                }
            } else {
                val offlineCmd = ClestraBotActionEngine.parseOfflineCommand(userMessage)
                if (offlineCmd != null) {
                    val (intro, actions) = offlineCmd
                    val execLogs = actions.map { executeBotAction(it) }
                    buildString {
                        if (intro.isNotBlank()) {
                            append(intro)
                            if (execLogs.isNotEmpty()) append("\n\n")
                        }
                        append(execLogs.joinToString("\n"))
                    }.trim()
                } else {
                    buildLocalClestraGuidanceReply(userName, userId, userMessage)
                }
            }

            val peer = peerDao.getPeer(OFFICIAL_BOT_ID)
            if (peer?.isBlocked == true) return

            val myId = freshAccount?.id ?: "CLS-ANONYMOUS"
            val encryptedReply = CryptoUtils.encryptPayload(replyText, OFFICIAL_BOT_ID, myId)
            val replyMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = OFFICIAL_BOT_ID,
                senderId = OFFICIAL_BOT_ID,
                recipientId = myId,
                content = replyText,
                compressedContent = encryptedReply,
                timestamp = System.currentTimeMillis(),
                deliveryRoute = "P2P_DIRECT",
                status = "DELIVERED",
                isOutgoing = false
            )

            messageDao.insertMessage(replyMsg)

            val isViewingBotChat = _isAppInForeground.value && _activeConversationPeerId.value == OFFICIAL_BOT_ID
            val currentConvo = conversationDao.getConversation(OFFICIAL_BOT_ID)
            val nextUnread = if (isViewingBotChat) 0 else (currentConvo?.unreadCount ?: 0) + 1

            conversationDao.upsertConversation(
                ConversationEntity(
                    peerId = OFFICIAL_BOT_ID,
                    peerName = OFFICIAL_BOT_NAME,
                    peerAvatarColor = 0xFFC4763A,
                    peerAvatarIcon = "C",
                    lastMessage = replyText,
                    lastTimestamp = replyMsg.timestamp,
                    unreadCount = nextUnread,
                    isPeerOnline = true,
                    lastDeliveryRoute = "P2P_DIRECT",
                    isOfficial = true,
                    isMuted = peer?.isMuted ?: false,
                    mutedUntil = peer?.mutedUntil,
                    isPinned = currentConvo?.isPinned ?: true,
                    isBlocked = false
                )
            )

            maybeNotifyIncomingMessage(OFFICIAL_BOT_ID, OFFICIAL_BOT_NAME, replyText)
        } finally {
            _isBotTyping.value = false
        }
    }

    private suspend fun buildLiveWorkspaceSnapshot(account: UserAccountEntity?): String {
        val peersList = peerDao.getAllPeers().first()
        val convosList = conversationDao.getAllConversations().first()
        return buildString {
            appendLine("LIVE WORKSPACE SNAPSHOT:")
            appendLine("- Account: name=${account?.displayName ?: "User"}, id=${account?.id ?: "CLS-USER"}, bio=${account?.bio ?: ""}, activeTag=${account?.activeTag ?: "none"}, unlockedTags=${account?.unlockedTags ?: "none"}")
            appendLine("- Contacts (${peersList.size}):")
            peersList.take(20).forEach { p ->
                val convo = convosList.firstOrNull { it.peerId == p.peerId }
                appendLine("  * ${p.displayName} [id=${p.peerId}, online=${p.isOnline}, muted=${p.isMuted}, blocked=${p.isBlocked}, pinned=${convo?.isPinned ?: false}, unread=${convo?.unreadCount ?: 0}, last=${convo?.lastMessage?.take(60) ?: ""}]")
            }
            val peerConvos = peersList.filter { it.peerId != OFFICIAL_BOT_ID }.take(5)
            if (peerConvos.isNotEmpty()) {
                appendLine("- Recent peer chat excerpts:")
                peerConvos.forEach { p ->
                    val msgs = messageDao.getRecentMessages(p.peerId, 3).reversed()
                    if (msgs.isNotEmpty()) {
                        appendLine("  [${p.displayName} (${p.peerId})]: " + msgs.joinToString(" | ") { m ->
                            "${if (m.isOutgoing) "You" else p.displayName}: ${m.content.take(70)}"
                        })
                    }
                }
            }
        }
    }

    private suspend fun findPeerByQuery(query: String?): PeerEntity? {
        val q = query?.trim().orEmpty()
        if (q.isEmpty()) return null
        val allPeers = peerDao.getAllPeers().first()
        return allPeers.firstOrNull { it.peerId.equals(q, ignoreCase = true) }
            ?: allPeers.firstOrNull { it.displayName.equals(q, ignoreCase = true) }
            ?: allPeers.firstOrNull {
                it.peerId != OFFICIAL_BOT_ID &&
                    (it.displayName.contains(q, ignoreCase = true) || it.peerId.contains(q, ignoreCase = true))
            }
    }

    private suspend fun executeBotAction(directive: BotActionDirective): String {
        return when (directive.action) {
            "send_message" -> {
                val query = directive.peer ?: return "Skipped: missing peer for send_message."
                val text = directive.text ?: return "Skipped: missing message text."
                var target = findPeerByQuery(query)
                if (target == null && query.startsWith("CLS-", ignoreCase = true)) {
                    addPeer(query.uppercase(), directive.name ?: query.uppercase())
                    target = findPeerByQuery(query)
                }
                if (target == null) {
                    "Could not find contact '$query'."
                } else if (target.peerId == OFFICIAL_BOT_ID) {
                    "Skipped sending to Clestra assistant loop."
                } else if (target.isBlocked) {
                    "${target.displayName} is blocked — unblock them first to send."
                } else {
                    sendMessage(recipientPeerId = target.peerId, content = text)
                    "Sent encrypted message to **${target.displayName}** (`${target.peerId}`)."
                }
            }

            "read_chats" -> {
                val query = directive.peer
                if (!query.isNullOrBlank()) {
                    val target = findPeerByQuery(query)
                        ?: return "No chat or contact found matching '$query'."
                    val recent = messageDao.getRecentMessages(target.peerId, 6).reversed()
                    if (recent.isEmpty()) {
                        "No messages yet in **${target.displayName}** (`${target.peerId}`)."
                    } else {
                        buildString {
                            appendLine("**${target.displayName}** (`${target.peerId}`) — last ${recent.size} messages:")
                            recent.forEach { m ->
                                val who = if (m.isOutgoing) "You" else target.displayName
                                appendLine("- **$who**: ${m.content}")
                            }
                        }.trim()
                    }
                } else {
                    val convos = conversationDao.getAllConversations().first()
                        .filter { it.peerId != OFFICIAL_BOT_ID }
                    if (convos.isEmpty()) {
                        "You don't have any peer conversations yet."
                    } else {
                        buildString {
                            appendLine("Your active conversations (${convos.size}):")
                            convos.take(10).forEach { c ->
                                val flags = listOfNotNull(
                                    if (c.isPinned) "Pinned" else null,
                                    if (c.isMuted) "Muted" else null,
                                    if (c.isBlocked) "Blocked" else null
                                ).joinToString(", ")
                                val extra = if (flags.isNotEmpty()) " [$flags]" else ""
                                appendLine("- **${c.peerName}** (`${c.peerId}`)$extra: ${c.lastMessage.take(70)}")
                            }
                        }.trim()
                    }
                }
            }

            "edit_last_message" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: conversationDao.getAllConversations().first()
                        .firstOrNull { it.peerId != OFFICIAL_BOT_ID }
                        ?.let { peerDao.getPeer(it.peerId) }
                    ?: return "Could not find a conversation to edit."
                val newText = directive.text ?: return "Missing new text to edit."
                val lastOutgoing = messageDao.getRecentMessages(target.peerId, 20)
                    .firstOrNull { it.isOutgoing && it.messageType == "TEXT" }
                    ?: return "No outgoing text message found in **${target.displayName}**."
                editMessage(lastOutgoing, newText)
                "Edited your last message in **${target.displayName}**."
            }

            "delete_last_message" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: conversationDao.getAllConversations().first()
                        .firstOrNull { it.peerId != OFFICIAL_BOT_ID }
                        ?.let { peerDao.getPeer(it.peerId) }
                    ?: return "Could not find a conversation to delete from."
                val lastMsg = messageDao.getRecentMessages(target.peerId, 5).firstOrNull()
                    ?: return "No messages to delete in **${target.displayName}**."
                deleteMessage(lastMsg.id)
                "Deleted the latest message in **${target.displayName}**."
            }

            "clear_chat" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Specify which contact's chat to clear."
                clearConversationHistory(target.peerId)
                "Cleared message history with **${target.displayName}**."
            }

            "delete_chat" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find contact '${directive.peer}'."
                if (target.peerId == OFFICIAL_BOT_ID) return "Official Clestra chat cannot be deleted."
                deletePeerAndConversation(target.peerId)
                "Deleted chat & contact **${target.displayName}**."
            }

            "block_peer" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find contact '${directive.peer}'."
                if (target.peerId == OFFICIAL_BOT_ID) return "Official Clestra assistant cannot be blocked."
                if (!target.isBlocked) {
                    toggleBlockPeer(target.peerId)
                }
                "Blocked **${target.displayName}** (`${target.peerId}`)."
            }

            "unblock_peer" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find contact '${directive.peer}'."
                if (target.isBlocked) {
                    toggleBlockPeer(target.peerId)
                }
                "Unblocked **${target.displayName}** (`${target.peerId}`)."
            }

            "mute_peer" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find contact '${directive.peer}'."
                val durationMs = directive.minutes?.takeIf { it > 0L }?.let { it * 60_000L }
                mutePeer(target.peerId, durationMs)
                val suffix = directive.minutes?.let { " for ${it}m" } ?: " indefinitely"
                "Muted notifications for **${target.displayName}**$suffix."
            }

            "unmute_peer" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find contact '${directive.peer}'."
                unmutePeer(target.peerId)
                "Unmuted notifications for **${target.displayName}**."
            }

            "pin_chat" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find chat '${directive.peer}'."
                if (target.peerId == OFFICIAL_BOT_ID) return "Official Clestra chat is already pinned."
                val convo = conversationDao.getConversation(target.peerId)
                if (convo != null && !convo.isPinned) {
                    conversationDao.updatePinned(target.peerId, true)
                }
                "Pinned chat with **${target.displayName}**."
            }

            "unpin_chat" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find chat '${directive.peer}'."
                if (target.peerId == OFFICIAL_BOT_ID) return "Official Clestra chat stays pinned."
                val convo = conversationDao.getConversation(target.peerId)
                if (convo != null && convo.isPinned) {
                    conversationDao.updatePinned(target.peerId, false)
                }
                "Unpinned chat with **${target.displayName}**."
            }

            "set_peer_online" -> {
                val target = findPeerByQuery(directive.peer)
                    ?: return "Could not find contact '${directive.peer}'."
                val wantOnline = directive.online ?: !target.isOnline
                peerDao.updateOnlineStatus(target.peerId, wantOnline)
                conversationDao.updatePeerOnlineStatus(target.peerId, wantOnline)
                val modeLabel = if (wantOnline) "P2P Online" else "Offline Relay"
                "Switched **${target.displayName}** to **$modeLabel**."
            }

            "add_contact" -> {
                val id = directive.peer?.trim()?.uppercase()
                    ?: return "Please provide a Clestra ID (`CLS-...`)."
                val name = directive.name?.trim()?.ifBlank { null } ?: id
                addPeer(id, name)
                "Added contact **$name** (`$id`)."
            }

            "update_bio" -> {
                val bio = directive.text ?: return "Missing bio text."
                updateProfileBio(bio)
                "Updated your profile bio to: _$bio _"
            }

            "update_name" -> {
                val name = directive.text ?: return "Missing display name."
                updateDisplayName(name)
                "Updated your display name to **$name**."
            }

            "redeem_tag" -> {
                val code = directive.code ?: directive.text ?: return "Missing redeem code."
                val (_, msg) = redeemTagCode(code)
                msg
            }

            "select_tag" -> {
                val desired = directive.tag?.trim().orEmpty()
                if (desired.isEmpty()) {
                    selectActiveTag(null)
                    "Cleared your active profile badge."
                } else {
                    val acct = getCurrentAccount()
                    val matched = acct?.unlockedTags
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.firstOrNull {
                            it.equals(desired, ignoreCase = true) ||
                                CustomTagCodec.extractTagName(it).equals(desired, ignoreCase = true)
                        }
                    if (matched != null) {
                        selectActiveTag(matched)
                        "Activated badge **${CustomTagCodec.extractTagName(matched)}**."
                    } else {
                        "Badge '$desired' isn't unlocked yet."
                    }
                }
            }

            "sync_relay" -> {
                val count = syncOfflineMessagesForUser()
                RelayKeepaliveJobService.sendInstantDeletePulseIfDue(context, force = true)
                "Synced Serverless Relay ($count new messages pulled)."
            }

            else -> "Action '${directive.action}' is not recognized."
        }
    }

    private fun buildLocalClestraGuidanceReply(
        userName: String,
        userId: String,
        userMessage: String
    ): String {
        val q = userMessage.lowercase()
        return when {
            q.contains("code") || q.contains("markdown") || q.contains("kotlin") || q.contains("python") -> """
Hi $userName! Clestra renders Markdown with fenced code blocks and syntax highlighting:

```kotlin
fun greetClestra(id: String): String {
    val status = "Connected"
    println("Node ${'$'}id -> ${'$'}status")
    return status
}
```

Tap **Full** on any code box to inspect all lines with line numbers!
            """.trimIndent()
            q.contains("id") -> "Hi $userName! Your permanent Clestra ID is `$userId`. Anyone can message you immediately by entering this ID in **Contacts** or the **+** New Chat button."
            q.contains("mute") || q.contains("notification") -> "You can mute notifications for **1 hour**, **8 hours**, **24 hours**, or **Always** — or simply tell me e.g. `mute Alex for 1h` or `unmute Alex`!"
            else -> "Hi $userName! I'm Clestra, your official built-in assistant. I can manage your chats directly — try asking me to:\n- `add contact CLS-... as Name`\n- `send Hello! to Alex`\n- `read chats` or `read messages with Alex`\n- `edit last message with Alex to Updated text`\n- `delete last message in Alex` or `clear chat with Alex`\n- `block Alex` / `mute Alex for 1h` / `pin Alex`\n- `set bio to Building cool apps` or `redeem tag:VIP@#FF6B35`"
        }
    }

    private suspend fun dispatchToServerlessRelay(message: MessageEntity) {
        val payload = SendMessagePayload(
            senderId = message.senderId,
            recipientId = message.recipientId,
            compressedContent = message.compressedContent,
            timestamp = message.timestamp
        )

        try {
            val service = NetworkClient.createRelayService(serverlessUrl.value)
            val response = service.sendOfflineMessage(payload)
            if (response.success) {
                _syncStatus.value = "Queued in Relay"
            } else {
                throw Exception("Relay queue deferred")
            }
        } catch (_: Exception) {
            if (isSimulationFallbackEnabled.value) {
                synchronized(mockServerlessPendingMessages) {
                    mockServerlessPendingMessages.add(payload)
                }
                _syncStatus.value = "Queued in Offline Relay"
            } else {
                _syncStatus.value = "Offline"
            }
        }
    }

    /**
     * Pull queued encrypted messages from Serverless store,
     * filter blocked senders, decrypt locally, and notify if outside the app.
     */
    suspend fun syncOfflineMessagesForUser(recipientId: String? = null): Int {
        val myId = recipientId ?: (getCurrentAccount()?.id ?: return 0)
        _syncStatus.value = "Syncing messages..."
        var count = 0

        try {
            val service = NetworkClient.createRelayService(serverlessUrl.value)
            val response = service.syncOfflineMessages(myId)

            if (response.success && response.messages.isNotEmpty()) {
                val incomingList = response.messages.mapNotNull { item ->
                    val senderPeer = peerDao.getPeer(item.senderId)
                    if (senderPeer?.isBlocked == true) return@mapNotNull null

                    val decrypted = CryptoUtils.decryptPayload(
                        cipherPayload = item.compressedContent,
                        senderId = item.senderId,
                        recipientId = item.recipientId
                    )
                    val decoded = decodeIncomingPayload(decrypted)
                    MessageEntity(
                        id = item.id ?: UUID.randomUUID().toString(),
                        conversationId = item.senderId,
                        senderId = item.senderId,
                        recipientId = item.recipientId,
                        content = decoded.content,
                        compressedContent = item.compressedContent,
                        timestamp = item.timestamp,
                        deliveryRoute = "SERVERLESS_RELAY",
                        status = "DELIVERED",
                        isOutgoing = false,
                        mediaUri = decoded.uri,
                        messageType = decoded.type,
                        attachmentName = if (decoded.type == "FILE") decoded.content else null,
                        attachmentMimeType = decoded.mimeType,
                        attachmentSizeBytes = decoded.sizeBytes
                    )
                }

                if (incomingList.isNotEmpty()) {
                    messageDao.insertMessages(incomingList)
                    count = incomingList.size
                    updateConversationsAfterSync(incomingList)
                    notifyForSyncedMessages(incomingList)
                }
                _syncStatus.value = "Synced $count messages"
            } else {
                _syncStatus.value = "Up to date"
            }
        } catch (_: Exception) {
            if (isSimulationFallbackEnabled.value) {
                val matched = synchronized(mockServerlessPendingMessages) {
                    val matching = mockServerlessPendingMessages.filter { it.recipientId == myId }
                    mockServerlessPendingMessages.removeAll { it.recipientId == myId }
                    matching
                }

                if (matched.isNotEmpty()) {
                    val incomingList = matched.mapNotNull { item ->
                        val senderPeer = peerDao.getPeer(item.senderId)
                        if (senderPeer?.isBlocked == true) return@mapNotNull null

                        val decrypted = CryptoUtils.decryptPayload(
                            cipherPayload = item.compressedContent,
                            senderId = item.senderId,
                            recipientId = item.recipientId
                        )
                        val decoded = decodeIncomingPayload(decrypted)
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = item.senderId,
                            senderId = item.senderId,
                            recipientId = item.recipientId,
                            content = decoded.content,
                            compressedContent = item.compressedContent,
                            timestamp = item.timestamp,
                            deliveryRoute = "SERVERLESS_RELAY",
                            status = "DELIVERED",
                            isOutgoing = false,
                            mediaUri = decoded.uri,
                            messageType = decoded.type,
                            attachmentName = if (decoded.type == "FILE") decoded.content else null,
                            attachmentMimeType = decoded.mimeType,
                            attachmentSizeBytes = decoded.sizeBytes
                        )
                    }
                    if (incomingList.isNotEmpty()) {
                        messageDao.insertMessages(incomingList)
                        count = incomingList.size
                        updateConversationsAfterSync(incomingList)
                        notifyForSyncedMessages(incomingList)
                    }
                    _syncStatus.value = "Synced $count messages"
                } else {
                    _syncStatus.value = "Ready"
                }
            } else {
                _syncStatus.value = "Offline"
            }
        }

        _lastSyncCount.value = count
        return count
    }

    private suspend fun notifyForSyncedMessages(incoming: List<MessageEntity>) {
        incoming.groupBy { it.conversationId }.forEach { (peerId, msgs) ->
            val latest = msgs.maxByOrNull { it.timestamp } ?: return@forEach
            val peer = peerDao.getPeer(peerId)
            val senderName = peer?.displayName ?: "Peer $peerId"
            maybeNotifyIncomingMessage(peerId, senderName, latest.content)
        }
    }

    private suspend fun updateConversationsAfterSync(incoming: List<MessageEntity>) {
        incoming.groupBy { it.conversationId }.forEach { (peerId, msgs) ->
            val latest = msgs.maxByOrNull { it.timestamp } ?: return@forEach
            val peer = peerDao.getPeer(peerId)
            val currentConvo = conversationDao.getConversation(peerId)
            val isViewing = _isAppInForeground.value && _activeConversationPeerId.value == peerId
            val unread = if (isViewing) 0 else (currentConvo?.unreadCount ?: 0) + msgs.size

            conversationDao.upsertConversation(
                ConversationEntity(
                    peerId = peerId,
                    peerName = peer?.displayName ?: "Peer $peerId",
                    peerAvatarColor = peer?.avatarColor ?: 0xFFC4763A,
                    peerAvatarIcon = peer?.avatarIcon ?: "🍑",
                    lastMessage = latest.content,
                    lastTimestamp = latest.timestamp,
                    unreadCount = unread,
                    isPeerOnline = peer?.isOnline ?: false,
                    lastDeliveryRoute = "SERVERLESS_RELAY",
                    isOfficial = peer?.isOfficial ?: false,
                    isMuted = peer?.isMuted ?: false,
                    mutedUntil = peer?.mutedUntil,
                    isPinned = currentConvo?.isPinned ?: false,
                    isBlocked = peer?.isBlocked ?: false
                )
            )
        }
    }

    suspend fun togglePeerOnline(peerId: String) {
        val currentPeer = peerDao.getPeer(peerId) ?: return
        val newOnline = !currentPeer.isOnline
        peerDao.updateOnlineStatus(peerId, newOnline)
        conversationDao.updatePeerOnlineStatus(peerId, newOnline)
    }

    suspend fun toggleMutePeer(peerId: String) {
        val currentPeer = peerDao.getPeer(peerId) ?: return
        if (currentPeer.isMuted) {
            unmutePeer(peerId)
        } else {
            mutePeer(peerId, null)
        }
    }

    suspend fun mutePeer(peerId: String, durationMillis: Long?) {
        val until = if (durationMillis != null && durationMillis > 0L) {
            System.currentTimeMillis() + durationMillis
        } else {
            null
        }
        peerDao.updateMuted(peerId, isMuted = true, mutedUntil = until)
        conversationDao.updateMuted(peerId, isMuted = true, mutedUntil = until)
    }

    suspend fun unmutePeer(peerId: String) {
        peerDao.updateMuted(peerId, isMuted = false, mutedUntil = null)
        conversationDao.updateMuted(peerId, isMuted = false, mutedUntil = null)
    }

    suspend fun togglePinConversation(peerId: String) {
        if (peerId == OFFICIAL_BOT_ID) return
        val current = conversationDao.getConversation(peerId) ?: return
        conversationDao.updatePinned(peerId, !current.isPinned)
    }

    suspend fun toggleBlockPeer(peerId: String) {
        if (peerId == OFFICIAL_BOT_ID) return
        val currentPeer = peerDao.getPeer(peerId) ?: return
        val newBlocked = !currentPeer.isBlocked
        peerDao.updateBlocked(peerId, newBlocked)
        conversationDao.updateBlocked(peerId, newBlocked)
    }

    suspend fun clearConversationHistory(peerId: String) {
        messageDao.clearMessagesForConversation(peerId)
        val convo = conversationDao.getConversation(peerId)
        if (convo != null) {
            conversationDao.upsertConversation(
                convo.copy(
                    lastMessage = "Conversation cleared",
                    lastTimestamp = System.currentTimeMillis(),
                    unreadCount = 0
                )
            )
        }
    }

    suspend fun deletePeerAndConversation(peerId: String) {
        if (peerId == OFFICIAL_BOT_ID) return
        messageDao.clearMessagesForConversation(peerId)
        conversationDao.deleteConversation(peerId)
        peerDao.deletePeer(peerId)
    }

    suspend fun updateMessageReaction(messageId: String, reaction: String?) {
        messageDao.updateMessageReaction(messageId, reaction)
    }

    suspend fun updateMessageStarred(messageId: String, isStarred: Boolean) {
        messageDao.updateMessageStarred(messageId, isStarred)
    }

    suspend fun editMessage(message: MessageEntity, newContent: String) {
        val clean = newContent.trim()
        if (clean.isBlank()) return
        val encrypted = CryptoUtils.encryptPayload(clean, message.senderId, message.recipientId)
        messageDao.updateMessageContent(message.id, clean, encrypted)
    }

    suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    suspend fun addPeer(peerId: String, name: String, avatarIcon: String = "🍑") {
        val cleanId = peerId.trim().uppercase()
        val existing = peerDao.getPeer(cleanId)
        if (existing != null) return

        val colors = listOf(0xFFC4763A, 0xFFA05C28, 0xFFE8D5C0, 0xFFF5D5B8)
        val color = colors[kotlin.math.abs(cleanId.hashCode()) % colors.size]
        val peer = PeerEntity(
            peerId = cleanId,
            displayName = name,
            bio = "Clestra Node • ID: $cleanId",
            avatarColor = color,
            avatarIcon = avatarIcon,
            isOnline = true,
            directAddress = "p2p://$cleanId.clestra.mesh"
        )
        peerDao.upsertPeer(peer)

        val convo = ConversationEntity(
            peerId = cleanId,
            peerName = name,
            peerAvatarColor = color,
            peerAvatarIcon = avatarIcon,
            lastMessage = "Chat started",
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isPeerOnline = true,
            lastDeliveryRoute = "P2P_DIRECT"
        )
        conversationDao.upsertConversation(convo)
    }

    suspend fun testRelayConnection(): String {
        return try {
            val service = NetworkClient.createRelayService(serverlessUrl.value)
            val health = service.getHealth()
            "Connected (${health.service ?: "Relay"})"
        } catch (_: Exception) {
            "Offline Relay Simulator active"
        }
    }

    private suspend fun simulatePeerP2PReply(
        peerId: String,
        encryptedUserPayload: String,
        myId: String
    ) {
        val peer = peerDao.getPeer(peerId) ?: return
        if (!peer.isOnline || peer.isBlocked) return

        val rawDecrypted = CryptoUtils.decryptPayload(encryptedUserPayload, myId, peerId)
        val decoded = decodeIncomingPayload(rawDecrypted)
        val replyText = when {
            decoded.type == "GIF" -> "Nice GIF!"
            decoded.type == "STICKER" -> "Awesome sticker!"
            decoded.type == "IMAGE" -> "Got your embedded photo!"
            decoded.type == "FILE" -> "Received your file: ${decoded.content}"
            decoded.content.contains("hello", ignoreCase = true) || decoded.content.contains("hi", ignoreCase = true) ->
                "Hey! Got your message directly."
            decoded.content.contains("id", ignoreCase = true) ->
                "Your Clestra ID looks great!"
            else ->
                "Got it! Received directly."
        }

        val encryptedReply = CryptoUtils.encryptPayload(replyText, peerId, myId)
        val decryptedOnReceive = CryptoUtils.decryptPayload(encryptedReply, peerId, myId)

        val replyMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = peerId,
            senderId = peerId,
            recipientId = myId,
            content = decryptedOnReceive,
            compressedContent = encryptedReply,
            timestamp = System.currentTimeMillis(),
            deliveryRoute = "P2P_DIRECT",
            status = "DELIVERED",
            isOutgoing = false
        )

        messageDao.insertMessage(replyMsg)

        val isViewing = _isAppInForeground.value && _activeConversationPeerId.value == peerId
        val currentConvo = conversationDao.getConversation(peerId)
        val unread = if (isViewing) 0 else (currentConvo?.unreadCount ?: 0) + 1

        conversationDao.upsertConversation(
            ConversationEntity(
                peerId = peerId,
                peerName = peer.displayName,
                peerAvatarColor = peer.avatarColor,
                peerAvatarIcon = peer.avatarIcon,
                lastMessage = decryptedOnReceive,
                lastTimestamp = replyMsg.timestamp,
                unreadCount = unread,
                isPeerOnline = true,
                lastDeliveryRoute = "P2P_DIRECT",
                isOfficial = peer.isOfficial,
                isMuted = peer.isMuted,
                mutedUntil = peer.mutedUntil,
                isPinned = currentConvo?.isPinned ?: false,
                isBlocked = peer.isBlocked
            )
        )

        maybeNotifyIncomingMessage(peerId, peer.displayName, decryptedOnReceive)
    }

    private data class DecodedIncomingPayload(
        val type: String,
        val content: String,
        val uri: String?,
        val mimeType: String? = null,
        val sizeBytes: Long = 0L
    )

    private fun decodeIncomingPayload(decrypted: String): DecodedIncomingPayload {
        if (decrypted.startsWith("FILE::")) {
            val parts = decrypted.removePrefix("FILE::").split("::", limit = 4)
            return DecodedIncomingPayload(
                type = "FILE",
                content = parts.getOrNull(0)?.ifBlank { "Attachment" } ?: "Attachment",
                uri = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
                mimeType = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                sizeBytes = parts.getOrNull(3)?.toLongOrNull() ?: 0L
            )
        }
        val prefixes = listOf("GIF::", "STICKER::", "IMAGE::")
        for (prefix in prefixes) {
            if (decrypted.startsWith(prefix)) {
                val type = prefix.removeSuffix("::")
                val rest = decrypted.removePrefix(prefix)
                val parts = rest.split("::", limit = 2)
                val content = parts.getOrNull(0)?.ifBlank { type } ?: type
                val uri = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                return DecodedIncomingPayload(type, content, uri)
            }
        }
        return DecodedIncomingPayload("TEXT", decrypted, null)
    }

    /**
     * Send notification if the user is online, outside the app (backgrounded),
     * peer is not blocked, and notification mute (including timed mute) is not active.
     */
    private suspend fun maybeNotifyIncomingMessage(
        peerId: String,
        senderName: String,
        messageText: String
    ) {
        val peer = peerDao.getPeer(peerId)
        if (peer?.isBlocked == true) return
        if (peer?.isMuted == true) {
            val until = peer.mutedUntil
            if (until == null || System.currentTimeMillis() <= until) {
                return
            } else {
                // Timed mute has expired — automatically restore notifications
                unmutePeer(peerId)
            }
        }

        val isOutsideApp = !_isAppInForeground.value
        val isOnline = NotificationHelper.isDeviceOnline(context)
        if (isOutsideApp && isOnline) {
            NotificationHelper.showMessageNotification(
                context = context,
                peerId = peerId,
                senderName = senderName,
                messageText = messageText
            )
        }
    }
}
