package com.example.myandroidapp.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import com.example.myandroidapp.util.ScannedFile
import java.util.Locale

// ═══════════════════════════════════════════════════════
// ── Main Community Screen (Reddit-like) ──
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(viewModel: CommunityViewModel) {
    val adaptive = rememberAdaptiveInfo()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var commentTarget by remember { mutableStateOf<CommunityPost?>(null) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (uiState.expandedPostId == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = TealPrimary,
                    contentColor = NavyDark,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Edit, "Create Post") },
                    text = { Text("Post", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
        ) {
            // Expanded post detail view
            val expandedPost = uiState.expandedPostId?.let { id ->
                uiState.posts.firstOrNull { it.id == id }
            }

            AnimatedContent(
                targetState = expandedPost != null,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                },
                label = "postDetail"
            ) { showDetail ->
                if (showDetail && expandedPost != null) {
                    PostDetailView(
                        post = expandedPost,
                        onBack = { viewModel.expandPost(null) },
                        onUpvote = { viewModel.toggleUpvote(expandedPost.id) },
                        onDownvote = { viewModel.toggleDownvote(expandedPost.id) },
                        onSave = { viewModel.toggleSavePost(expandedPost.id) },
                        onAward = { viewModel.toggleAwardPost(expandedPost.id) },
                        onComment = { viewModel.addComment(expandedPost.id, it) },
                        adaptive = adaptive
                    )
                } else {
                    // Main feed
                    Column(
                        modifier = Modifier
                            .then(
                                if (adaptive.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified)
                                    Modifier.widthIn(max = adaptive.maxContentWidth)
                                else Modifier
                            )
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                    ) {
                        // ── Header ──
                        CommunityHeader(
                            isSearching = uiState.isSearching,
                            searchQuery = uiState.searchQuery,
                            onSearchToggle = { viewModel.toggleSearch() },
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            isTablet = adaptive.isTablet,
                            horizontalPadding = adaptive.horizontalPadding
                        )

                        // ── Sort Tabs ──
                        SortTabs(
                            currentSort = uiState.sortMode,
                            onSortChanged = { viewModel.setSortMode(it) },
                            horizontalPadding = adaptive.horizontalPadding
                        )

                        // ── Tag Filter Chips ──
                        TagFilterRow(
                            selectedTag = uiState.selectedTag,
                            onTagSelected = { viewModel.setSelectedTag(it) }
                        )

                        Spacer(Modifier.height(4.dp))

                        // ── Posts Feed ──
                        if (uiState.posts.isEmpty()) {
                            EmptyFeedState(
                                isFiltered = uiState.selectedTag != "All" || uiState.searchQuery.isNotBlank(),
                                onClearFilter = {
                                    viewModel.setSelectedTag("All")
                                    viewModel.setSearchQuery("")
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = adaptive.horizontalPadding,
                                    end = adaptive.horizontalPadding,
                                    bottom = 96.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(uiState.posts, key = { _, p -> p.id }) { _, post ->
                                    PostCard(
                                        post = post,
                                        onUpvote = { viewModel.toggleUpvote(post.id) },
                                        onDownvote = { viewModel.toggleDownvote(post.id) },
                                        onComment = { viewModel.expandPost(post.id) },
                                        onSave = { viewModel.toggleSavePost(post.id) },
                                        onAward = { viewModel.toggleAwardPost(post.id) },
                                        onExpand = { viewModel.expandPost(post.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dialogs
            if (showCreateDialog) {
                NewPostDialog(
                    files = uiState.studyFiles,
                    authorName = uiState.studentName,
                    onDismiss = { showCreateDialog = false },
                    onSubmit = { title, body, tag, file ->
                        viewModel.addPost(title, body, tag, file)
                        showCreateDialog = false
                    }
                )
            }

            commentTarget?.let { post ->
                CommentSheet(
                    post = post,
                    onDismiss = { commentTarget = null },
                    onAddComment = { text ->
                        viewModel.addComment(post.id, text)
                        commentTarget = null
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Header ──
// ═══════════════════════════════════════════════════════

@Composable
private fun CommunityHeader(
    isSearching: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    isTablet: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = if (isTablet) 24.dp else 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = !isSearching, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Community",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "University Study Hub",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(visible = isSearching, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
            val focusManager = LocalFocusManager.current
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search posts...", color = TextMuted, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = 300.dp),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = TextMuted.copy(0.2f),
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    cursorColor = TealPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp)) }
            )
        }

        IconButton(onClick = onSearchToggle) {
            Icon(
                if (isSearching) Icons.Default.Close else Icons.Default.Search,
                "Search",
                tint = TextPrimary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Sort Tabs ──
// ═══════════════════════════════════════════════════════

@Composable
private fun SortTabs(
    currentSort: SortMode,
    onSortChanged: (SortMode) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SortMode.entries.forEach { mode ->
            val selected = currentSort == mode
            FilterChip(
                selected = selected,
                onClick = { onSortChanged(mode) },
                label = { Text(mode.label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealPrimary.copy(0.15f),
                    selectedLabelColor = TealPrimary,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = TealPrimary.copy(0.4f),
                    borderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Tag Filter Row ──
// ═══════════════════════════════════════════════════════

@Composable
private fun TagFilterRow(selectedTag: String, onTagSelected: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(COMMUNITY_TAGS) { tag ->
            val selected = selectedTag == tag
            val tagColor = getTagColor(tag)
            FilterChip(
                selected = selected,
                onClick = { onTagSelected(tag) },
                label = { Text(tag, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = tagColor.copy(0.15f),
                    selectedLabelColor = tagColor,
                    containerColor = Color.Transparent,
                    labelColor = TextMuted
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = tagColor.copy(0.4f),
                    borderColor = TextMuted.copy(0.15f)
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Post Card (Reddit-style) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun PostCard(
    post: CommunityPost,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onAward: () -> Unit,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(0.08f))
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── Header: Avatar + Author + Time + Tag ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    getAuthorColor(post.author),
                                    getAuthorColor(post.author).copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        post.authorInitials,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            post.author,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        if (post.awardCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Text("🏆 ${ post.awardCount }", fontSize = 11.sp, color = AmberAccent)
                        }
                    }
                    Text(post.timeAgo, fontSize = 11.sp, color = TextMuted)
                }
                // Tag badge
                val tagColor = getTagColor(post.tag)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagColor.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        post.tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Title ──
            Text(
                post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ── Body preview ──
            if (post.body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    post.body,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Attachment ──
            post.attachment?.let { file ->
                Spacer(Modifier.height(10.dp))
                AttachmentRow(file)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = TextMuted.copy(0.08f))
            Spacer(Modifier.height(6.dp))

            // ── Action Row ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vote buttons
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NavyLight.copy(0.4f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onUpvote, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (post.isUpvoted) Icons.Filled.ArrowUpward else Icons.Outlined.ArrowUpward,
                            "Upvote",
                            tint = if (post.isUpvoted) TealPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        formatVotes(post.upvotes),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            post.isUpvoted -> TealPrimary
                            post.isDownvoted -> RedError
                            else -> TextPrimary
                        }
                    )
                    IconButton(onClick = onDownvote, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (post.isDownvoted) Icons.Filled.ArrowDownward else Icons.Outlined.ArrowDownward,
                            "Downvote",
                            tint = if (post.isDownvoted) RedError else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Comments
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NavyLight.copy(0.4f))
                        .clickable { onComment() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Comment,
                        "Comments",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        post.comments.size.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.weight(1f))

                // Award
                IconButton(onClick = onAward, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (post.isAwarded) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                        "Award",
                        tint = if (post.isAwarded) AmberAccent else TextMuted.copy(0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Save/Bookmark
                IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        "Save",
                        tint = if (post.isSaved) PurpleAccent else TextMuted.copy(0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Share
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Share,
                        "Share",
                        tint = TextMuted.copy(0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Post Detail View ──
// ═══════════════════════════════════════════════════════

@Composable
private fun PostDetailView(
    post: CommunityPost,
    onBack: () -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onSave: () -> Unit,
    onAward: () -> Unit,
    onComment: (String) -> Unit,
    adaptive: com.example.myandroidapp.ui.util.AdaptiveInfo
) {
    var commentText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .then(
                if (adaptive.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified)
                    Modifier.widthIn(max = adaptive.maxContentWidth)
                else Modifier
            )
            .fillMaxSize()
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = adaptive.horizontalPadding)
                .padding(top = if (adaptive.isTablet) 24.dp else 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                post.tag,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = getTagColor(post.tag)
            )
            Spacer(Modifier.weight(1f))
            Text("${post.comments.size} comments", fontSize = 13.sp, color = TextMuted)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = adaptive.horizontalPadding, end = adaptive.horizontalPadding, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Full post content
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, TealPrimary.copy(0.1f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        // Author row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(getAuthorColor(post.author), getAuthorColor(post.author).copy(0.6f)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(post.authorInitials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(post.author, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (post.awardCount > 0) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("🏆 ${post.awardCount}", fontSize = 12.sp, color = AmberAccent)
                                    }
                                }
                                Text(post.timeAgo, fontSize = 12.sp, color = TextMuted)
                            }
                            val tagColor = getTagColor(post.tag)
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(tagColor.copy(0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(post.tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tagColor)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(post.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 26.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(post.body, fontSize = 14.sp, color = TextSecondary, lineHeight = 22.sp)

                        post.attachment?.let { file ->
                            Spacer(Modifier.height(14.dp))
                            AttachmentRow(file)
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = TextMuted.copy(0.1f))
                        Spacer(Modifier.height(10.dp))

                        // Actions
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                Modifier.clip(RoundedCornerShape(20.dp)).background(NavyLight.copy(0.4f)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onUpvote, modifier = Modifier.size(36.dp)) {
                                    Icon(if (post.isUpvoted) Icons.Filled.ArrowUpward else Icons.Outlined.ArrowUpward, "Upvote", tint = if (post.isUpvoted) TealPrimary else TextMuted, modifier = Modifier.size(20.dp))
                                }
                                Text(formatVotes(post.upvotes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (post.isUpvoted) TealPrimary else if (post.isDownvoted) RedError else TextPrimary)
                                IconButton(onClick = onDownvote, modifier = Modifier.size(36.dp)) {
                                    Icon(if (post.isDownvoted) Icons.Filled.ArrowDownward else Icons.Outlined.ArrowDownward, "Downvote", tint = if (post.isDownvoted) RedError else TextMuted, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onAward, modifier = Modifier.size(36.dp)) {
                                Icon(if (post.isAwarded) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents, "Award", tint = if (post.isAwarded) AmberAccent else TextMuted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                                Icon(if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, "Save", tint = if (post.isSaved) PurpleAccent else TextMuted, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Outlined.Share, "Share", tint = TextMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Comments header
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Comment, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Comments (${post.comments.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }

            // Comments list
            if (post.comments.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(12.dp),
                        CardDefaults.cardColors(SurfaceCard)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💬", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("No comments yet", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                            Text("Be the first to comment!", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            } else {
                itemsIndexed(post.comments) { idx, comment ->
                    CommentItem(comment = comment, isFirst = idx == 0)
                }
            }
        }

        // Comment input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(NavyMedium),
            border = BorderStroke(1.dp, TealPrimary.copy(0.1f))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = adaptive.horizontalPadding, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = TextMuted.copy(0.15f),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = TealPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onComment(commentText.trim())
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (commentText.isNotBlank()) TealPrimary else TealPrimary.copy(0.2f))
                ) {
                    Icon(
                        Icons.Default.Send,
                        "Send",
                        tint = if (commentText.isNotBlank()) NavyDark else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Comment Item ──
// ═══════════════════════════════════════════════════════

@Composable
private fun CommentItem(comment: CommunityComment, isFirst: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp)
    ) {
        // Thread line indicator
        Box(
            Modifier
                .width(3.dp)
                .height(IntrinsicSize.Max)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isFirst) TealPrimary.copy(0.4f) else PurpleAccent.copy(0.3f)
                )
        )
        Spacer(Modifier.width(12.dp))

        // Avatar
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(getAuthorColor(comment.author), getAuthorColor(comment.author).copy(0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                comment.author.take(1).uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.author,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    comment.timeAgo,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                comment.body,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Attachment Row ──
// ═══════════════════════════════════════════════════════

@Composable
private fun AttachmentRow(file: ScannedFile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(TealPrimary.copy(0.06f)),
        border = BorderStroke(1.dp, TealPrimary.copy(0.15f))
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AttachFile, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontSize = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(file.subfolder.ifBlank { "StudyBuddy" }, fontSize = 10.sp, color = TextMuted)
            }
            Text(formatFileSize(file.size), fontSize = 10.sp, color = TextMuted)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Empty State ──
// ═══════════════════════════════════════════════════════

@Composable
private fun EmptyFeedState(isFiltered: Boolean, onClearFilter: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (isFiltered) "No posts match your filter" else "No posts yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            if (isFiltered) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onClearFilter) {
                    Text("Clear filters", color = TealPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── New Post Dialog ──
// ═══════════════════════════════════════════════════════

@Composable
private fun NewPostDialog(
    files: List<ScannedFile>,
    authorName: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, ScannedFile?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("General") }
    var selectedFile: ScannedFile? by remember { mutableStateOf(null) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSubmit(title.trim(), body.trim(), selectedTag, selectedFile) },
                enabled = title.isNotBlank() && body.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = NavyDark)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Post", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(authorName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Create Post", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Posting as $authorName", color = TextMuted, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Tag selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tag:", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.width(8.dp))
                    val tagColor = getTagColor(selectedTag)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tagColor.copy(0.12f))
                            .clickable { showTagPicker = !showTagPicker }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedTag, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tagColor)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, null, tint = tagColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                AnimatedVisibility(visible = showTagPicker) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(COMMUNITY_TAGS.filter { it != "All" }) { tag ->
                            val tc = getTagColor(tag)
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { selectedTag = tag; showTagPicker = false },
                                label = { Text(tag, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tc.copy(0.2f),
                                    selectedLabelColor = tc,
                                    containerColor = Color.Transparent,
                                    labelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Post title", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.2f),
                        cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                // Body field
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text("Share your question, tip, or resource...", color = TextMuted) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.2f),
                        cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                // Attach file
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showFilePicker = !showFilePicker }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AttachFile, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedFile?.name ?: "Attach from StudyBuddy (optional)",
                        fontSize = 13.sp,
                        color = if (selectedFile != null) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selectedFile != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { selectedFile = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                AnimatedVisibility(visible = showFilePicker) {
                    Column {
                        if (files.isEmpty()) {
                            Text("No files in StudyBuddy folder", fontSize = 12.sp, color = TextMuted)
                        } else {
                            files.take(5).forEach { file ->
                                val selected = selectedFile?.absolutePath == file.absolutePath
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) TealPrimary.copy(0.08f) else Color.Transparent)
                                        .clickable {
                                            selectedFile = if (selected) null else file
                                            showFilePicker = false
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (selected) Icons.Default.CheckCircle else Icons.Outlined.InsertDriveFile,
                                        null,
                                        tint = if (selected) TealPrimary else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(file.name, fontSize = 12.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(formatFileSize(file.size), fontSize = 10.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = NavyMedium,
        shape = RoundedCornerShape(20.dp)
    )
}

// ═══════════════════════════════════════════════════════
// ── Comment Sheet (for quick access from feed) ──
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentSheet(
    post: CommunityPost,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Comments",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = TextMuted)
                }
            }
            Text(
                post.title,
                fontSize = 14.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            post.comments.forEach { c ->
                CommentItem(c)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Add a comment...", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = TextMuted.copy(0.15f),
                        cursorColor = TealPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { if (comment.isNotBlank()) onAddComment(comment.trim()) },
                    enabled = comment.isNotBlank(),
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(if (comment.isNotBlank()) TealPrimary else TealPrimary.copy(0.2f))
                ) {
                    Icon(Icons.Default.Send, "Send", tint = if (comment.isNotBlank()) NavyDark else TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Helpers ──
// ═══════════════════════════════════════════════════════

private fun getTagColor(tag: String): Color {
    return when (tag) {
        "All" -> TealPrimary
        "General" -> TextSecondary
        "Question" -> PurpleAccent
        "Study Tips" -> TealPrimary
        "Notes" -> GreenSuccess
        "Resources" -> AmberAccent
        "Exam Prep" -> RedError
        "Motivation" -> PinkAccent
        "Discussion" -> PurpleLight
        "Help" -> AmberAccent
        "Achievement" -> AmberAccent
        else -> TextMuted
    }
}

private fun getAuthorColor(author: String): Color {
    val hash = author.hashCode()
    val colors = listOf(TealPrimary, PurpleAccent, AmberAccent, PinkAccent, GreenSuccess, PurpleLight, TealDark)
    return colors[Math.abs(hash) % colors.size]
}

private fun formatVotes(votes: Int): String {
    return when {
        votes >= 1000 -> String.format(Locale.US, "%.1fk", votes / 1000.0)
        else -> votes.toString()
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
        bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1e6)
        bytes >= 1_000 -> String.format(Locale.US, "%.0f KB", bytes / 1e3)
        else -> "$bytes B"
    }
}
