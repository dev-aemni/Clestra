package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConversationEntity
import com.example.data.model.CustomStickerEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PeerEntity
import com.example.data.model.UserAccountEntity
import com.example.data.network.GifItem
import com.example.data.network.GifProvider
import com.example.data.network.GifProviderManager
import com.example.data.repository.ChatRepository
import com.example.ui.components.CustomTagCodec
import com.example.ui.components.FileAttachmentUtils
import com.example.ui.components.StickerStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChatRepository(application, viewModelScope)

    // User Account State
    val userAccount: StateFlow<UserAccountEntity?> = repository.accountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAccountSetup: StateFlow<Boolean?> = repository.accountFlow
        .combine(flowOf(true)) { account, _ -> account != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mongoDbSecret = repository.mongoDbSecret

    // Current navigation state
    private val _activePeerId = MutableStateFlow<String?>(null)
    val activePeerId: StateFlow<String?> = _activePeerId.asStateFlow()

    val activeConversation: StateFlow<ConversationEntity?> = _activePeerId.flatMapLatest { peerId ->
        if (peerId == null) flowOf(null)
        else repository.getAllConversations().combine(flowOf(peerId)) { list, id ->
            list.firstOrNull { it.peerId == id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePeer: StateFlow<PeerEntity?> = _activePeerId.flatMapLatest { peerId ->
        if (peerId == null) flowOf(null)
        else repository.getAllPeers().combine(flowOf(peerId)) { list, id ->
            list.firstOrNull { it.peerId == id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filter & Search
    val selectedFilterTab = MutableStateFlow(0)
    val searchQuery = MutableStateFlow("")

    val rawConversations = repository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredConversations: StateFlow<List<ConversationEntity>> = combine(
        rawConversations,
        selectedFilterTab,
        searchQuery
    ) { list, tab, query ->
        list.filter { convo ->
            val matchesQuery = query.isBlank() ||
                convo.peerName.contains(query, ignoreCase = true) ||
                convo.lastMessage.contains(query, ignoreCase = true) ||
                convo.peerId.contains(query, ignoreCase = true)

            val matchesTab = when (tab) {
                1 -> convo.isPeerOnline
                2 -> !convo.isPeerOnline
                else -> true
            }
            matchesQuery && matchesTab
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val peers = repository.getAllPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMessages: StateFlow<List<MessageEntity>> = _activePeerId.flatMapLatest { peerId ->
        if (peerId == null) flowOf(emptyList())
        else repository.getMessagesForConversation(peerId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val p2pMessagesCount = repository.getP2PMessagesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val relayMessagesCount = repository.getRelayMessagesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncStatus = repository.syncStatus
    val lastSyncCount = repository.lastSyncCount
    val serverlessUrl = repository.serverlessUrl
    val isBotTyping = repository.isBotTyping

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _relayTestResult = MutableStateFlow<String?>(null)
    val relayTestResult: StateFlow<String?> = _relayTestResult.asStateFlow()

    // Stickers & GIF Search State
    val stickers: StateFlow<List<CustomStickerEntity>> = repository.getAllStickers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGifProvider = MutableStateFlow(GifProvider.ALL)
    val selectedGifProvider: StateFlow<GifProvider> = _selectedGifProvider.asStateFlow()

    private val _gifQuery = MutableStateFlow("")
    val gifQuery: StateFlow<String> = _gifQuery.asStateFlow()

    private val _gifItems = MutableStateFlow<List<GifItem>>(emptyList())
    val gifItems: StateFlow<List<GifItem>> = _gifItems.asStateFlow()

    private val _isLoadingGifs = MutableStateFlow(false)
    val isLoadingGifs: StateFlow<Boolean> = _isLoadingGifs.asStateFlow()

    private val _gifNotice = MutableStateFlow<String?>(null)
    val gifNotice: StateFlow<String?> = _gifNotice.asStateFlow()

    private var gifSearchJob: Job? = null

    init {
        refreshGifs()
    }

    fun setAppInForeground(inForeground: Boolean) {
        repository.setAppInForeground(inForeground)
    }

    fun setupAccount(
        name: String,
        avatarIcon: String = "🍑",
        avatarColor: Long = 0xFFC4763A,
        password: String,
        username: String = ""
    ) {
        viewModelScope.launch {
            repository.setupAccount(
                name = name,
                avatarIcon = avatarIcon,
                avatarColor = avatarColor,
                password = password,
                username = username
            )
        }
    }

    fun updateProfilePhoto(photoUri: String?) {
        viewModelScope.launch {
            repository.updateProfilePhoto(photoUri)
        }
    }

    fun updateProfileBio(bio: String) {
        viewModelScope.launch {
            repository.updateProfileBio(bio)
        }
    }

    private val _redeemStatus = MutableStateFlow<Pair<Boolean, String>?>(null)
    val redeemStatus: StateFlow<Pair<Boolean, String>?> = _redeemStatus.asStateFlow()

    fun redeemTagCode(rawCode: String) {
        viewModelScope.launch {
            _redeemStatus.value = repository.redeemTagCode(rawCode)
        }
    }

    fun equipPublicTag(tag: String) {
        viewModelScope.launch {
            _redeemStatus.value = repository.equipPublicTag(tag)
        }
    }

    fun selectActiveTag(tag: String?) {
        viewModelScope.launch {
            repository.selectActiveTag(tag)
        }
    }

    fun clearRedeemStatus() {
        _redeemStatus.value = null
    }

    fun startDirectChatById(
        peerIdInput: String,
        displayNameInput: String = "",
        onReady: (String) -> Unit = {}
    ) {
        val cleanId = CustomTagCodec.normalizePeerId(peerIdInput)
        if (cleanId.isBlank()) return
        val name = displayNameInput.trim().ifBlank { cleanId }
        viewModelScope.launch {
            repository.addPeer(cleanId, name)
            onReady(cleanId)
        }
    }

    fun selectConversation(peerId: String) {
        _activePeerId.value = peerId
        repository.setActiveConversationPeerId(peerId)
        viewModelScope.launch {
            repository.clearUnread(peerId)
        }
    }

    fun closeConversation() {
        _activePeerId.value = null
        repository.setActiveConversationPeerId(null)
    }

    fun sendMessage(
        content: String,
        mediaUri: String? = null,
        messageType: String = "TEXT",
        replyTo: MessageEntity? = null,
        replySenderName: String? = null
    ) {
        val peerId = _activePeerId.value ?: return
        if (content.isBlank() && mediaUri == null) return

        viewModelScope.launch {
            repository.sendMessage(
                recipientPeerId = peerId,
                content = content.trim(),
                mediaUri = mediaUri,
                messageType = messageType,
                replyToMessageId = replyTo?.id,
                replyToPreview = replyTo?.let { buildReplyPreview(it) },
                replyToSenderName = replySenderName
            )
        }
    }

    fun sendAttachment(
        sourceUri: Uri,
        forceImage: Boolean,
        caption: String = "",
        replyTo: MessageEntity? = null,
        replySenderName: String? = null
    ) {
        val peerId = _activePeerId.value ?: return
        viewModelScope.launch {
            val embedded = FileAttachmentUtils.importAndEmbedAttachment(
                context = getApplication(),
                sourceUri = sourceUri,
                forceImage = forceImage
            ) ?: return@launch

            val type = if (embedded.isImage) "IMAGE" else "FILE"
            val textContent = if (embedded.isImage) caption.trim() else embedded.displayName

            repository.sendMessage(
                recipientPeerId = peerId,
                content = textContent,
                mediaUri = embedded.localFileUri,
                messageType = type,
                attachmentName = embedded.displayName,
                attachmentMimeType = embedded.mimeType,
                attachmentSizeBytes = embedded.sizeBytes,
                transportMediaUri = embedded.transportData,
                replyToMessageId = replyTo?.id,
                replyToPreview = replyTo?.let { buildReplyPreview(it) },
                replyToSenderName = replySenderName
            )
        }
    }

    private fun buildReplyPreview(msg: MessageEntity): String {
        return when (msg.messageType) {
            "IMAGE" -> "Photo"
            "GIF" -> "GIF"
            "STICKER" -> "Sticker"
            "FILE" -> msg.attachmentName ?: msg.content
            else -> msg.content.take(90)
        }
    }

    fun selectGifProvider(provider: GifProvider) {
        _selectedGifProvider.value = provider
        refreshGifs(immediate = true)
    }

    fun updateGifQuery(query: String) {
        _gifQuery.value = query
        refreshGifs(immediate = false)
    }

    fun refreshGifs(immediate: Boolean = true) {
        gifSearchJob?.cancel()
        gifSearchJob = viewModelScope.launch {
            if (!immediate) delay(350)
            _isLoadingGifs.value = true
            val result = GifProviderManager.searchOrTrending(
                provider = _selectedGifProvider.value,
                query = _gifQuery.value
            )
            _gifItems.value = result.items
            _gifNotice.value = null
            _isLoadingGifs.value = false
        }
    }

    fun createCustomSticker(label: String, sourceUri: Uri, styleTag: String = "OUTLINE") {
        viewModelScope.launch {
            val savedUri = withContext(Dispatchers.IO) {
                StickerStorageUtils.createCustomStickerFile(getApplication(), sourceUri, styleTag)
            }
            if (savedUri != null) {
                repository.addCustomSticker(label, savedUri, styleTag)
            }
        }
    }

    fun deleteSticker(id: String) {
        viewModelScope.launch {
            repository.deleteSticker(id)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncOfflineMessagesForUser()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun togglePeerOnline(peerId: String) {
        viewModelScope.launch {
            repository.togglePeerOnline(peerId)
        }
    }

    fun toggleMutePeer(peerId: String) {
        viewModelScope.launch {
            repository.toggleMutePeer(peerId)
        }
    }

    fun mutePeer(peerId: String, durationMillis: Long?) {
        viewModelScope.launch {
            repository.mutePeer(peerId, durationMillis)
        }
    }

    fun unmutePeer(peerId: String) {
        viewModelScope.launch {
            repository.unmutePeer(peerId)
        }
    }

    fun togglePinConversation(peerId: String) {
        viewModelScope.launch {
            repository.togglePinConversation(peerId)
        }
    }

    fun toggleBlockPeer(peerId: String) {
        viewModelScope.launch {
            repository.toggleBlockPeer(peerId)
        }
    }

    fun clearConversationHistory(peerId: String) {
        viewModelScope.launch {
            repository.clearConversationHistory(peerId)
        }
    }

    fun deletePeerAndConversation(peerId: String) {
        viewModelScope.launch {
            repository.deletePeerAndConversation(peerId)
        }
    }

    fun updateServerlessUrl(newUrl: String) {
        repository.updateServerlessUrl(newUrl)
    }

    fun testRelayConnection() {
        viewModelScope.launch {
            _relayTestResult.value = "Checking connection..."
            val result = repository.testRelayConnection()
            _relayTestResult.value = result
        }
    }

    fun addPeer(peerId: String, name: String, avatarIcon: String = "🍑") {
        viewModelScope.launch {
            repository.addPeer(peerId.trim(), name.trim(), avatarIcon)
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            repository.updateMessageReaction(messageId, emoji)
        }
    }

    fun toggleStarMessage(messageId: String, currentStarred: Boolean) {
        viewModelScope.launch {
            repository.updateMessageStarred(messageId, !currentStarred)
        }
    }

    fun editMessage(message: MessageEntity, newContent: String) {
        viewModelScope.launch {
            repository.editMessage(message, newContent)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }
}
