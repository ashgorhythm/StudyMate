package com.example.myandroidapp.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.firebase.FirebaseSocialService
import com.example.myandroidapp.data.model.UserProfileEntity
import com.example.myandroidapp.ui.screens.community.CommunityPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════
// ── UI State for viewing another user's profile ──
// ═══════════════════════════════════════════════════════

enum class FriendshipStatus { NONE, PENDING_SENT, PENDING_RECEIVED, FRIENDS }

data class UserProfileUiState(
    val profile: UserProfileEntity? = null,
    val posts: List<CommunityPost> = emptyList(),
    val friendshipStatus: FriendshipStatus = FriendshipStatus.NONE,
    val isLoading: Boolean = true,
    val isCurrentUser: Boolean = false,
    val postCount: Int = 0,
    val requestSent: Boolean = false,
    val error: String? = null
)

// ═══════════════════════════════════════════════════════
// ── ViewModel ──
// ═══════════════════════════════════════════════════════

class UserProfileViewModel(
    private val firebase: FirebaseSocialService,
    private val currentMemberId: String,
    private val targetMemberId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val isCurrentUser = currentMemberId == targetMemberId

                // Load profile from Firebase
                val profile = withContext(Dispatchers.IO) {
                    firebase.getProfile(targetMemberId)
                }

                if (profile == null) {
                    _uiState.update { it.copy(isLoading = false, error = "User not found") }
                    return@launch
                }

                // Load user's posts
                val allPosts = mutableListOf<CommunityPost>()
                firebase.observePosts().first().forEach { rawPost ->
                    val authorId = rawPost["authorMemberId"] as? String ?: ""
                    if (authorId == targetMemberId) {
                        allPosts.add(rawPost.toUiPost())
                    }
                }

                // Check friendship status
                val friendshipStatus = if (isCurrentUser) {
                    FriendshipStatus.FRIENDS // self
                } else {
                    withContext(Dispatchers.IO) {
                        checkFriendshipStatus()
                    }
                }

                _uiState.update {
                    it.copy(
                        profile = profile,
                        posts = allPosts.sortedByDescending { p -> p.id },
                        postCount = allPosts.size,
                        friendshipStatus = friendshipStatus,
                        isCurrentUser = isCurrentUser,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private suspend fun checkFriendshipStatus(): FriendshipStatus {
        // Check if already friends (accepted requests in either direction)
        try {
            val friends = firebase.observeAcceptedFriends(currentMemberId).first()
            val isFriend = friends.any { req ->
                (req.fromMemberId == currentMemberId && req.toMemberId == targetMemberId) ||
                (req.fromMemberId == targetMemberId && req.toMemberId == currentMemberId)
            }
            if (isFriend) return FriendshipStatus.FRIENDS

            // Check pending requests
            val incoming = firebase.observeIncomingRequests(currentMemberId).first()
            val hasIncoming = incoming.any { it.fromMemberId == targetMemberId }
            if (hasIncoming) return FriendshipStatus.PENDING_RECEIVED

            // We can't directly check outgoing with current API; treat as NONE
            return FriendshipStatus.NONE
        } catch (_: Exception) {
            return FriendshipStatus.NONE
        }
    }

    fun sendFriendRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebase.sendFriendRequest(currentMemberId, targetMemberId)
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.PENDING_SENT, requestSent = true) }
            } catch (_: Exception) { }
        }
    }

    private fun Map<String, Any>.toUiPost(): CommunityPost {
        return CommunityPost(
            id = this["id"] as? String ?: "",
            communityId = this["communityId"] as? String ?: "",
            authorMemberId = this["authorMemberId"] as? String ?: "",
            author = this["author"] as? String ?: "Unknown",
            authorInitials = this["authorInitials"] as? String ?: "?",
            timeAgo = this["timeAgoLabel"] as? String ?: "",
            title = this["title"] as? String ?: "",
            body = this["body"] as? String ?: "",
            upvotes = (this["upvotes"] as? Number)?.toInt() ?: 0,
            isUpvoted = this["isUpvoted"] as? Boolean ?: false,
            isDownvoted = this["isDownvoted"] as? Boolean ?: false,
            tag = this["tag"] as? String ?: "General",
            attachmentUri = (this["attachmentUri"] as? String)?.takeIf { it.isNotBlank() },
            attachmentName = (this["attachmentName"] as? String)?.takeIf { it.isNotBlank() }
        )
    }
}

class UserProfileViewModelFactory(
    private val firebase: FirebaseSocialService,
    private val currentMemberId: String,
    private val targetMemberId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserProfileViewModel(firebase, currentMemberId, targetMemberId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
