package com.example.myandroidapp.ui.screens.community

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.repository.CommunityRepository
import com.example.myandroidapp.data.local.PostWithComments
import com.example.myandroidapp.data.model.CommunityPostEntity
import com.example.myandroidapp.data.model.CommunityCommentEntity
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.util.ScannedFile
import com.example.myandroidapp.util.StudyBuddyFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════
// ── Data Models ──
// ═══════════════════════════════════════════════════════

data class CommunityComment(
    val id: Long = 0,
    val author: String,
    val body: String,
    val timeAgo: String = "just now",
    val upvotes: Int = 0,
    val isUpvoted: Boolean = false
)

data class CommunityPost(
    val id: Long,
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
    val isSaved: Boolean = false,
    val isAwarded: Boolean = false,
    val awardCount: Int = 0
)

enum class SortMode(val label: String) {
    HOT("🔥 Hot"),
    NEW("🆕 New"),
    TOP("⬆️ Top")
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
    val expandedPostId: Long? = null,
    val studentName: String = "You"
)

// ═══════════════════════════════════════════════════════
// ── ViewModel ──
// ═══════════════════════════════════════════════════════

class CommunityViewModel(
    private val repository: CommunityRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    private val prefs = UserPreferences(context)

    private var allPosts: List<CommunityPost> = emptyList()

    init {
        viewModelScope.launch {
            prefs.studentName.collect { name ->
                _uiState.update { it.copy(studentName = name) }
            }
        }
        viewModelScope.launch {
            repository.observePosts()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
                .collect { posts ->
                    allPosts = posts.map { it.toUi() }
                    if (allPosts.isEmpty()) {
                        seedInitialPosts()
                    } else {
                        applyFilters()
                    }
                }
        }
        refreshStudyFiles()
    }

    fun refreshStudyFiles() {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { StudyBuddyFolder.scanFiles(context) }
            _uiState.update { it.copy(studyFiles = files) }
        }
    }

    fun setSortMode(mode: SortMode) {
        _uiState.update { it.copy(sortMode = mode) }
        applyFilters()
    }

    fun setSelectedTag(tag: String) {
        _uiState.update { it.copy(selectedTag = tag) }
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun toggleSearch() {
        _uiState.update { it.copy(isSearching = !it.isSearching, searchQuery = "") }
        applyFilters()
    }

    fun expandPost(postId: Long?) {
        _uiState.update { it.copy(expandedPostId = postId) }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = allPosts

        // Tag filter
        if (state.selectedTag != "All") {
            filtered = filtered.filter { it.tag == state.selectedTag }
        }

        // Search filter
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                it.body.lowercase().contains(q) ||
                it.author.lowercase().contains(q) ||
                it.tag.lowercase().contains(q)
            }
        }

        // Sort
        filtered = when (state.sortMode) {
            SortMode.HOT -> filtered.sortedByDescending { it.upvotes + it.comments.size * 2 }
            SortMode.NEW -> filtered.sortedByDescending { it.id }
            SortMode.TOP -> filtered.sortedByDescending { it.upvotes }
        }

        _uiState.update { it.copy(posts = filtered) }
    }

    fun addPost(title: String, body: String, tag: String, attachment: ScannedFile?) {
        val name = _uiState.value.studentName
        val initials = name.take(2).uppercase()
        viewModelScope.launch(Dispatchers.IO) {
            val entity = CommunityPostEntity(
                author = name,
                authorInitials = initials,
                timeAgoLabel = "just now",
                title = title,
                body = body,
                tag = tag.ifBlank { "General" },
                upvotes = 0,
                attachmentPath = attachment?.absolutePath,
                attachmentSize = attachment?.size,
                attachmentSubfolder = attachment?.subfolder
            )
            repository.addPost(entity)
        }
    }

    fun addComment(postId: Long, text: String) {
        if (text.isBlank()) return
        val name = _uiState.value.studentName
        viewModelScope.launch(Dispatchers.IO) {
            repository.addComment(
                CommunityCommentEntity(
                    postId = postId,
                    author = name,
                    authorInitials = name.take(2).uppercase(),
                    body = text
                )
            )
        }
    }

    fun toggleUpvote(postId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = allPosts.firstOrNull { it.id == postId } ?: return@launch
            val newUpvotes = if (current.isUpvoted) current.upvotes - 1 else current.upvotes + if (current.isDownvoted) 2 else 1
            repository.updatePost(
                CommunityPostEntity(
                    id = postId,
                    author = current.author,
                    authorInitials = current.authorInitials,
                    timeAgoLabel = current.timeAgo,
                    title = current.title,
                    body = current.body,
                    tag = current.tag,
                    upvotes = newUpvotes,
                    isUpvoted = !current.isUpvoted,
                    isDownvoted = false,
                    attachmentPath = current.attachment?.absolutePath,
                    attachmentSize = current.attachment?.size,
                    attachmentSubfolder = current.attachment?.subfolder
                )
            )
        }
    }

    fun toggleDownvote(postId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = allPosts.firstOrNull { it.id == postId } ?: return@launch
            val newUpvotes = if (current.isDownvoted) current.upvotes + 1 else current.upvotes - if (current.isUpvoted) 2 else 1
            repository.updatePost(
                CommunityPostEntity(
                    id = postId,
                    author = current.author,
                    authorInitials = current.authorInitials,
                    timeAgoLabel = current.timeAgo,
                    title = current.title,
                    body = current.body,
                    tag = current.tag,
                    upvotes = newUpvotes,
                    isUpvoted = false,
                    isDownvoted = !current.isDownvoted,
                    attachmentPath = current.attachment?.absolutePath,
                    attachmentSize = current.attachment?.size,
                    attachmentSubfolder = current.attachment?.subfolder
                )
            )
        }
    }

    fun toggleSavePost(postId: Long) {
        // Toggle saved status in UI (persisted locally in state)
        allPosts = allPosts.map {
            if (it.id == postId) it.copy(isSaved = !it.isSaved) else it
        }
        applyFilters()
    }

    fun toggleAwardPost(postId: Long) {
        allPosts = allPosts.map {
            if (it.id == postId) it.copy(
                isAwarded = !it.isAwarded,
                awardCount = if (it.isAwarded) it.awardCount - 1 else it.awardCount + 1
            ) else it
        }
        applyFilters()
    }

    private fun seedInitialPosts() {
        viewModelScope.launch(Dispatchers.IO) {
            val seeds = listOf(
                CommunityPostEntity(
                    author = "StudyBot",
                    authorInitials = "SB",
                    timeAgoLabel = "2h ago",
                    title = "Welcome to the University Community! 🎓",
                    body = "This is your space to share study tips, ask questions, post resources, and connect with fellow students. Create a post using the + button below!\n\n✨ Tips:\n• Use tags to categorize your posts\n• Upvote helpful content\n• Attach files from your StudyBuddy folder\n• Comment and engage with others",
                    tag = "General",
                    upvotes = 42
                ),
                CommunityPostEntity(
                    author = "Alex Chen",
                    authorInitials = "AC",
                    timeAgoLabel = "4h ago",
                    title = "Best way to study for finals? 📚",
                    body = "I have 3 exams next week and feeling overwhelmed. What study techniques work best for you? I've been using the Pomodoro technique with 25-min focus sessions but wondering if there's something better.\n\nMy subjects: Linear Algebra, Organic Chemistry, Data Structures",
                    tag = "Question",
                    upvotes = 28
                ),
                CommunityPostEntity(
                    author = "Maya Patel",
                    authorInitials = "MP",
                    timeAgoLabel = "6h ago",
                    title = "Free Calculus Notes — Chapter 1-8 🧮",
                    body = "Hey everyone! I've compiled my complete calculus notes from this semester. Covers:\n• Limits & Continuity\n• Derivatives & Integrals\n• Series & Sequences\n• Multivariable intro\n\nFeel free to use and share! Drop a comment if you find any errors.",
                    tag = "Notes",
                    upvotes = 156
                ),
                CommunityPostEntity(
                    author = "Jordan Kim",
                    authorInitials = "JK",
                    timeAgoLabel = "8h ago",
                    title = "Just finished a 4-hour deep focus session! 🔥",
                    body = "Used the app's focus timer with ambient rain sounds and the app lock feature. Zero distractions! My productivity was insane. Managed to finish my entire research paper outline.\n\nHere's what worked:\n1. Turned on DND mode\n2. Used 50-min focus / 10-min break cycles\n3. Kept water and snacks ready beforehand\n4. Set clear goals before starting",
                    tag = "Achievement",
                    upvotes = 89
                ),
                CommunityPostEntity(
                    author = "Sarah Lee",
                    authorInitials = "SL",
                    timeAgoLabel = "12h ago",
                    title = "Study Group for Computer Science 101?",
                    body = "Looking for people to form a study group for CS101. We can meet virtually or in the library. Topics I need help with:\n• Recursion\n• Binary trees\n• Graph algorithms\n\nDM me or comment if interested! Planning to meet Tuesdays and Thursdays.",
                    tag = "Discussion",
                    upvotes = 34
                ),
                CommunityPostEntity(
                    author = "Dr. Smith",
                    authorInitials = "DS",
                    timeAgoLabel = "1d ago",
                    title = "📌 How to use Active Recall effectively",
                    body = "Active recall is one of the most evidence-backed study techniques:\n\n1. Close your notes\n2. Try to recall everything from memory\n3. Check what you missed\n4. Repeat\n\nStudies show this is 3x more effective than re-reading! Combine with spaced repetition for maximum retention.\n\nPro tip: Use flashcards or practice problems instead of just highlighting.",
                    tag = "Study Tips",
                    upvotes = 213
                ),
                CommunityPostEntity(
                    author = "Mike Rodriguez",
                    authorInitials = "MR",
                    timeAgoLabel = "1d ago",
                    title = "Exam prep checklist I wish I had freshman year",
                    body = "Sharing my exam prep system that's never failed me:\n\n□ Review syllabus & identify key topics (3 weeks before)\n□ Organize notes by topic (2 weeks before)\n□ Create summary sheets (1 week before)\n□ Practice problems daily (ongoing)\n□ Join/form study group (2 weeks before)\n□ Past exams if available (1 week before)\n□ Self-test (3 days before)\n□ Rest & light review (day before)\n\nGood luck everyone! 💪",
                    tag = "Exam Prep",
                    upvotes = 167
                ),
                CommunityPostEntity(
                    author = "Emma Wilson",
                    authorInitials = "EW",
                    timeAgoLabel = "2d ago",
                    title = "Feeling burnt out... any advice? 😞",
                    body = "I've been studying 10+ hours a day for the past two weeks and I'm completely exhausted. Can barely focus for 20 minutes now. \n\nHow do you all deal with burnout while keeping up with coursework? I feel guilty whenever I take breaks.",
                    tag = "Help",
                    upvotes = 76
                )
            )

            seeds.forEach { repository.addPost(it) }

            // Add some seed comments
            val postIds = repository.observePosts().first().map { it.post.id }
            if (postIds.size >= 3) {
                repository.addComment(CommunityCommentEntity(postId = postIds[1], author = "Nina Park", authorInitials = "NP", body = "Spaced repetition + active recall is the combo! Check out Anki for flashcards."))
                repository.addComment(CommunityCommentEntity(postId = postIds[1], author = "Tom Hayes", authorInitials = "TH", body = "For Organic Chem I recommend drawing reaction mechanisms by hand. Muscle memory helps a lot!"))
                repository.addComment(CommunityCommentEntity(postId = postIds[2], author = "Lisa Chang", authorInitials = "LC", body = "These notes are amazing, thank you so much! 🙏"))
                repository.addComment(CommunityCommentEntity(postId = postIds[2], author = "Ryan Scott", authorInitials = "RS", body = "Found a small typo in Chapter 5 — the integral boundary should be from 0 to π, not 0 to 2π."))
                repository.addComment(CommunityCommentEntity(postId = postIds[3], author = "Jamie Wu", authorInitials = "JW", body = "4 hours straight is impressive! I can barely do 2. The app lock feature is a game changer."))
                repository.addComment(CommunityCommentEntity(postId = postIds[7], author = "StudyBot", authorInitials = "SB", body = "Remember that taking breaks actually IMPROVES productivity! Try the 52-17 rule: 52 minutes of work, 17 minutes of rest. You deserve breaks! ❤️"))
                repository.addComment(CommunityCommentEntity(postId = postIds[7], author = "Alex Chen", authorInitials = "AC", body = "I went through the same thing last semester. What helped: 1) Set a hard stop time each day 2) Exercise 3) Sleep 8 hours minimum. Your brain needs rest to form memories!"))
            }
        }
    }

    private fun PostWithComments.toUi(): CommunityPost {
        val attachment = post.attachmentPath?.let {
            ScannedFile(
                name = it.substringAfterLast('/'),
                absolutePath = it,
                size = post.attachmentSize ?: 0L,
                lastModified = post.createdAt,
                type = StudyBuddyFolder.classifyFile(it),
                subfolder = post.attachmentSubfolder ?: ""
            )
        }
        return CommunityPost(
            id = post.id,
            author = post.author,
            authorInitials = post.authorInitials,
            timeAgo = post.timeAgoLabel,
            title = post.title,
            body = post.body,
            upvotes = post.upvotes,
            comments = comments.map { CommunityComment(id = it.id, author = it.author, body = it.body, timeAgo = it.timeAgoLabel) },
            isUpvoted = post.isUpvoted,
            isDownvoted = post.isDownvoted,
            tag = post.tag,
            attachment = attachment
        )
    }
}

class CommunityViewModelFactory(
    private val repository: CommunityRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommunityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommunityViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
