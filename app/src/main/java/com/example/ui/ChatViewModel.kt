package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PeerEntity
import com.example.data.model.UserAccountEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    // Filter tab (0: All, 1: Online P2P, 2: Offline Relay)
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

    fun startDirectChatById(
        peerIdInput: String,
        displayNameInput: String = "",
        onReady: (String) -> Unit = {}
    ) {
        val cleanId = peerIdInput.trim().uppercase()
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

    fun sendMessage(content: String, mediaUri: String? = null) {
        val peerId = _activePeerId.value ?: return
        if (content.isBlank() && mediaUri == null) return

        viewModelScope.launch {
            repository.sendMessage(peerId, content.trim(), mediaUri)
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
        repository.serverlessUrl.value = newUrl
    }

    fun testRelayConnection() {
        viewModelScope.launch {
            _relayTestResult.value = "Pinging relay endpoint..."
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

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }
}
