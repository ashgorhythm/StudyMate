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
    val error: String? = null,
    val friendRequestDocId: String? = null  // Firestore doc ID for accept/reject operations
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

                // Check friendship status (with full outgoing detection - Phase 4 fix)
                val friendshipStatus: FriendshipStatus
                var docId: String? = null

                if (isCurrentUser) {
                    friendshipStatus = FriendshipStatus.FRIENDS // self
                } else {
                    val result = withContext(Dispatchers.IO) {
                        checkFriendshipStatus()
                    }
                    friendshipStatus = result.first
                    docId = result.second
                }

                _uiState.update {
                    it.copy(
                        profile = profile,
                        posts = allPosts.sortedByDescending { p -> p.id },
                        postCount = allPosts.size,
                        friendshipStatus = friendshipStatus,
                        friendRequestDocId = docId,
                        isCurrentUser = isCurrentUser,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    /**
     * Phase 4 fix: Now properly checks BOTH incoming AND outgoing requests.
     * Returns (FriendshipStatus, Firestore docId for the request if applicable)
     */
    private suspend fun checkFriendshipStatus(): Pair<FriendshipStatus, String?> {
        try {
            // Use direct lookup between the two users (any direction, any status)
            val (request, docId) = firebase.getFriendRequestBetween(currentMemberId, targetMemberId)

            if (request != null && docId != null) {
                return when {
                    // ACCEPTED in either direction = FRIENDS
                    request.status == com.example.myandroidapp.data.model.FriendRequestStatus.ACCEPTED ->
                        FriendshipStatus.FRIENDS to docId
                    // PENDING: check who sent it
                    request.status == com.example.myandroidapp.data.model.FriendRequestStatus.PENDING &&
                    request.fromMemberId == currentMemberId ->
                        FriendshipStatus.PENDING_SENT to docId
                    request.status == com.example.myandroidapp.data.model.FriendRequestStatus.PENDING &&
                    request.toMemberId == currentMemberId ->
                        FriendshipStatus.PENDING_RECEIVED to docId
                    else -> FriendshipStatus.NONE to null
                }
            }

            return FriendshipStatus.NONE to null
        } catch (_: Exception) {
            return FriendshipStatus.NONE to null
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

    /**
     * Phase 4 fix: Accept an incoming friend request
     */
    fun acceptFriendRequest() {
        val docId = _uiState.value.friendRequestDocId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebase.acceptFriendRequestByDocId(docId)
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.FRIENDS) }
            } catch (_: Exception) { }
        }
    }

    /**
     * Phase 4 fix: Reject an incoming friend request
     */
    fun rejectFriendRequest() {
        val docId = _uiState.value.friendRequestDocId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebase.deleteFriendRequest(docId)
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.NONE, friendRequestDocId = null) }
            } catch (_: Exception) { }
        }
    }

    /**
     * Phase 4: Unfriend — removes the accepted friend request
     */
    fun unfriend() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebase.unfriend(currentMemberId, targetMemberId)
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.NONE, friendRequestDocId = null) }
            } catch (_: Exception) { }
        }
    }

    private fun Map<String, Any>.toUiPost(): CommunityPost {
        return CommunityPost(
            id = this["id"] as? String ?: "",
            communityId = this["communityId"] as? String ?: "",
            authorMemberId = this["authorMemberId"] as? String ?: "",
            author = this["author"] as? String ?: "Unknown",
            authorUsername = this["authorUsername"] as? String ?: "",
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

