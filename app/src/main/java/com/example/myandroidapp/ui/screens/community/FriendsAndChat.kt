package com.example.myandroidapp.ui.screens.community

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
import com.example.myandroidapp.data.model.*
import com.example.myandroidapp.ui.theme.*

// ─────────────────────────────────────────────────────────
// Friends Tab
// ─────────────────────────────────────────────────────────

@Composable
fun FriendsTab(
    friends: List<UserProfileEntity>,
    incomingRequests: List<Pair<FriendRequestEntity, UserProfileEntity?>>,
    onAccept: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onChat: (UserProfileEntity) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Incoming requests section
        if (incomingRequests.isNotEmpty()) {
            Text("Friend Requests", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
            Spacer(Modifier.height(10.dp))
            incomingRequests.forEach { (req, profile) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(AmberAccent.copy(0.06f)),
                    border = BorderStroke(1.dp, AmberAccent.copy(0.15f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AmberAccent, PinkAccent.copy(0.6f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (profile?.displayName ?: "?").take(1).uppercase(),
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile?.displayName ?: "Unknown", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            if (profile != null && profile.username.isNotBlank()) {
                                Text("@${profile.username}", fontSize = 11.sp, color = TealPrimary.copy(0.6f))
                            } else {
                                Text("Wants to connect", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        IconButton(onClick = { onAccept(req.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Check, null, tint = GreenSuccess)
                        }
                        IconButton(onClick = { onReject(req.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, null, tint = RedError)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Friends list
        Text("Friends (${friends.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))

        if (friends.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👋", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No friends yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("Send friend requests from community posts", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        friends.forEach { friend ->
            FriendCard(friend = friend, onChat = { onChat(friend) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FriendCard(friend: UserProfileEntity, onChat: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(0.1f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent.copy(0.7f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(friend.displayName.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (friend.username.isNotBlank()) {
                    Text("@${friend.username}", fontSize = 11.sp, color = TealPrimary.copy(0.6f))
                }
                if (friend.bio.isNotBlank()) {
                    Text(friend.bio, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥 ${friend.studyStreak}d", fontSize = 10.sp, color = AmberAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("📚 ${friend.totalStudyHours}h", fontSize = 10.sp, color = TealPrimary)
                }
            }
            Button(
                onClick = onChat,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(TealPrimary.copy(0.15f), TealPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Chat Screen (overlay)
// ─────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    targetProfile: UserProfileEntity,
    messages: List<ChatMessageEntity>,
    currentMemberId: String,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDark, NavyMedium.copy(0.5f))))
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .background(NavyMedium.copy(0.8f))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) }
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent))),
                contentAlignment = Alignment.Center
            ) { Text(targetProfile.displayName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(targetProfile.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("🔥 ${targetProfile.studyStreak}d streak", fontSize = 11.sp, color = TextMuted)
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Start a conversation!", fontSize = 14.sp, color = TextMuted)
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentMemberId
                MessageBubble(message = msg.message, isMe = isMe)
            }
        }

        // Input
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
                    focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.15f),
                    cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
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
                    .background(if (messageText.isNotBlank()) TealPrimary else TealPrimary.copy(0.2f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, "Send",
                    tint = if (messageText.isNotBlank()) NavyDark else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: String, isMe: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) TealPrimary.copy(0.2f) else SurfaceCard)
                .border(
                    1.dp,
                    if (isMe) TealPrimary.copy(0.3f) else TextMuted.copy(0.1f),
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
        }
    }
}
