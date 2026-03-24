package com.example.myandroidapp.ui.screens.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import java.text.SimpleDateFormat
import java.util.*

data class CommunityPost(
    val id: String,
    val author: String,
    val authorInitials: String,
    val timeAgo: String,
    val title: String,
    val body: String,
    val upvotes: Int,
    val commentsCount: Int,
    val isUpvoted: Boolean = false,
    val isDownvoted: Boolean = false,
    val tag: String,
    val tagColor: Color
)

val dummyPosts = listOf(
    CommunityPost(
        "1", "Sarah M.", "SM", "2h ago",
        "Best resources for Calculus III?",
        "Hey everyone, I'm struggling with multiple integrals in Calc III. Does anyone have good video playlists or PDF notes they could share?",
        upvotes = 42, commentsCount = 12, tag = "Help Needed", tagColor = AmberAccent
    ),
    CommunityPost(
        "2", "Alex G.", "AG", "5h ago",
        "Pomodoro Technique actually works!",
        "I used to study 4 hours straight and burn out. Switched to 50/10 Pomodoro blocks today using the app's Focus mode, and the difference is insane. Highly recommend!",
        upvotes = 156, commentsCount = 24, tag = "Study Tips", tagColor = GreenSuccess, isUpvoted = true
    ),
    CommunityPost(
        "3", "University Staff", "US", "1d ago",
        "Library hours extended for Finals Week",
        "Starting next Monday, the main campus library will be open 24/7. Please remember your student ID card for entry after 10 PM. Good luck with exams!",
        upvotes = 890, commentsCount = 45, tag = "Announcement", tagColor = PinkAccent
    ),
    CommunityPost(
        "4", "Emily C.", "EC", "2d ago",
        "Lost my notes for Biology 101 unit 3 :(",
        "If anyone is taking Dr. Smith's bio class and can share their notes on cell respiration, I would be forever grateful!",
        upvotes = 15, commentsCount = 3, tag = "Notes", tagColor = PurpleAccent
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(onBack: () -> Unit) {
    val adaptive = rememberAdaptiveInfo()

    var posts by remember { mutableStateOf(dummyPosts) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: New Post */ },
                containerColor = TealPrimary,
                contentColor = NavyDark,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "New Post")
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (adaptive.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified)
                            Modifier.widthIn(max = adaptive.maxContentWidth)
                        else Modifier
                    )
                    .fillMaxSize()
                    .padding(horizontal = adaptive.horizontalPadding)
                    .align(Alignment.TopCenter)
            ) {
                // Header
                Row(
                    Modifier.padding(top = if (adaptive.isTablet) 24.dp else 48.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("University Community", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                // Filters
                Row(
                    modifier = Modifier.padding(bottom = 16.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Trending", "Study Tips", "Help Needed", "Announcements").forEachIndexed { index, tag ->
                        FilterChip(
                            selected = index == 0,
                            onClick = { },
                            label = { Text(tag, color = if (index == 0) NavyDark else TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                containerColor = SurfaceCard
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = index == 0,
                                borderColor = TealPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // Feed
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts) { post ->
                        PostCard(
                            post = post,
                            onUpvote = {
                                posts = posts.map {
                                    if (it.id == post.id) {
                                        if (it.isUpvoted) it.copy(isUpvoted = false, upvotes = it.upvotes - 1)
                                        else it.copy(isUpvoted = true, isDownvoted = false, upvotes = it.upvotes + (if (it.isDownvoted) 2 else 1))
                                    } else it
                                }
                            },
                            onDownvote = {
                                posts = posts.map {
                                    if (it.id == post.id) {
                                        if (it.isDownvoted) it.copy(isDownvoted = false, upvotes = it.upvotes + 1)
                                        else it.copy(isDownvoted = true, isUpvoted = false, upvotes = it.upvotes - (if (it.isUpvoted) 2 else 1))
                                    } else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: CommunityPost,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(0.12f))
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header: Avatar, Name, Time, Tag, Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.authorInitials, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(post.author, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(post.timeAgo, fontSize = 12.sp, color = TextMuted)
                }
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(post.tagColor.copy(0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(post.tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = post.tagColor)
                }
                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, "More", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            
            // Content
            Text(post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(post.body, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = TextMuted.copy(0.1f))
            Spacer(Modifier.height(8.dp))

            // Actions (Upvote, Comment, Share)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Upvotes
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(NavyLight.copy(0.5f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onUpvote, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowUpward, "Upvote", tint = if (post.isUpvoted) TealPrimary else TextMuted, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        post.upvotes.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isUpvoted) TealPrimary else if (post.isDownvoted) RedError else TextPrimary
                    )
                    IconButton(onClick = onDownvote, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowDownward, "Downvote", tint = if (post.isDownvoted) RedError else TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                
                // Comments
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).clickable { }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, "Comments", tint = TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(post.commentsCount.toString(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                }
                
                Spacer(Modifier.weight(1f))
                
                // Share
                IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, "Share", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
