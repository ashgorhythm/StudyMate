package com.example.myandroidapp.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*

// ═══════════════════════════════════════════════════════
// ── Community Hub — The landing page for the Community module ──
// Shows welcome card, quick actions, trending, stats, featured groups
// ═══════════════════════════════════════════════════════

@Composable
fun CommunityHubScreen(
    state: CommunityUiState,
    onNavigateToFeed: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNewPost: () -> Unit,
    onOpenGroupDetail: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Hero Welcome Card ──
        HeroWelcomeCard(
            userName = state.studentName,
            username = state.username,
            postCount = state.posts.size,
            friendCount = state.friends.size,
            communityCount = state.communities.size
        )

        Spacer(Modifier.height(20.dp))

        // ── Quick Actions Grid ──
        Text(
            "Quick Actions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Edit,
                label = "New Post",
                color = TealPrimary,
                modifier = Modifier.weight(1f),
                onClick = onNewPost
            )
            QuickActionCard(
                icon = Icons.Default.Forum,
                label = "Feed",
                color = PurpleAccent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFeed
            )
            QuickActionCard(
                icon = Icons.Default.Groups,
                label = "Groups",
                color = AmberAccent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToGroups
            )
            QuickActionCard(
                icon = Icons.Default.People,
                label = "Friends",
                color = PinkAccent,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToFriends
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Activity Overview ──
        Text(
            "Your Activity",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("Posts", "${state.posts.count { it.authorMemberId == state.currentMemberId }}", Icons.AutoMirrored.Filled.Article, TealPrimary, Modifier.weight(1f))
            StatCard("Upvotes", "${state.posts.filter { it.authorMemberId == state.currentMemberId }.sumOf { it.upvotes }}", Icons.Default.ThumbUp, GreenSuccess, Modifier.weight(1f))
            StatCard("Saved", "${state.posts.count { it.isSaved }}", Icons.Default.Bookmark, AmberAccent, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // ── Trending Topics ──
        TrendingTopicsSection(posts = state.posts)

        Spacer(Modifier.height(24.dp))

        // ── Featured Communities ──
        if (state.communities.isNotEmpty()) {
            Text(
                "Featured Communities",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))

            state.communities.take(3).forEach { info ->
                FeaturedCommunityCard(
                    emoji = info.entity.iconEmoji,
                    name = info.entity.name,
                    members = info.entity.memberCount,
                    description = info.entity.description,
                    isJoined = info.membershipStatus == com.example.myandroidapp.data.model.MembershipStatus.APPROVED,
                    onClick = { onOpenGroupDetail(info.entity.communityId) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Recent Posts Preview ──
        if (state.posts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Posts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToFeed) {
                    Text("See All", color = TealPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            state.posts.take(3).forEach { post ->
                CompactPostPreview(post = post, onClick = onNavigateToFeed)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

// ─────────────────────────────────────────────────────────
// ── Hero Welcome Card ──
// ─────────────────────────────────────────────────────────

@Composable
private fun HeroWelcomeCard(
    userName: String,
    username: String,
    postCount: Int,
    friendCount: Int,
    communityCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            TealPrimary.copy(0.15f),
                            PurpleAccent.copy(0.12f),
                            TealDark.copy(0.18f)
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 400f, 300f)
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(TealPrimary.copy(0.3f), PurpleAccent.copy(0.15f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Gradient avatar
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(TealPrimary, PurpleAccent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            userName.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Welcome back,",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        if (username.isNotBlank()) {
                            Text(
                                "@$username",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TealPrimary.copy(0.8f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Mini stats row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat(emoji = "📝", value = "$postCount", label = "Posts")
                    MiniStat(emoji = "👥", value = "$friendCount", label = "Friends")
                    MiniStat(emoji = "🏛️", value = "$communityCount", label = "Groups")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

// ─────────────────────────────────────────────────────────
// ── Quick Action Card ──
// ─────────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.9f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, color.copy(0.12f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Stat Card ──
// ─────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, color.copy(0.12f))
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, fontSize = 11.sp, color = TextMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Trending Topics ──
// ─────────────────────────────────────────────────────────

@Composable
private fun TrendingTopicsSection(posts: List<CommunityPost>) {
    // Count tag frequency
    val tagCounts = posts.groupBy { it.tag }
        .mapValues { it.value.size }
        .entries
        .sortedByDescending { it.value }
        .take(5)

    if (tagCounts.isEmpty()) return

    Text(
        "Trending Topics",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tagCounts.forEach { (tag, count) ->
            val tagColor = getTagColor(tag)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(tagColor.copy(0.08f)),
                border = BorderStroke(1.dp, tagColor.copy(0.15f))
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tagColor)
                        Text("$count posts", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Featured Community Card ──
// ─────────────────────────────────────────────────────────

@Composable
private fun FeaturedCommunityCard(
    emoji: String,
    name: String,
    members: Int,
    description: String,
    isJoined: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, if (isJoined) TealPrimary.copy(0.15f) else TextMuted.copy(0.08f))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(TealPrimary.copy(0.12f), PurpleAccent.copy(0.08f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.isNotBlank()) {
                    Text(
                        description,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "$members members",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            if (isJoined) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenSuccess.copy(0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Joined", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Compact Post Preview (for hub recent posts) ──
// ─────────────────────────────────────────────────────────

@Composable
private fun CompactPostPreview(
    post: CommunityPost,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, TextMuted.copy(0.06f))
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Author avatar
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(getAuthorColor(post.author), getAuthorColor(post.author).copy(0.6f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    post.authorInitials.take(1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    post.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.author, fontSize = 11.sp, color = TextMuted)
                    if (post.authorUsername.isNotBlank()) {
                        Text(" @${post.authorUsername}", fontSize = 11.sp, color = TealPrimary.copy(0.5f))
                    }
                    Text(" · ", fontSize = 11.sp, color = TextMuted)
                    Text(post.timeAgo, fontSize = 11.sp, color = TextMuted)
                }
            }
            // Upvote count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ThumbUp,
                    null,
                    tint = TealPrimary.copy(0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${post.upvotes}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }
        }
    }
}
