package com.example.myandroidapp.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.firebase.FirebaseSocialService
import com.example.myandroidapp.data.model.ChatMessageEntity
import com.example.myandroidapp.data.model.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════
// ── Conversation Preview Model ──
// ═══════════════════════════════════════════════════════

data class ConversationPreview(
    val otherUser: UserProfileEntity,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int
)

data class InboxUiState(
    val conversations: List<ConversationPreview> = emptyList(),
    val unreadTotal: Int = 0,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val selectedConversation: UserProfileEntity? = null,
    val chatMessages: List<ChatMessageEntity> = emptyList()
)

// ═══════════════════════════════════════════════════════
// ── Inbox ViewModel ──
// ═══════════════════════════════════════════════════════

class InboxViewModel(
    private val firebase: FirebaseSocialService,
    private val currentMemberId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            firebase.observeAllConversations(currentMemberId).collect { messages ->
                // Group messages by conversation partner
                val grouped = messages.groupBy { msg ->
                    if (msg.senderId == currentMemberId) msg.receiverId else msg.senderId
                }

                val previews = grouped.map { (partnerId, msgs) ->
                    val sortedMsgs = msgs.sortedByDescending { it.sentAt }
                    val lastMsg = sortedMsgs.first()
                    val unread = sortedMsgs.count { !it.isRead && it.senderId != currentMemberId }
                    val profile = withContext(Dispatchers.IO) {
                        firebase.getProfile(partnerId)
                    } ?: UserProfileEntity(
                        memberId = partnerId,
                        displayName = "Unknown User"
                    )

                    ConversationPreview(
                        otherUser = profile,
                        lastMessage = lastMsg.message,
                        lastMessageTime = lastMsg.sentAt,
                        unreadCount = unread
                    )
                }.sortedByDescending { it.lastMessageTime }

                _uiState.update {
                    it.copy(
                        conversations = previews,
                        unreadTotal = previews.sumOf { c -> c.unreadCount },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(isSearching = !it.isSearching, searchQuery = "") }
    }

    fun openConversation(profile: UserProfileEntity) {
        _uiState.update { it.copy(selectedConversation = profile) }
        // Mark messages as read
        viewModelScope.launch(Dispatchers.IO) {
            firebase.markMessagesRead(currentMemberId, profile.memberId)
        }
        // Start observing conversation
        viewModelScope.launch {
            firebase.observeConversation(currentMemberId, profile.memberId).collect { msgs ->
                _uiState.update { it.copy(chatMessages = msgs) }
            }
        }
    }

    fun closeConversation() {
        _uiState.update { it.copy(selectedConversation = null, chatMessages = emptyList()) }
    }

    fun sendMessage(text: String) {
        val target = _uiState.value.selectedConversation ?: return
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            firebase.sendMessage(currentMemberId, target.memberId, text)
        }
    }

    val filteredConversations: StateFlow<List<ConversationPreview>> =
        _uiState.map { state ->
            if (state.searchQuery.isBlank()) state.conversations
            else {
                val q = state.searchQuery.lowercase()
                state.conversations.filter { conv ->
                    conv.otherUser.displayName.lowercase().contains(q) ||
                    conv.otherUser.username.lowercase().contains(q) ||
                    conv.lastMessage.lowercase().contains(q)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class InboxViewModelFactory(
    private val firebase: FirebaseSocialService,
    private val currentMemberId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InboxViewModel(firebase, currentMemberId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
