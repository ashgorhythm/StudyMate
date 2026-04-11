package com.example.myandroidapp.ui.screens.inbox

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.data.model.ChatMessageEntity
import com.example.myandroidapp.data.model.UserProfileEntity
import com.example.myandroidapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════
// ── Inbox Screen — Dedicated messaging hub ──
// Premium glassmorphism design with conversation list
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val filteredConversations by viewModel.filteredConversations.collectAsState()
    val accent = LocalAccentColor.current

    // If viewing a conversation, show full-screen chat
    state.selectedConversation?.let { profile ->
        InboxChatScreen(
            targetProfile = profile,
            messages = state.chatMessages,
            currentMemberId = "", // Will be pulled from VM
            onSend = { viewModel.sendMessage(it) },
            onBack = { viewModel.closeConversation() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDark, NavyMedium.copy(0.5f), NavyDark)
                )
            )
    ) {
        // ── Top Bar ──
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Inbox",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                    if (state.unreadTotal > 0) {
                        Spacer(Modifier.width(10.dp))
                        Badge(
                            containerColor = accent,
                            contentColor = NavyDark
                        ) {
                            Text(
                                "${state.unreadTotal}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleSearch() }) {
                    Icon(
                        if (state.isSearching) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = TextPrimary
            )
        )

        // ── Search Bar ──
        AnimatedVisibility(
            visible = state.isSearching,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search conversations...", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = TextMuted.copy(0.15f),
                    cursorColor = accent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        // ── Content ──
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Loading messages…", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
            filteredConversations.isEmpty() -> {
                // Empty state
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Animated envelope icon
                        val infiniteTransition = rememberInfiniteTransition(label = "emptyInbox")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "emptyScale"
                        )

                        Box(
                            modifier = Modifier
                                .size((72 * scale).dp)
                                .clip(CircleShape)
                                .background(accent.copy(0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.MailOutline,
                                contentDescription = null,
                                tint = accent.copy(0.6f),
                                modifier = Modifier.size((36 * scale).dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            if (state.isSearching && state.searchQuery.isNotBlank())
                                "No conversations match your search"
                            else
                                "No messages yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.isSearching && state.searchQuery.isNotBlank())
                                "Try a different search term"
                            else
                                "Start chatting with friends from the community!",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        filteredConversations,
                        key = { it.otherUser.memberId }
                    ) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            accent = accent,
                            onClick = { viewModel.openConversation(conversation.otherUser) }
                        )
                    }

                    // Bottom spacer for navigation
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Conversation Card ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ConversationCard(
    conversation: ConversationPreview,
    accent: Color,
    onClick: () -> Unit
) {
    val hasUnread = conversation.unreadCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasUnread) accent.copy(0.04f) else SurfaceCard
        ),
        border = BorderStroke(
            1.dp,
            if (hasUnread) accent.copy(0.15f) else TextMuted.copy(0.06f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    getConversationColor(conversation.otherUser.displayName),
                                    getConversationColor(conversation.otherUser.displayName).copy(0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        conversation.otherUser.displayName.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Conversation details
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        conversation.otherUser.displayName,
                        fontSize = 15.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conversation.otherUser.username.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "@${conversation.otherUser.username}",
                            fontSize = 11.sp,
                            color = accent.copy(0.5f),
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    conversation.lastMessage,
                    fontSize = 13.sp,
                    color = if (hasUnread) TextSecondary else TextMuted,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            // Time + unread badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTimestamp(conversation.lastMessageTime),
                    fontSize = 11.sp,
                    color = if (hasUnread) accent else TextMuted
                )
                if (hasUnread) {
                    Spacer(Modifier.height(6.dp))
                    Badge(
                        containerColor = accent,
                        contentColor = NavyDark
                    ) {
                        Text(
                            "${conversation.unreadCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Full-Screen Chat (for inbox-based navigation) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun InboxChatScreen(
    targetProfile: UserProfileEntity,
    messages: List<ChatMessageEntity>,
    currentMemberId: String,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val accent = LocalAccentColor.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDark, NavyMedium.copy(0.5f))))
    ) {
        // ── Chat Header ──
        Row(
            Modifier
                .fillMaxWidth()
                .background(NavyMedium.copy(0.8f))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                getConversationColor(targetProfile.displayName),
                                getConversationColor(targetProfile.displayName).copy(0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    targetProfile.displayName.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    targetProfile.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (targetProfile.username.isNotBlank()) {
                    Text(
                        "@${targetProfile.username}",
                        fontSize = 12.sp,
                        color = accent.copy(0.6f)
                    )
                } else {
                    Text(
                        "🔥 ${targetProfile.studyStreak}d streak",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // ── Messages ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Start chatting with ${targetProfile.displayName}!",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                // Since we don't get currentMemberId passed through,
                // we infer: if the sender is NOT the target, it's us
                val isMe = msg.senderId != targetProfile.memberId
                ChatBubble(
                    message = msg.message,
                    isMe = isMe,
                    timestamp = msg.sentAt,
                    accent = accent
                )
            }
        }

        // ── Input ──
        Row(
            Modifier
                .fillMaxWidth()
                .background(NavyMedium.copy(0.9f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...", color = TextMuted, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = TextMuted.copy(0.15f),
                    cursorColor = accent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSend(messageText.trim())
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageText.isNotBlank()) accent
                        else accent.copy(0.2f)
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = if (messageText.isNotBlank()) NavyDark else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Chat Bubble ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ChatBubble(
    message: String,
    isMe: Boolean,
    timestamp: Long,
    accent: Color
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) accent.copy(0.2f) else SurfaceCard)
                .border(
                    1.dp,
                    if (isMe) accent.copy(0.3f) else TextMuted.copy(0.1f),
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(message, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    formatTimestamp(timestamp),
                    fontSize = 9.sp,
                    color = TextMuted.copy(0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Helpers ──
// ═══════════════════════════════════════════════════════

private fun formatTimestamp(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(millis))
    }
}

private fun getConversationColor(name: String): Color {
    val colors = listOf(TealPrimary, PurpleAccent, AmberAccent, PinkAccent, GreenSuccess, PurpleLight, TealDark)
    return colors[kotlin.math.abs(name.hashCode()) % colors.size]
}
