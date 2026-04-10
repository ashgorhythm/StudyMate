package com.example.myandroidapp.ui.screens.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.util.ScannedFile
import java.util.Locale

// ═══════════════════════════════════════════════════════
// ── Main Community Screen ──
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel,
    onNavigateToProfile: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // Dialog states
    var showNewPostDialog by remember { mutableStateOf(false) }
    var commentSheetPost by remember { mutableStateOf<CommunityPost?>(null) }
    var showCreateCommunity by remember { mutableStateOf(false) }
    var manageCommunityId by remember { mutableStateOf<String?>(null) }

    // Navigation rail state
    var isRailExpanded by remember { mutableStateOf(false) }

    // Chat overlay
    val chatTarget = state.chatTarget
    if (chatTarget != null) {
        ChatScreen(
            targetProfile = chatTarget,
            messages = state.chatMessages,
            currentMemberId = state.currentMemberId,
            onSend = { viewModel.sendMessage(it) },
            onBack = { viewModel.closeChat() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDark, NavyMedium.copy(0.5f), NavyDark)))
    ) {
        // ── Group Detail Screen Overlay ──
        val selectedGroupId = state.selectedGroupId
        if (selectedGroupId != null) {
            val groupInfo = state.communities.find { it.entity.communityId == selectedGroupId }
            if (groupInfo != null) {
                CommunityGroupDetailScreen(
                    communityInfo = groupInfo,
                    posts = state.posts,
                    members = state.communityMembers,
                    currentMemberId = state.currentMemberId,
                    onBack = { viewModel.closeGroupDetail() },
                    onNewPost = { showNewPostDialog = true },
                    onExpandPost = { viewModel.expandPost(it) },
                    expandedPostId = state.expandedPostId,
                    onUpvote = { viewModel.toggleUpvote(it) },
                    onDownvote = { viewModel.toggleDownvote(it) },
                    onComment = { commentSheetPost = state.posts.find { p -> p.id == it } },
                    onSave = { viewModel.toggleSavePost(it) },
                    onProfileClick = { memberId -> onNavigateToProfile(memberId) },
                    onManageMembers = { manageCommunityId = selectedGroupId; viewModel.loadPendingRequests(selectedGroupId) }
                )
                return@Box
            }
        }

        Row(Modifier.fillMaxSize()) {
            // ── Animated Navigation Rail ──
            CommunityNavigationRail(
                currentTab = state.currentTab,
                isExpanded = isRailExpanded,
                onTabSelected = { tab ->
                    viewModel.setTab(tab)
                    // Auto-collapse after selection on small screens
                    isRailExpanded = false
                },
                onToggle = { isRailExpanded = !isRailExpanded }
            )

            // ── Main Content ──
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // ── Top Bar ──
                CommunityTopBar(
                    state = state,
                    currentTab = state.currentTab,
                    onToggleSearch = { viewModel.toggleSearch() },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleRail = { isRailExpanded = !isRailExpanded }
                )

                // ── Tab Content ──
                when (state.currentTab) {
                    CommunityTab.HUB -> CommunityHubScreen(
                        state = state,
                        onNavigateToFeed = { viewModel.setTab(CommunityTab.FEED) },
                        onNavigateToGroups = { viewModel.setTab(CommunityTab.COMMUNITIES) },
                        onNavigateToFriends = { viewModel.setTab(CommunityTab.FRIENDS) },
                        onNewPost = { showNewPostDialog = true },
                        onOpenGroupDetail = { communityId -> viewModel.openGroupDetail(communityId) }
                    )
                    CommunityTab.FEED -> FeedContent(
                        state = state,
                        onSortChange = { viewModel.setSortMode(it) },
                        onTagChange = { viewModel.setSelectedTag(it) },
                        onExpandPost = { viewModel.expandPost(it) },
                        onUpvote = { viewModel.toggleUpvote(it) },
                        onDownvote = { viewModel.toggleDownvote(it) },
                        onSave = { viewModel.toggleSavePost(it) },
                        onComment = { commentSheetPost = state.posts.find { p -> p.id == it } },
                        onClearCommunityFilter = { viewModel.selectCommunity(null) },
                        onProfileClick = { memberId -> onNavigateToProfile(memberId) }
                    )
                    CommunityTab.COMMUNITIES -> CommunitiesTab(
                        communities = state.communities,
                        currentMemberId = state.currentMemberId,
                        onJoin = { viewModel.joinCommunity(it) },
                        onSelect = { viewModel.selectCommunity(it); viewModel.setTab(CommunityTab.FEED) },
                        onCreate = { showCreateCommunity = true },
                        onManageMembers = { manageCommunityId = it; viewModel.loadPendingRequests(it) },
                        onOpenGroupDetail = { communityId -> viewModel.openGroupDetail(communityId) }
                    )
                    CommunityTab.FRIENDS -> FriendsTab(
                        friends = state.friends,
                        incomingRequests = state.incomingFriendRequests,
                        onAccept = { viewModel.acceptFriendRequest(it) },
                        onReject = { viewModel.rejectFriendRequest(it) },
                        onChat = { viewModel.openChat(it) }
                    )
                }
            }
        }

        // ── FAB ──
        if (state.currentTab == CommunityTab.FEED || state.currentTab == CommunityTab.HUB) {
            FloatingActionButton(
                onClick = { showNewPostDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = TealPrimary,
                contentColor = NavyDark,
                shape = CircleShape
            ) { Icon(Icons.Default.Edit, "New Post") }
        }

        // ── Scrim for rail (click outside to close) ──
        if (isRailExpanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(onClick = { isRailExpanded = false })
                    .background(Color.Black.copy(0.3f))
            )
        }
    }

    // ── Dialogs ──
    if (showNewPostDialog) {
        NewPostDialog(
            authorName = state.studentName,
            viewModel = viewModel,
            onDismiss = { showNewPostDialog = false },
            onSubmit = { title, body, tag, attachment, uri, name ->
                viewModel.addPost(title, body, tag, attachment, uri, name)
                showNewPostDialog = false
            }
        )
    }

    // ── Upload progress overlay ──
    if (state.isUploading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.6f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(SurfaceCard),
                modifier = Modifier.padding(40.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = TealPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = when {
                            state.uploadProgress < 70f -> "Compressing media…"
                            state.uploadProgress < 100f -> "Uploading…"
                            else -> "Almost done…"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.uploadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = TealPrimary,
                        trackColor = TealPrimary.copy(0.15f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${state.uploadProgress.toInt()}%",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }

    commentSheetPost?.let { post ->
        CommentSheet(
            post = post,
            onDismiss = { commentSheetPost = null },
            onAddComment = { viewModel.addComment(post.id, it); commentSheetPost = null }
        )
    }

    if (showCreateCommunity) {
        CreateCommunityDialog(
            onDismiss = { showCreateCommunity = false },
            onCreate = { n, d, p, e -> viewModel.createCommunity(n, d, p, e); showCreateCommunity = false }
        )
    }

    manageCommunityId?.let { cId ->
        val community = state.communities.find { it.entity.communityId == cId }
        ManageMembersDialog(
            communityName = community?.entity?.name ?: "",
            pendingRequests = state.pendingRequests,
            onApprove = { viewModel.approveMember(cId, it) },
            onReject = { viewModel.rejectMember(cId, it) },
            onDismiss = { manageCommunityId = null }
        )
    }
}

// ═══════════════════════════════════════════════════════
// ── Animated Navigation Rail ──
// ═══════════════════════════════════════════════════════

private data class NavRailItem(
    val tab: CommunityTab,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val navRailItems = listOf(
    NavRailItem(CommunityTab.HUB, "Hub", Icons.Outlined.Home, Icons.Filled.Home),
    NavRailItem(CommunityTab.FEED, "Feed", Icons.Outlined.Forum, Icons.Filled.Forum),
    NavRailItem(CommunityTab.COMMUNITIES, "Groups", Icons.Outlined.Groups, Icons.Filled.Groups),
    NavRailItem(CommunityTab.FRIENDS, "Friends", Icons.Outlined.People, Icons.Filled.People)
)

@Composable
private fun CommunityNavigationRail(
    currentTab: CommunityTab,
    isExpanded: Boolean,
    onTabSelected: (CommunityTab) -> Unit,
    onToggle: () -> Unit
) {
    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 180.dp else 56.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "railWidth"
    )

    Column(
        Modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(NavyMedium, NavyLight.copy(0.6f), NavyMedium)
                )
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle button
        IconButton(
            onClick = onToggle,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "menuIcon"
            ) { expanded ->
                Icon(
                    if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                    contentDescription = "Toggle navigation",
                    tint = TealPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        HorizontalDivider(
            color = TealPrimary.copy(0.15f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Navigation items
        navRailItems.forEach { item ->
            val isSelected = currentTab == item.tab
            val bgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 0.15f else 0f,
                animationSpec = tween(250),
                label = "bgAlpha"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TealPrimary.copy(bgAlpha))
                    .clickable { onTabSelected(item.tab) }
                    .padding(vertical = 12.dp, horizontal = if (isExpanded) 14.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) TealPrimary else TextMuted,
                    modifier = Modifier.size(22.dp)
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(200)) + expandHorizontally(tween(250)),
                    exit = fadeOut(tween(150)) + shrinkHorizontally(tween(200))
                ) {
                    Row {
                        Spacer(Modifier.width(14.dp))
                        Text(
                            item.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TealPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom branding
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200))
        ) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = TextMuted.copy(0.1f))
                Spacer(Modifier.height(8.dp))
                Text(
                    "StudyMate",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted.copy(0.5f)
                )
                Text(
                    "Community",
                    fontSize = 10.sp,
                    color = TextMuted.copy(0.3f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Top Bar (with hamburger toggle) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun CommunityTopBar(
    state: CommunityUiState,
    currentTab: CommunityTab,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleRail: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(NavyMedium.copy(0.9f), Color.Transparent)))
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Title with tab name
            val tabTitle = when (currentTab) {
                CommunityTab.HUB -> "Community Hub"
                CommunityTab.FEED -> "Feed"
                CommunityTab.COMMUNITIES -> "Groups"
                CommunityTab.FRIENDS -> "Friends"
            }
            Text(
                tabTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (currentTab == CommunityTab.FEED) {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        if (state.isSearching) Icons.Default.Close else Icons.Default.Search,
                        null,
                        tint = TextSecondary
                    )
                }
            }
        }
        if (currentTab == CommunityTab.FEED) {
            AnimatedVisibility(visible = state.isSearching) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search posts...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.15f),
                        cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Feed Content ──
// ═══════════════════════════════════════════════════════

@Composable
private fun FeedContent(
    state: CommunityUiState,
    onSortChange: (SortMode) -> Unit,
    onTagChange: (String) -> Unit,
    onExpandPost: (String?) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onSave: (String) -> Unit,
    onComment: (String) -> Unit,
    onClearCommunityFilter: () -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Community filter indicator
        state.selectedCommunityId?.let { cId ->
            val community = state.communities.find { it.entity.communityId == cId }
            if (community != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TealPrimary.copy(0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(community.entity.iconEmoji, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Viewing: ${community.entity.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearCommunityFilter) { Text("Show All", color = TextMuted, fontSize = 11.sp) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Sort chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.sortMode == mode,
                    onClick = { onSortChange(mode) },
                    label = { Text(mode.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary.copy(0.2f), selectedLabelColor = TealPrimary,
                        containerColor = SurfaceCard, labelColor = TextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selectedBorderColor = TealPrimary.copy(0.4f), borderColor = Color.Transparent,
                        enabled = true, selected = state.sortMode == mode
                    )
                )
            }
        }

        // Tag filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(COMMUNITY_TAGS) { tag ->
                val tc = getTagColor(tag)
                FilterChip(
                    selected = state.selectedTag == tag,
                    onClick = { onTagChange(tag) },
                    label = { Text(tag, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = tc.copy(0.2f), selectedLabelColor = tc,
                        containerColor = Color.Transparent, labelColor = TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Posts
        if (state.posts.isEmpty()) {
            EmptyFeedState(isFiltered = state.selectedTag != "All" || state.searchQuery.isNotBlank(), onClearFilter = { onTagChange("All") })
        } else {
            state.posts.forEach { post ->
                PostCard(
                    post = post,
                    isExpanded = state.expandedPostId == post.id,
                    onExpand = { onExpandPost(if (state.expandedPostId == post.id) null else post.id) },
                    onUpvote = { onUpvote(post.id) },
                    onDownvote = { onDownvote(post.id) },
                    onComment = { onComment(post.id) },
                    onSave = { onSave(post.id) },
                    onProfileClick = { onProfileClick(post.authorMemberId) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ═══════════════════════════════════════════════════════
// ── Post Card ──
// ═══════════════════════════════════════════════════════

@Composable
private fun PostCard(
    post: CommunityPost,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, if (isExpanded) TealPrimary.copy(0.2f) else TextMuted.copy(0.08f))
    ) {
        Column(Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(getAuthorColor(post.author), getAuthorColor(post.author).copy(0.6f)))),
                    contentAlignment = Alignment.Center
                ) { Text(post.authorInitials.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        post.author,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealPrimary,
                        modifier = Modifier.clickable(onClick = onProfileClick)
                    )
                    if (post.authorUsername.isNotBlank()) {
                        Text(
                            "@${post.authorUsername}",
                            fontSize = 11.sp,
                            color = TealPrimary.copy(0.6f)
                        )
                    }
                    Text(post.timeAgo, fontSize = 11.sp, color = TextMuted)
                }
                val tagColor = getTagColor(post.tag)
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(tagColor.copy(0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(post.tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tagColor)
                }
            }
            Spacer(Modifier.height(10.dp))

            // Title
            Text(post.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = if (isExpanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(post.body, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp, maxLines = if (isExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)

            // Attachment — media preview (image/video/PDF thumbnails)
            if (post.attachmentUri != null || post.attachment != null) {
                Spacer(Modifier.height(8.dp))
                MediaPreviewCard(
                    attachmentUri = post.attachmentUri ?: post.attachment?.absolutePath,
                    attachmentName = post.attachmentName ?: post.attachment?.name
                )
            }

            Spacer(Modifier.height(10.dp))

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Votes
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onUpvote, modifier = Modifier.size(30.dp)) {
                        Icon(if (post.isUpvoted) Icons.Default.ThumbUp else Icons.Default.ThumbUp, null,
                            tint = if (post.isUpvoted) TealPrimary else TextMuted, modifier = Modifier.size(16.dp))
                    }
                    Text(formatVotes(post.upvotes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (post.isUpvoted) TealPrimary else if (post.isDownvoted) RedError else TextSecondary)
                    IconButton(onClick = onDownvote, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.ThumbDown, null,
                            tint = if (post.isDownvoted) RedError else TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))

                // Comments
                Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { onComment() }.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${post.comments.size}", fontSize = 12.sp, color = TextMuted)
                }
                Spacer(Modifier.weight(1f))

                // Save
                IconButton(onClick = onSave, modifier = Modifier.size(30.dp)) {
                    Icon(if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null,
                        tint = if (post.isSaved) AmberAccent else TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            // Expanded comments
            if (isExpanded && post.comments.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = TextMuted.copy(0.15f))
                Spacer(Modifier.height(8.dp))
                post.comments.forEach { c ->
                    CommentItem(c)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Comment ──
// ═══════════════════════════════════════════════════════

@Composable
private fun CommentItem(comment: CommunityComment) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).padding(10.dp)) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(getAuthorColor(comment.author), getAuthorColor(comment.author).copy(0.6f)))),
            contentAlignment = Alignment.Center
        ) { Text(comment.author.take(1).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row { Text(comment.author, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary); Spacer(Modifier.width(6.dp)); Text(comment.timeAgo, fontSize = 10.sp, color = TextMuted) }
            Text(comment.body, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Empty State ──
// ═══════════════════════════════════════════════════════

@Composable
private fun EmptyFeedState(isFiltered: Boolean, onClearFilter: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(if (isFiltered) "No posts match your filter" else "No posts yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            if (isFiltered) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onClearFilter) { Text("Clear filters", color = TealPrimary, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── New Post Dialog (with file picker) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun NewPostDialog(
    authorName: String,
    viewModel: CommunityViewModel,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, ScannedFile?, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("General") }
    var showTagPicker by remember { mutableStateOf(false) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            val (name, _) = viewModel.getFileInfoFromUri(uri)
            pickedName = name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSubmit(title.trim(), body.trim(), selectedTag, null, pickedUri?.toString(), pickedName) },
                enabled = title.isNotBlank() && body.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Post", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent))), contentAlignment = Alignment.Center) {
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
                // Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tag:", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.width(8.dp))
                    val tagColor = getTagColor(selectedTag)
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(tagColor.copy(0.12f)).clickable { showTagPicker = !showTagPicker }.padding(horizontal = 10.dp, vertical = 5.dp)) {
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
                            FilterChip(selected = selectedTag == tag, onClick = { selectedTag = tag; showTagPicker = false },
                                label = { Text(tag, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = tc.copy(0.2f), selectedLabelColor = tc, containerColor = Color.Transparent, labelColor = TextMuted),
                                shape = RoundedCornerShape(8.dp))
                        }
                    }
                }

                OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Post title", color = TextMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.2f), cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = body, onValueChange = { body = it }, placeholder = { Text("Share your question, tip, or resource...", color = TextMuted) }, minLines = 4, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.2f), cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary))

                // File picker button
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { filePicker.launch("*/*") }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AttachFile, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(pickedName ?: "Attach file (optional)", fontSize = 13.sp, color = if (pickedName != null) TextPrimary else TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (pickedName != null) {
                        IconButton(onClick = { pickedUri = null; pickedName = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(14.dp))
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
// ── Comment Sheet ──
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentSheet(post: CommunityPost, onDismiss: () -> Unit, onAddComment: (String) -> Unit) {
    var comment by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = NavyMedium, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Comments", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextMuted) }
            }
            Text(post.title, fontSize = 14.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            post.comments.forEach { c -> CommentItem(c) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = comment, onValueChange = { comment = it },
                    placeholder = { Text("Add a comment...", color = TextMuted, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.15f), cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { if (comment.isNotBlank()) onAddComment(comment.trim()) },
                    enabled = comment.isNotBlank(),
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(if (comment.isNotBlank()) TealPrimary else TealPrimary.copy(0.2f))
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (comment.isNotBlank()) NavyDark else TextMuted, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Helpers ──
// ═══════════════════════════════════════════════════════

internal fun getTagColor(tag: String): Color = when (tag) {
    "All" -> TealPrimary; "General" -> TextSecondary; "Question" -> PurpleAccent
    "Study Tips" -> TealPrimary; "Notes" -> GreenSuccess; "Resources" -> AmberAccent
    "Exam Prep" -> RedError; "Motivation" -> PinkAccent; "Discussion" -> PurpleLight
    "Help" -> AmberAccent; "Achievement" -> AmberAccent; else -> TextMuted
}

internal fun getAuthorColor(author: String): Color {
    val colors = listOf(TealPrimary, PurpleAccent, AmberAccent, PinkAccent, GreenSuccess, PurpleLight, TealDark)
    return colors[Math.abs(author.hashCode()) % colors.size]
}

private fun formatVotes(v: Int): String = if (v >= 1000) String.format(Locale.US, "%.1fk", v / 1000.0) else v.toString()
