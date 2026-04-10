package com.example.myandroidapp.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.myandroidapp.data.model.*
import com.example.myandroidapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

// ═══════════════════════════════════════════════════════
// ── Community Group Detail Screen (CommunityHub) ──
// Full-screen group view with header, tabs, posts, members, resources
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityGroupDetailScreen(
    communityInfo: CommunityInfo,
    posts: List<CommunityPost>,
    members: List<CommunityMemberEntity>,
    currentMemberId: String,
    onBack: () -> Unit,
    onNewPost: () -> Unit,
    onExpandPost: (String?) -> Unit,
    expandedPostId: String?,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onComment: (String) -> Unit,
    onSave: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onManageMembers: () -> Unit
) {
    val community = communityInfo.entity
    val isMember = communityInfo.membershipStatus == MembershipStatus.APPROVED
    val isAdmin = communityInfo.memberRole == CommunityRole.ADMIN
    val isMod = communityInfo.memberRole == CommunityRole.MODERATOR

    val tabs = listOf("Posts", "Members", "Resources")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var showMoreMenu by remember { mutableStateOf(false) }

    // Filter posts for this community
    val communityPosts = posts.filter { it.communityId == community.communityId }
    val approvedMembers = members.filter { it.status == MembershipStatus.APPROVED }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDark, NavyMedium.copy(0.5f), NavyDark)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Top App Bar ──
            item {
                DetailTopBar(
                    onBack = onBack,
                    showMoreMenu = showMoreMenu,
                    onToggleMenu = { showMoreMenu = !showMoreMenu },
                    onDismissMenu = { showMoreMenu = false },
                    isAdmin = isAdmin || isMod
                )
            }

            // ── Community Header ──
            item {
                CommunityHeaderSection(
                    community = community,
                    isMember = isMember,
                    memberCount = approvedMembers.size,
                    postCount = communityPosts.size
                )
            }

            // ── Tab Navigation ──
            item {
                DetailTabRow(
                    tabs = tabs,
                    selectedIndex = pagerState.currentPage,
                    onTabSelected = { idx ->
                        coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                    }
                )
            }

            // ── Tab Content ──
            when (pagerState.currentPage) {
                0 -> {
                    // Posts Feed
                    if (communityPosts.isEmpty()) {
                        item {
                            EmptyContentState(
                                emoji = "📝",
                                title = "No posts yet",
                                subtitle = "Be the first to share something!",
                                actionLabel = if (isMember) "Create Post" else null,
                                onAction = onNewPost
                            )
                        }
                    } else {
                        items(communityPosts, key = { it.id }) { post ->
                            DetailPostCard(
                                post = post,
                                isExpanded = expandedPostId == post.id,
                                onExpand = {
                                    onExpandPost(
                                        if (expandedPostId == post.id) null else post.id
                                    )
                                },
                                onUpvote = { onUpvote(post.id) },
                                onDownvote = { onDownvote(post.id) },
                                onComment = { onComment(post.id) },
                                onSave = { onSave(post.id) },
                                onProfileClick = { onProfileClick(post.authorMemberId) }
                            )
                        }
                    }
                }

                1 -> {
                    // Members List
                    if (approvedMembers.isEmpty()) {
                        item {
                            EmptyContentState(
                                emoji = "👥",
                                title = "No members yet",
                                subtitle = "Be the first to join this community!"
                            )
                        }
                    } else {
                        item {
                            // Admin tools header
                            if (isAdmin || isMod) {
                                AdminToolsHeader(
                                    memberCount = approvedMembers.size,
                                    onManage = onManageMembers
                                )
                            }
                        }
                        items(approvedMembers, key = { it.memberId }) { member ->
                            MemberCard(
                                member = member,
                                isCurrentUser = member.memberId == currentMemberId
                            )
                        }
                    }
                }

                2 -> {
                    // Resources (placeholder for now)
                    item {
                        EmptyContentState(
                            emoji = "📁",
                            title = "No resources shared",
                            subtitle = "Resources shared by members will appear here"
                        )
                    }
                }
            }

            // Bottom spacer for FAB
            item { Spacer(Modifier.height(100.dp)) }
        }

        // ── FAB ──
        if (isMember && pagerState.currentPage == 0) {
            FloatingActionButton(
                onClick = onNewPost,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = TealPrimary,
                contentColor = NavyDark,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Edit, "Create Post")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Top App Bar ──
// ═══════════════════════════════════════════════════════

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    showMoreMenu: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    isAdmin: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(NavyMedium.copy(0.95f), Color.Transparent)
                    )
                )
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "Back",
                tint = TextPrimary, modifier = Modifier.size(24.dp)
            )
        }

        Text(
            "CommunityHub",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = { /* search */ }) {
            Icon(Icons.Default.Search, "Search", tint = TextSecondary)
        }

        Box {
            IconButton(onClick = onToggleMenu) {
                Icon(Icons.Default.MoreVert, "More", tint = TextSecondary)
            }
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = onDismissMenu,
                containerColor = NavyMedium.copy(0.95f)
            ) {
                DropdownMenuItem(
                    text = { Text("Share Community", color = TextPrimary) },
                    onClick = { onDismissMenu() },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = TextSecondary) }
                )
                DropdownMenuItem(
                    text = { Text("Report", color = TextPrimary) },
                    onClick = { onDismissMenu() },
                    leadingIcon = { Icon(Icons.Default.Flag, null, tint = TextSecondary) }
                )
                if (isAdmin) {
                    DropdownMenuItem(
                        text = { Text("Community Settings", color = TextPrimary) },
                        onClick = { onDismissMenu() },
                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = TextSecondary) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Community Header Section ──
// ═══════════════════════════════════════════════════════

@Composable
private fun CommunityHeaderSection(
    community: CommunityEntity,
    isMember: Boolean,
    memberCount: Int,
    postCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            TealPrimary.copy(glowAlpha),
                            PurpleAccent.copy(glowAlpha * 0.7f),
                            TealDark.copy(glowAlpha * 1.2f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(TealPrimary.copy(0.2f), PurpleAccent.copy(0.1f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    // Community Avatar
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        TealPrimary.copy(0.2f),
                                        PurpleAccent.copy(0.15f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                TealPrimary.copy(0.2f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(community.iconEmoji, fontSize = 32.sp)
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        // Community Name
                        Text(
                            community.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Subtitle with member count
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatMemberCount(memberCount),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(" • ", fontSize = 13.sp, color = TextMuted)
                            Text(
                                if (community.isPublic) "Public Group" else "Private Group",
                                fontSize = 13.sp,
                                color = if (community.isPublic) TealPrimary.copy(0.8f) else AmberAccent.copy(0.8f)
                            )
                            if (!community.isPublic) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Lock, null,
                                    tint = AmberAccent.copy(0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // Joined Status Badge
                    if (isMember) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(TealPrimary, TealDark)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Joined",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                        }
                    }
                }

                // Description
                if (community.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        community.description,
                        fontSize = 13.sp,
                        color = TextSecondary.copy(0.85f),
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick stats row
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeaderStat(
                        icon = Icons.Default.People,
                        value = "$memberCount",
                        label = "Members",
                        color = TealPrimary
                    )
                    HeaderStat(
                        icon = Icons.Default.Forum,
                        value = "$postCount",
                        label = "Posts",
                        color = PurpleAccent
                    )
                    HeaderStat(
                        icon = Icons.Default.Whatshot,
                        value = "Active",
                        label = "Status",
                        color = GreenSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderStat(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color.copy(0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 10.sp, color = TextMuted)
    }
}

// ═══════════════════════════════════════════════════════
// ── Tab Navigation ──
// ═══════════════════════════════════════════════════════

@Composable
private fun DetailTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabIcons = listOf(
        Icons.Default.Forum,
        Icons.Default.People,
        Icons.Default.Folder
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard.copy(0.7f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val bgAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(250),
                    label = "tabBg"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Brush.linearGradient(
                                listOf(TealPrimary.copy(0.2f), PurpleAccent.copy(0.12f))
                            )
                            else Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            )
                        )
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            tabIcons[index], null,
                            tint = if (isSelected) TealPrimary else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TealPrimary else TextMuted
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Post Card for Detail Screen ──
// ═══════════════════════════════════════════════════════

@Composable
private fun DetailPostCard(
    post: CommunityPost,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(
            1.dp,
            if (isExpanded) TealPrimary.copy(0.25f) else TextMuted.copy(0.06f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── Header Row ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                // User avatar
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    getAuthorColor(post.author),
                                    getAuthorColor(post.author).copy(0.6f)
                                )
                            )
                        )
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        post.authorInitials.take(1),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        post.author,
                        fontSize = 14.sp,
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
                    Text(
                        post.timeAgo,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Category tag
                val tagColor = getTagColor(post.tag)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tagColor.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        post.tag.uppercase(Locale.US),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Post Content ──
            Text(
                post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (post.body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    post.body,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Media Section ──
            if (post.attachmentUri != null || post.attachment != null) {
                Spacer(Modifier.height(10.dp))
                MediaPreviewCard(
                    attachmentUri = post.attachmentUri ?: post.attachment?.absolutePath,
                    attachmentName = post.attachmentName ?: post.attachment?.name
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Interaction Row ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Upvote/Downvote cluster
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavyMedium.copy(0.5f))
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onUpvote,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ThumbUp, null,
                            tint = if (post.isUpvoted) TealPrimary else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        formatVotes(post.upvotes),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            post.isUpvoted -> TealPrimary
                            post.isDownvoted -> RedError
                            else -> TextSecondary
                        }
                    )
                    IconButton(
                        onClick = onDownvote,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ThumbDown, null,
                            tint = if (post.isDownvoted) RedError else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Comment button
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavyMedium.copy(0.5f))
                        .clickable { onComment() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline, null,
                        tint = TextMuted, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${post.comments.size}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(Modifier.weight(1f))

                // Save/Bookmark
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        null,
                        tint = if (post.isSaved) AmberAccent else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Expanded Comments ──
            AnimatedVisibility(visible = isExpanded && post.comments.isNotEmpty()) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = TextMuted.copy(0.15f))
                    Spacer(Modifier.height(8.dp))
                    post.comments.forEach { c ->
                        DetailCommentItem(c)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCommentItem(comment: CommunityComment) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NavyMedium.copy(0.4f))
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            getAuthorColor(comment.author),
                            getAuthorColor(comment.author).copy(0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                comment.author.take(1).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    comment.author,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    comment.timeAgo,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            Text(
                comment.body,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Member Card ──
// ═══════════════════════════════════════════════════════

@Composable
private fun MemberCard(
    member: CommunityMemberEntity,
    isCurrentUser: Boolean
) {
    val roleColor = when (member.role) {
        CommunityRole.ADMIN -> AmberAccent
        CommunityRole.MODERATOR -> PurpleAccent
        CommunityRole.MEMBER -> TealPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(
            1.dp,
            if (isCurrentUser) TealPrimary.copy(0.2f) else TextMuted.copy(0.06f)
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                getAuthorColor(member.displayName),
                                getAuthorColor(member.displayName).copy(0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.displayName.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        member.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "(You)",
                            fontSize = 11.sp,
                            color = TealPrimary.copy(0.7f)
                        )
                    }
                }
                Text(
                    "Joined • ${member.role.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            // Role badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(roleColor.copy(0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    member.role.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = roleColor
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Admin Tools Header ──
// ═══════════════════════════════════════════════════════

@Composable
private fun AdminToolsHeader(
    memberCount: Int,
    onManage: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$memberCount Members",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        TextButton(onClick = onManage) {
            Icon(
                Icons.Default.ManageAccounts, null,
                tint = PurpleAccent, modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Manage Requests",
                fontSize = 12.sp,
                color = PurpleAccent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Empty Content State ──
// ═══════════════════════════════════════════════════════

@Composable
private fun EmptyContentState(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Helpers ──
// ═══════════════════════════════════════════════════════

private fun formatMemberCount(count: Int): String {
    return when {
        count >= 10000 -> String.format(Locale.US, "%.1fk Scholars", count / 1000.0)
        count >= 1000 -> String.format(Locale.US, "%.1fk Members", count / 1000.0)
        count == 1 -> "1 Member"
        else -> "$count Members"
    }
}

private fun formatVotes(v: Int): String {
    return if (v >= 1000) String.format(Locale.US, "%.1fk", v / 1000.0) else v.toString()
}
