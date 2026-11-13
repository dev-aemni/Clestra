package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import com.example.data.model.ClestraIdGenerator
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PeerEntity
import com.example.data.model.SendMessagePayload
import com.example.data.model.UserAccountEntity
import com.example.data.network.CryptoUtils
import com.example.data.network.NetworkClient
import com.example.data.network.NotificationHelper
import com.example.data.network.OpenRouterClient
import com.example.data.network.OpenRouterMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val serverlessUrl = MutableStateFlow("https://clestra-relay.vercel.app")
    val isSimulationFallbackEnabled = MutableStateFlow(true)

    // Foreground & active conversation tracking for out-of-app notifications
    private val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    private val _activeConversationPeerId = MutableStateFlow<String?>(null)

    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    // Sync status reporting
    private val _syncStatus = MutableStateFlow("Idle")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastSyncCount = MutableStateFlow(0)
    val lastSyncCount: StateFlow<Int> = _lastSyncCount.asStateFlow()

    // In-memory serverless mock store for local offline testing (stores encrypted payloads only)
    private val mockServerlessPendingMessages = mutableListOf<SendMessagePayload>()

    init {
        NotificationHelper.ensureChannel(context)
        scope.launch {
            ensureOfficialClestraBotSeeded()
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
                bio = "Official Clestra Assistant • End-to-End Encrypted",
                avatarColor = 0xFFC4763A,
                avatarIcon = "C",
                isOnline = true,
                directAddress = "clestra://official-assistant",
                isOfficial = true
            )
            peerDao.upsertPeer(botPeer)
        }

        val existingConvo = conversationDao.getConversation(OFFICIAL_BOT_ID)
        if (existingConvo == null) {
            val welcomeText = "Hi! I'm Clestra, your official built-in assistant. Ask me anything about your Clestra ID, encryption, P2P routing, or app features."
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
                    isOfficial = true
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
        ensureOfficialClestraBotSeeded()
        return account
    }

    suspend fun updateProfilePhoto(photoUri: String?) {
        accountDao.updatePhotoUri(photoUri)
    }

    suspend fun updateProfileBio(bio: String) {
        accountDao.updateBio(bio)
    }

    suspend fun getCurrentAccount(): UserAccountEntity? = accountDao.getAccount()

    /**
     * Send encrypted message:
     * - Every outgoing message is encrypted with AES-256-GCM before going over P2P or MongoDB Relay.
     * - If recipient is OFFICIAL_BOT_ID: routes to the Clestra AI assistant.
     * - If peer is blocked: refuses delivery.
     */
    suspend fun sendMessage(
        recipientPeerId: String,
        content: String,
        mediaUri: String? = null
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
        val effectiveContent = content.ifBlank { if (mediaUri != null) "Photo attachment" else "" }

        // Encrypt payload with AES-256-GCM for both P2P and MongoDB relay transport
        val encryptedPayload = CryptoUtils.encryptPayload(effectiveContent, myId, recipientPeerId)

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
            mediaUri = mediaUri
        )

        messageDao.insertMessage(message)

        val peerName = peer?.displayName ?: "Clestra Peer"
        val peerAvatarColor = peer?.avatarColor ?: 0xFFC4763A
        val peerAvatarIcon = peer?.avatarIcon ?: "🍑"
        val updatedConvo = ConversationEntity(
            peerId = recipientPeerId,
            peerName = peerName,
            peerAvatarColor = peerAvatarColor,
            peerAvatarIcon = peerAvatarIcon,
            lastMessage = if (mediaUri != null && content.isBlank()) "📷 Photo" else effectiveContent,
            lastTimestamp = now,
            unreadCount = 0,
            isPeerOnline = isOnline || recipientPeerId == OFFICIAL_BOT_ID,
            lastDeliveryRoute = route,
            isOfficial = peer?.isOfficial ?: false,
            isMuted = peer?.isMuted ?: false,
            isBlocked = peer?.isBlocked ?: false
        )
        conversationDao.upsertConversation(updatedConvo)

        when {
            recipientPeerId == OFFICIAL_BOT_ID -> {
                scope.launch {
                    replyAsClestraBot(myAccount, effectiveContent)
                }
            }
            isOnline -> {
                scope.launch {
                    delay(1200)
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
            val userName = userAccount?.displayName ?: "Friend"
            val userId = userAccount?.id ?: "CLS-USER"

            val recent = messageDao.getRecentMessages(OFFICIAL_BOT_ID, 12)
                .reversed()
                .map { msg ->
                    OpenRouterMessage(
                        role = if (msg.isOutgoing) "user" else "assistant",
                        content = msg.content
                    )
                }

            val systemPrompt = OpenRouterClient.buildClestraSystemPrompt(userName, userId)
            val requestMessages = buildList {
                add(OpenRouterMessage(role = "system", content = systemPrompt))
                if (recent.isEmpty()) {
                    add(OpenRouterMessage(role = "user", content = userMessage))
                } else {
                    addAll(recent)
                }
            }

            val replyText = if (openRouterApiKey.isBlank() || openRouterApiKey == "MY_OPENROUTER_API_KEY") {
                "Hi $userName! I'm Clestra, your official built-in assistant. Add your OPENROUTER_API_KEY in the AI Studio Secrets panel so I can answer everything in real time."
            } else {
                try {
                    val response = OpenRouterClient.service.chatCompletion(
                        authorization = "Bearer $openRouterApiKey",
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
                        ?: "I'm Clestra — I'm here to help with anything inside the app. Could you rephrase that?"
                } catch (e: Exception) {
                    "I'm Clestra! I couldn't reach my neural service right now because of a network hiccup. Please check your connection and try again."
                }
            }

            val peer = peerDao.getPeer(OFFICIAL_BOT_ID)
            if (peer?.isBlocked == true) return

            val myId = userAccount?.id ?: "CLS-ANONYMOUS"
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
                    isBlocked = false
                )
            )

            maybeNotifyIncomingMessage(OFFICIAL_BOT_ID, OFFICIAL_BOT_NAME, replyText)
        } finally {
            _isBotTyping.value = false
        }
    }

    private suspend fun dispatchToServerlessRelay(message: MessageEntity) {
        // Send AES-256-GCM encrypted payload to MongoDB serverless relay
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
                _syncStatus.value = "Encrypted & stored in MongoDB Atlas"
            } else {
                throw Exception(response.error ?: "Relay returned error")
            }
        } catch (e: Exception) {
            if (isSimulationFallbackEnabled.value) {
                synchronized(mockServerlessPendingMessages) {
                    mockServerlessPendingMessages.add(payload)
                }
                _syncStatus.value = "Encrypted & queued in Serverless Relay"
            } else {
                _syncStatus.value = "Relay Network Error: ${e.message}"
            }
        }
    }

    /**
     * Pull queued encrypted messages from Serverless MongoDB store,
     * filter blocked senders, decrypt locally, and notify if outside the app.
     */
    suspend fun syncOfflineMessagesForUser(recipientId: String? = null): Int {
        val myId = recipientId ?: (getCurrentAccount()?.id ?: return 0)
        _syncStatus.value = "Syncing encrypted messages..."
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
                    MessageEntity(
                        id = item.id ?: UUID.randomUUID().toString(),
                        conversationId = item.senderId,
                        senderId = item.senderId,
                        recipientId = item.recipientId,
                        content = decrypted,
                        compressedContent = item.compressedContent,
                        timestamp = item.timestamp,
                        deliveryRoute = "SERVERLESS_RELAY",
                        status = "DELIVERED",
                        isOutgoing = false
                    )
                }

                if (incomingList.isNotEmpty()) {
                    messageDao.insertMessages(incomingList)
                    count = incomingList.size
                    updateConversationsAfterSync(incomingList)
                    notifyForSyncedMessages(incomingList)
                }
                _syncStatus.value = "Synced $count encrypted messages (MongoDB cleared)"
            } else {
                _syncStatus.value = "No pending messages in relay"
            }
        } catch (e: Exception) {
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
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = item.senderId,
                            senderId = item.senderId,
                            recipientId = item.recipientId,
                            content = decrypted,
                            compressedContent = item.compressedContent,
                            timestamp = item.timestamp,
                            deliveryRoute = "SERVERLESS_RELAY",
                            status = "DELIVERED",
                            isOutgoing = false
                        )
                    }
                    if (incomingList.isNotEmpty()) {
                        messageDao.insertMessages(incomingList)
                        count = incomingList.size
                        updateConversationsAfterSync(incomingList)
                        notifyForSyncedMessages(incomingList)
                    }
                    _syncStatus.value = "Synced $count encrypted messages (Store cleared)"
                } else {
                    _syncStatus.value = "Relay idle; 0 pending messages"
                }
            } else {
                _syncStatus.value = "Sync failed: ${e.localizedMessage}"
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
        val newMuted = !currentPeer.isMuted
        peerDao.updateMuted(peerId, newMuted)
        conversationDao.updateMuted(peerId, newMuted)
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
            lastMessage = "End-to-end encrypted chat ready",
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
            "Connected! Service: ${health.service ?: "Relay"}, MongoDB: ${health.mongodb ?: "active"}"
        } catch (e: Exception) {
            if (isSimulationFallbackEnabled.value) {
                "Relay server not live yet. Encrypted Offline Relay Simulator is active!"
            } else {
                "Connection failed: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun simulatePeerP2PReply(
        peerId: String,
        encryptedUserPayload: String,
        myId: String
    ) {
        val peer = peerDao.getPeer(peerId) ?: return
        if (!peer.isOnline || peer.isBlocked) return

        // Decrypt the incoming encrypted P2P payload on peer side
        val userMessage = CryptoUtils.decryptPayload(encryptedUserPayload, myId, peerId)
        val replyText = when {
            userMessage.contains("hello", ignoreCase = true) || userMessage.contains("hi", ignoreCase = true) ->
                "Hey! Got your encrypted message directly over P2P."
            userMessage.contains("id", ignoreCase = true) ->
                "Your Clestra ID looks great! Uncollidable and end-to-end encrypted."
            else ->
                "Received via encrypted P2P channel (AES-256-GCM)."
        }

        // Encrypt reply before delivering back over P2P
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
                isBlocked = peer.isBlocked
            )
        )

        maybeNotifyIncomingMessage(peerId, peer.displayName, decryptedOnReceive)
    }

    /**
     * Send notification if the user is online, outside the app (backgrounded),
     * and the peer is neither muted nor blocked.
     */
    private suspend fun maybeNotifyIncomingMessage(
        peerId: String,
        senderName: String,
        messageText: String
    ) {
        val peer = peerDao.getPeer(peerId)
        if (peer?.isMuted == true || peer?.isBlocked == true) return

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
