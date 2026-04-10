package com.example.myandroidapp.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*

// ═══════════════════════════════════════════════════════
// ── User Profile Screen — view another user's profile ──
// Inspired by Stitch "Aether Scholar" design
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    onBack: () -> Unit,
    onChat: ((String) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDark, PurpleDeep, NavyDark)
                )
            )
    ) {
        when {
            state.isLoading -> {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Loading profile…", color = TextSecondary, fontSize = 14.sp)
                }
            }
            state.error != null -> {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = RedError, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(state.error ?: "Error", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(accent)
                    ) { Text("Go Back") }
                }
            }
            state.profile != null -> {
                val profile = state.profile!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // ── Top bar ──
                    item {
                        TopAppBar(
                            title = { Text("Profile", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = TextPrimary
                            )
                        )
                    }

                    // ── Avatar Section ──
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Animated scalloped avatar border
                            val infiniteTransition = rememberInfiniteTransition(label = "avatarRotation")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 12000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )

                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Scalloped gradient ring
                                Canvas(modifier = Modifier.size(120.dp)) {
                                    val strokeWidth = 4.dp.toPx()
                                    val radius = size.minDimension / 2 - strokeWidth
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                TealPrimary, PurpleAccent, PinkAccent,
                                                TealPrimary, PurpleAccent, TealPrimary
                                            ),
                                            center = Offset(size.width / 2, size.height / 2)
                                        ),
                                        radius = radius,
                                        style = Stroke(
                                            width = strokeWidth,
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(12f, 6f),
                                                phase = rotation * 0.6f
                                            )
                                        )
                                    )
                                }
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(TealPrimary.copy(0.8f), PurpleAccent.copy(0.8f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.displayName.take(2).uppercase(),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Name
                            Text(
                                text = profile.displayName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (profile.username.isNotBlank()) {
                                Text(
                                    text = "@${profile.username}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = accent.copy(0.8f)
                                )
                            }
                        }
                    }

                    // ── Stats Row ──
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                emoji = "🔥",
                                label = "Streak",
                                value = "${profile.studyStreak}d",
                                gradientColors = listOf(AmberAccent.copy(0.15f), PinkAccent.copy(0.08f)),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                emoji = "📚",
                                label = "Hours",
                                value = "${profile.totalStudyHours}h",
                                gradientColors = listOf(TealPrimary.copy(0.15f), PurpleAccent.copy(0.08f)),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                emoji = "💬",
                                label = "Posts",
                                value = "${state.postCount}",
                                gradientColors = listOf(PurpleAccent.copy(0.15f), PinkAccent.copy(0.08f)),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ── Bio Section ──
                    item {
                        if (profile.bio.isNotBlank()) {
                            Spacer(Modifier.height(20.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(SurfaceCard),
                                border = BorderStroke(1.dp, accent.copy(0.1f))
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Bio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = profile.bio,
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    // ── Friend Request / Action Buttons ──
                    if (!state.isCurrentUser) {
                        item {
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (state.friendshipStatus) {
                                    FriendshipStatus.NONE -> {
                                        Button(
                                            onClick = { viewModel.sendFriendRequest() },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accent,
                                                contentColor = NavyDark
                                            )
                                        ) {
                                            Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Send Friend Request", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    FriendshipStatus.PENDING_SENT -> {
                                        OutlinedButton(
                                            onClick = {},
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, AmberAccent.copy(0.5f))
                                        ) {
                                            Icon(Icons.Default.HourglassBottom, null, Modifier.size(18.dp), tint = AmberAccent)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Request Pending", color = AmberAccent, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    FriendshipStatus.PENDING_RECEIVED -> {
                                        Button(
                                            onClick = {},
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GreenSuccess.copy(0.2f),
                                                contentColor = GreenSuccess
                                            )
                                        ) {
                                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Accept Request", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    FriendshipStatus.FRIENDS -> {
                                        OutlinedButton(
                                            onClick = {},
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, GreenSuccess.copy(0.3f))
                                        ) {
                                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = GreenSuccess)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Friends", color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                                        }

                                        Button(
                                            onClick = { onChat?.invoke(profile.memberId) },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accent.copy(0.15f),
                                                contentColor = accent
                                            )
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Message", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Recent Posts ──
                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent Posts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${state.postCount} total",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (state.posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📝", fontSize = 40.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No posts yet", fontSize = 14.sp, color = TextMuted)
                                }
                            }
                        }
                    }

                    items(state.posts.take(10), key = { it.id }) { post ->
                        CompactPostCard(
                            title = post.title,
                            body = post.body,
                            tag = post.tag,
                            timeAgo = post.timeAgo,
                            upvotes = post.upvotes,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // ── Success snackbar ──
                AnimatedVisibility(
                    visible = state.requestSent,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(GreenSuccess.copy(0.15f)),
                        border = BorderStroke(1.dp, GreenSuccess.copy(0.3f))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Friend request sent!", color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Stat Card (glassmorphism) ──
// ─────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    emoji: String,
    label: String,
    value: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
        border = BorderStroke(1.dp, TextMuted.copy(0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text(label, fontSize = 11.sp, color = TextMuted)
        }
    }
}

// ─────────────────────────────────────────────────────────
// ── Compact Post Card ──
// ─────────────────────────────────────────────────────────

@Composable
private fun CompactPostCard(
    title: String,
    body: String,
    tag: String,
    timeAgo: String,
    upvotes: Int,
    modifier: Modifier = Modifier
) {
    val tagColor = when (tag.lowercase()) {
        "question" -> AmberAccent
        "resource" -> TealPrimary
        "discussion" -> PurpleAccent
        "announcement" -> PinkAccent
        "study tips" -> GreenSuccess
        else -> TextSecondary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(0.06f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tag chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagColor.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tagColor)
                }
                Spacer(Modifier.weight(1f))
                Text(timeAgo, fontSize = 10.sp, color = TextMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ThumbUp, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("$upvotes", fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}
