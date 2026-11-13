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

    private val _activeConversation = MutableStateFlow<ConversationEntity?>(null)
    val activeConversation: StateFlow<ConversationEntity?> = _activeConversation.asStateFlow()

    private val _activePeer = MutableStateFlow<PeerEntity?>(null)
    val activePeer: StateFlow<PeerEntity?> = _activePeer.asStateFlow()

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
                1 -> convo.isPeerOnline // Online P2P
                2 -> !convo.isPeerOnline // Offline Relay
                else -> true // All
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

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _relayTestResult = MutableStateFlow<String?>(null)
    val relayTestResult: StateFlow<String?> = _relayTestResult.asStateFlow()

    private val _friendRequestStatus = MutableStateFlow("")
    val friendRequestStatus: StateFlow<String> = _friendRequestStatus.asStateFlow()

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

    fun sendFriendRequest(usernameOrId: String) {
        val trimmed = usernameOrId.trim()
        if (trimmed.isBlank()) {
            _friendRequestStatus.value = "Please enter a username or Clestra ID."
            return
        }
        viewModelScope.launch {
            val normalizedId = if (trimmed.startsWith("CLS-", ignoreCase = true)) {
                trimmed.uppercase()
            } else {
                "CLS-${trimmed.uppercase().take(4).padEnd(4, 'X')}-PEER"
            }
            val displayName = if (trimmed.startsWith("CLS-", ignoreCase = true)) trimmed else trimmed
            repository.addPeer(normalizedId, displayName)
            _friendRequestStatus.value = "Request sent & connected with $displayName ($normalizedId)"
        }
    }

    fun selectConversation(peerId: String) {
        _activePeerId.value = peerId
        viewModelScope.launch {
            repository.clearUnread(peerId)
            _activeConversation.value = repository.getConversation(peerId)
            _activePeer.value = repository.getPeer(peerId)
        }
    }

    fun closeConversation() {
        _activePeerId.value = null
        _activeConversation.value = null
        _activePeer.value = null
    }

    fun sendMessage(content: String, mediaUri: String? = null) {
        val peerId = _activePeerId.value ?: return
        if (content.isBlank() && mediaUri == null) return

        viewModelScope.launch {
            repository.sendMessage(peerId, content.trim(), mediaUri)
            _activeConversation.value = repository.getConversation(peerId)
            _activePeer.value = repository.getPeer(peerId)
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
            _activeConversation.value = repository.getConversation(peerId)
            _activePeer.value = repository.getPeer(peerId)
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
