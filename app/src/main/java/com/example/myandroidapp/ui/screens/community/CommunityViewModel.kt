package com.example.myandroidapp.ui.screens.community

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.firebase.FirebaseSocialService
import com.example.myandroidapp.data.model.*
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.util.ScannedFile
import com.example.myandroidapp.util.StudyBuddyFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════
// ── UI Data Models ──
// ═══════════════════════════════════════════════════════

data class CommunityComment(
    val id: String = "",
    val author: String,
    val body: String,
    val timeAgo: String = "just now",
    val upvotes: Int = 0,
    val isUpvoted: Boolean = false
)

data class CommunityPost(
    val id: String = "",
    val communityId: String = "",
    val authorMemberId: String = "",
    val author: String,
    val authorInitials: String,
    val timeAgo: String,
    val title: String,
    val body: String,
    val upvotes: Int,
    val comments: List<CommunityComment> = emptyList(),
    val isUpvoted: Boolean = false,
    val isDownvoted: Boolean = false,
    val tag: String = "General",
    val attachment: ScannedFile? = null,
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val isSaved: Boolean = false
)

data class CommunityInfo(
    val entity: CommunityEntity,
    val memberRole: CommunityRole? = null,
    val membershipStatus: MembershipStatus? = null
)

enum class SortMode(val label: String) {
    HOT("🔥 Hot"), NEW("🆕 New"), TOP("⬆️ Top")
}

val COMMUNITY_TAGS = listOf(
    "All", "General", "Question", "Study Tips", "Notes", "Resources",
    "Exam Prep", "Motivation", "Discussion", "Help", "Achievement"
)

data class CommunityUiState(
    val posts: List<CommunityPost> = emptyList(),
    val studyFiles: List<ScannedFile> = emptyList(),
    val sortMode: SortMode = SortMode.HOT,
    val selectedTag: String = "All",
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val expandedPostId: String? = null,
    val studentName: String = "You",
    val currentMemberId: String = "",
    val communities: List<CommunityInfo> = emptyList(),
    val selectedCommunityId: String? = null,
    val pendingRequests: List<CommunityMemberEntity> = emptyList(),
    val currentTab: CommunityTab = CommunityTab.HUB,
    val friends: List<UserProfileEntity> = emptyList(),
    val incomingFriendRequests: List<Pair<FriendRequestEntity, UserProfileEntity?>> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    val chatTarget: UserProfileEntity? = null,
    val unreadCount: Int = 0,
    val isLoading: Boolean = true
)

enum class CommunityTab { HUB, FEED, COMMUNITIES, FRIENDS }

// ═══════════════════════════════════════════════════════
// ── ViewModel (Firebase-backed) ──
// ═══════════════════════════════════════════════════════

class CommunityViewModel(
    private val firebase: FirebaseSocialService,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    private val prefs = UserPreferences(context)

    private var allPosts: List<CommunityPost> = emptyList()
    private var allComments: Map<String, List<CommunityComment>> = emptyMap()

    init {
        // Authenticate and load profile
        viewModelScope.launch {
            try {
                val name = prefs.studentName.first()
                val profile = firebase.getOrCreateProfile(name.ifBlank { "Student" })
                _uiState.update { it.copy(
                    studentName = name.ifBlank { "Student" },
                    currentMemberId = profile.memberId,
                    isLoading = false
                )}

                // Start real-time listeners after auth
                startListeners()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        // Listen for name changes
        viewModelScope.launch {
            prefs.studentName.collect { name ->
                _uiState.update { it.copy(studentName = name) }
            }
        }

        refreshStudyFiles()
    }

    private fun startListeners() {
        // Watch posts (real-time from Firestore)
        viewModelScope.launch {
            firebase.observePosts().collect { rawPosts ->
                allPosts = rawPosts.map { it.toUiPost() }
                if (allPosts.isEmpty()) {
                    seedInitialData()
                }
                applyFilters()
            }
        }

        // Watch communities
        viewModelScope.launch {
            firebase.observeCommunities().collect { communities ->
                val memberId = _uiState.value.currentMemberId
                val infos = communities.map { community ->
                    val membership = firebase.getMembership(community.communityId, memberId)
                    CommunityInfo(
                        entity = community,
                        memberRole = membership?.role,
                        membershipStatus = membership?.status
                    )
                }
                _uiState.update { it.copy(communities = infos) }
            }
        }

        // Watch friends
        viewModelScope.launch {
            val memberId = _uiState.value.currentMemberId
            if (memberId.isBlank()) return@launch
            firebase.observeAcceptedFriends(memberId).collect { friendReqs ->
                val profiles = friendReqs.mapNotNull { req ->
                    val friendId = if (req.fromMemberId == memberId) req.toMemberId else req.fromMemberId
                    firebase.getProfile(friendId)
                }
                _uiState.update { it.copy(friends = profiles) }
            }
        }

        // Watch incoming friend requests
        viewModelScope.launch {
            val memberId = _uiState.value.currentMemberId
            if (memberId.isBlank()) return@launch
            firebase.observeIncomingRequests(memberId).collect { requests ->
                val withProfiles = requests.map { req ->
                    req to firebase.getProfile(req.fromMemberId)
                }
                _uiState.update { it.copy(incomingFriendRequests = withProfiles) }
            }
        }
    }

    fun refreshStudyFiles() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { StudyBuddyFolder.scanFiles(context) }
            _uiState.update { it.copy(studyFiles = files) }
        }
    }

    fun setSortMode(mode: SortMode) { _uiState.update { it.copy(sortMode = mode) }; applyFilters() }
    fun setSelectedTag(tag: String) { _uiState.update { it.copy(selectedTag = tag) }; applyFilters() }
    fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) }; applyFilters() }
    fun toggleSearch() { _uiState.update { it.copy(isSearching = !it.isSearching, searchQuery = "") }; applyFilters() }
    fun expandPost(postId: String?) { _uiState.update { it.copy(expandedPostId = postId) } }
    fun setTab(tab: CommunityTab) { _uiState.update { it.copy(currentTab = tab) } }
    fun selectCommunity(communityId: String?) { _uiState.update { it.copy(selectedCommunityId = communityId) }; applyFilters() }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = allPosts
        if (state.selectedCommunityId != null) filtered = filtered.filter { it.communityId == state.selectedCommunityId }
        if (state.selectedTag != "All") filtered = filtered.filter { it.tag == state.selectedTag }
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            filtered = filtered.filter { it.title.lowercase().contains(q) || it.body.lowercase().contains(q) || it.author.lowercase().contains(q) }
        }
        filtered = when (state.sortMode) {
            SortMode.HOT -> filtered.sortedByDescending { it.upvotes + it.comments.size * 2 }
            SortMode.NEW -> filtered.sortedByDescending { it.id }
            SortMode.TOP -> filtered.sortedByDescending { it.upvotes }
        }
        _uiState.update { it.copy(posts = filtered) }
    }

    // ═══════ Post Actions ═══════

    fun addPost(title: String, body: String, tag: String, attachment: ScannedFile?, attachmentUri: String? = null, attachmentName: String? = null) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            var finalUri = attachmentUri
            var finalName = attachmentName

            // Upload attachment to Firebase Storage if URI provided
            if (attachmentUri != null) {
                try {
                    val uri = Uri.parse(attachmentUri)
                    val name = attachmentName ?: "attachment"
                    val downloadUrl = firebase.uploadAttachment(uri, name)
                    finalUri = downloadUrl
                } catch (_: Exception) { /* keep local URI as fallback */ }
            }

            val entity = CommunityPostEntity(
                communityId = state.selectedCommunityId ?: "",
                authorMemberId = state.currentMemberId,
                author = state.studentName,
                authorInitials = state.studentName.take(2).uppercase(),
                timeAgoLabel = "just now",
                title = title, body = body,
                tag = tag.ifBlank { "General" },
                attachmentUri = finalUri,
                attachmentName = finalName
            )
            firebase.addPost(entity)
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        val name = _uiState.value.studentName
        viewModelScope.launch(Dispatchers.IO) {
            firebase.addComment(postId, name, name.take(2).uppercase(), text)
        }
    }

    fun toggleUpvote(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val post = allPosts.firstOrNull { it.id == postId } ?: return@launch
            val newVotes = if (post.isUpvoted) post.upvotes - 1 else post.upvotes + if (post.isDownvoted) 2 else 1
            firebase.updatePost(postId, mapOf("upvotes" to newVotes, "isUpvoted" to !post.isUpvoted, "isDownvoted" to false))
        }
    }

    fun toggleDownvote(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val post = allPosts.firstOrNull { it.id == postId } ?: return@launch
            val newVotes = if (post.isDownvoted) post.upvotes + 1 else post.upvotes - if (post.isUpvoted) 2 else 1
            firebase.updatePost(postId, mapOf("upvotes" to newVotes, "isUpvoted" to false, "isDownvoted" to !post.isDownvoted))
        }
    }

    fun toggleSavePost(postId: String) {
        allPosts = allPosts.map { if (it.id == postId) it.copy(isSaved = !it.isSaved) else it }
        applyFilters()
    }



    // ═══════ Community Management ═══════

    fun createCommunity(name: String, description: String, isPublic: Boolean, emoji: String) {
        val memberId = _uiState.value.currentMemberId
        viewModelScope.launch(Dispatchers.IO) {
            val community = CommunityEntity(name = name, description = description, isPublic = isPublic, creatorId = memberId, iconEmoji = emoji.ifBlank { "📚" })
            firebase.createCommunity(community)
            firebase.insertMember(CommunityMemberEntity(communityId = community.communityId, memberId = memberId, displayName = _uiState.value.studentName, role = CommunityRole.ADMIN, status = MembershipStatus.APPROVED))
        }
    }

    fun joinCommunity(communityId: String) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val community = firebase.getCommunity(communityId) ?: return@launch
            val existing = firebase.getMembership(communityId, state.currentMemberId)
            if (existing != null) return@launch
            val status = if (community.isPublic) MembershipStatus.APPROVED else MembershipStatus.PENDING
            firebase.insertMember(CommunityMemberEntity(communityId = communityId, memberId = state.currentMemberId, displayName = state.studentName, role = CommunityRole.MEMBER, status = status))
        }
    }

    fun approveMember(communityId: String, memberId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val member = firebase.getMembership(communityId, memberId) ?: return@launch
            firebase.updateMember(member.copy(status = MembershipStatus.APPROVED))
        }
    }

    fun rejectMember(communityId: String, memberId: String) {
        viewModelScope.launch(Dispatchers.IO) { firebase.removeMember(communityId, memberId) }
    }

    fun loadPendingRequests(communityId: String) {
        viewModelScope.launch {
            firebase.observePendingMembers(communityId).collect { pending ->
                _uiState.update { it.copy(pendingRequests = pending) }
            }
        }
    }

    // ═══════ Friend Requests ═══════

    fun sendFriendRequest(toMemberId: String) {
        viewModelScope.launch(Dispatchers.IO) { firebase.sendFriendRequest(_uiState.value.currentMemberId, toMemberId) }
    }

    fun acceptFriendRequest(requestId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val docId = firebase.getDocId(requestId) ?: return@launch
            firebase.updateFriendRequestStatus(docId, "ACCEPTED")
        }
    }

    fun rejectFriendRequest(requestId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val docId = firebase.getDocId(requestId) ?: return@launch
            firebase.deleteFriendRequest(docId)
        }
    }

    // ═══════ Chat ═══════

    fun openChat(targetProfile: UserProfileEntity) {
        _uiState.update { it.copy(chatTarget = targetProfile) }
        viewModelScope.launch {
            firebase.observeConversation(_uiState.value.currentMemberId, targetProfile.memberId).collect { msgs ->
                _uiState.update { it.copy(chatMessages = msgs) }
            }
        }
    }

    fun closeChat() { _uiState.update { it.copy(chatTarget = null, chatMessages = emptyList()) } }

    fun sendMessage(text: String) {
        val target = _uiState.value.chatTarget ?: return
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            firebase.sendMessage(_uiState.value.currentMemberId, target.memberId, text)
        }
    }

    // ═══════ File Helper ═══════

    fun getFileInfoFromUri(uri: Uri): Pair<String, Long> {
        var name = "Attachment"; var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (ni >= 0) name = cursor.getString(ni) ?: "Attachment"
                    if (si >= 0) size = cursor.getLong(si)
                }
            }
        } catch (_: Exception) { }
        return name to size
    }

    // ═══════ Seeding ═══════

    private fun seedInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            val memberId = _uiState.value.currentMemberId
            val hub = CommunityEntity(name = "University Hub", description = "The main community for all students.", isPublic = true, creatorId = "system", memberCount = 1, iconEmoji = "🎓")
            firebase.createCommunity(hub)

            val cs = CommunityEntity(name = "CS Study Group", description = "Private group for CS students.", isPublic = false, creatorId = "system", memberCount = 1, iconEmoji = "💻")
            firebase.createCommunity(cs)

            val exam = CommunityEntity(name = "Exam Warriors", description = "Preparing for exams together!", isPublic = true, creatorId = "system", memberCount = 1, iconEmoji = "⚔️")
            firebase.createCommunity(exam)

            if (memberId.isNotBlank()) {
                firebase.insertMember(CommunityMemberEntity(communityId = hub.communityId, memberId = memberId, displayName = _uiState.value.studentName, role = CommunityRole.MEMBER, status = MembershipStatus.APPROVED))
            }

            // Seed posts
            listOf(
                CommunityPostEntity(communityId = hub.communityId, authorMemberId = "system", author = "StudyBot", authorInitials = "SB", timeAgoLabel = "2h ago", title = "Welcome to the University Community! 🎓", body = "Share tips, ask questions, and connect with fellow students!", tag = "General", upvotes = 42),
                CommunityPostEntity(communityId = hub.communityId, authorMemberId = "system", author = "Alex Chen", authorInitials = "AC", timeAgoLabel = "4h ago", title = "Best way to study for finals? 📚", body = "I have 3 exams next week. What study techniques work best?", tag = "Question", upvotes = 28),
                CommunityPostEntity(communityId = hub.communityId, authorMemberId = "system", author = "Maya Patel", authorInitials = "MP", timeAgoLabel = "6h ago", title = "Free Calculus Notes — Chapter 1-8 🧮", body = "Complete calculus notes covering limits, derivatives, integrals, and series.", tag = "Notes", upvotes = 156),
                CommunityPostEntity(communityId = hub.communityId, authorMemberId = "system", author = "Dr. Smith", authorInitials = "DS", timeAgoLabel = "1d ago", title = "📌 How to use Active Recall effectively", body = "Active recall is 3x more effective than re-reading! Close notes → recall → check → repeat.", tag = "Study Tips", upvotes = 213),
            ).forEach { firebase.addPost(it) }
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

class CommunityViewModelFactory(
    private val firebase: FirebaseSocialService,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommunityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommunityViewModel(firebase, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
