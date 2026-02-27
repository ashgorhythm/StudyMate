package com.example.myandroidapp.ui.screens.aichat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.data.model.ChatMessage
import com.example.myandroidapp.ui.theme.*

@Composable
fun AiChatScreen(viewModel: AiChatViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyDark, Color(0xFF0D1025))))) {
        Column(Modifier.fillMaxSize().padding(top = 48.dp, bottom = 100.dp)) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(PurpleAccent.copy(0.2f)), Alignment.Center) {
                    Icon(Icons.Default.SmartToy, null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Study AI", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(GreenSuccess))
                        Spacer(Modifier.width(4.dp))
                        Text("Online", fontSize = 12.sp, color = GreenSuccess)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Quick actions
            Row(Modifier.padding(horizontal = 20.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Summarize Notes", "Quiz Me", "Study Plan", "Explain Concept").forEach { a ->
                    SuggestionChip({ viewModel.quickAction(a) }, { Text(a, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SurfaceCard, labelColor = TealPrimary),
                        border = SuggestionChipDefaults.suggestionChipBorder(borderColor = TealPrimary.copy(0.3f), enabled = true)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Messages
            LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.messages) { msg -> ChatBubble(msg) }
                if (uiState.isTyping) { item { TypingIndicator() } }
            }

            // Input
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.inputText, onValueChange = { viewModel.updateInput(it) },
                    placeholder = { Text("Ask anything...", color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                        cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton({ viewModel.sendMessage() }, Modifier.size(48.dp), CircleShape, containerColor = TealPrimary, contentColor = NavyDark) {
                    Icon(Icons.Default.Send, "Send", Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.isFromUser
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Card(
            Modifier.widthIn(max = 300.dp),
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
            colors = CardDefaults.cardColors(if (isUser) TealPrimary.copy(0.15f) else SurfaceCard),
            border = BorderStroke(1.dp, if (isUser) TealPrimary.copy(0.3f) else PurpleAccent.copy(0.2f))
        ) {
            Text(msg.content, Modifier.padding(14.dp), color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(PurpleAccent.copy(0.2f)), Alignment.Center) {
            Icon(Icons.Default.SmartToy, null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("AI is thinking...", fontSize = 13.sp, color = TextSecondary)
    }
}
