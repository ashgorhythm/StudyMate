package com.example.myandroidapp.ui.screens.aichat

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.data.model.ChatMessage
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AiChatScreen(viewModel: AiChatViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    val adaptive = rememberAdaptiveInfo()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDark, Color(0xFF0D1025)))),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .then(
                    if (adaptive.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified)
                        Modifier.widthIn(max = adaptive.maxContentWidth)
                    else Modifier
                )
                .fillMaxSize()
                .padding(top = if (adaptive.isTablet) 24.dp else 48.dp, bottom = if (adaptive.isTablet) 16.dp else 100.dp)
        ) {
            // ── Header ──
            ChatHeader(
                currentMode = uiState.currentMode,
                isTyping = uiState.isTyping,
                onClearChat = { viewModel.clearChat() }
            )
            Spacer(Modifier.height(8.dp))

            // ── Mode Selector Chips ──
            ModeSelector(
                currentMode = uiState.currentMode,
                onModeSelected = { viewModel.setMode(it) }
            )
            Spacer(Modifier.height(8.dp))

            // ── Messages ──
            LazyColumn(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(msg)
                }
                if (uiState.isTyping) {
                    item { TypingIndicator() }
                }
            }

            // ── Input Bar ──
            ChatInputBar(
                inputText = uiState.inputText,
                isTyping = uiState.isTyping,
                currentMode = uiState.currentMode,
                onInputChange = { viewModel.updateInput(it) },
                onSend = { viewModel.sendMessage() }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Header ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ChatHeader(
    currentMode: AiMode,
    isTyping: Boolean,
    onClearChat: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI Avatar with glow when active
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(PurpleAccent.copy(0.3f), TealPrimary.copy(0.3f))
                    )
                ),
            Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Study AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTyping) {
                    // Pulsing dot
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                        label = "pulseAlpha"
                    )
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = alpha))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Thinking...", fontSize = 12.sp, color = AmberAccent)
                } else {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(GreenSuccess)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Powered by Gemini • ${currentMode.emoji} ${currentMode.label}",
                        fontSize = 12.sp,
                        color = GreenSuccess
                    )
                }
            }
        }
        // Clear chat button
        IconButton(onClick = onClearChat) {
            Icon(
                Icons.Outlined.DeleteSweep,
                "Clear Chat",
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Mode Selector ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ModeSelector(currentMode: AiMode, onModeSelected: (AiMode) -> Unit) {
    data class ModeItem(val mode: AiMode, val icon: ImageVector)

    val modes = listOf(
        ModeItem(AiMode.CHAT, Icons.Default.ChatBubble),
        ModeItem(AiMode.SUMMARIZE, Icons.Default.Summarize),
        ModeItem(AiMode.QUIZ, Icons.Default.Quiz),
        ModeItem(AiMode.STUDY_PLAN, Icons.Default.CalendarMonth),
        ModeItem(AiMode.EXPLAIN, Icons.Default.Lightbulb)
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modes.forEach { item ->
            val isSelected = currentMode == item.mode
            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(item.mode) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(item.mode.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (item.mode) {
                        AiMode.CHAT -> TealPrimary.copy(0.2f)
                        AiMode.SUMMARIZE -> PurpleAccent.copy(0.2f)
                        AiMode.QUIZ -> AmberAccent.copy(0.2f)
                        AiMode.STUDY_PLAN -> GreenSuccess.copy(0.2f)
                        AiMode.EXPLAIN -> PinkAccent.copy(0.2f)
                    },
                    selectedLabelColor = when (item.mode) {
                        AiMode.CHAT -> TealPrimary
                        AiMode.SUMMARIZE -> PurpleAccent
                        AiMode.QUIZ -> AmberAccent
                        AiMode.STUDY_PLAN -> GreenSuccess
                        AiMode.EXPLAIN -> PinkAccent
                    },
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = when (item.mode) {
                        AiMode.CHAT -> TealPrimary.copy(0.5f)
                        AiMode.SUMMARIZE -> PurpleAccent.copy(0.5f)
                        AiMode.QUIZ -> AmberAccent.copy(0.5f)
                        AiMode.STUDY_PLAN -> GreenSuccess.copy(0.5f)
                        AiMode.EXPLAIN -> PinkAccent.copy(0.5f)
                    },
                    borderColor = Color.Transparent,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Chat Bubble ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.isFromUser
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PurpleAccent.copy(0.15f)),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SmartToy, null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("Study AI", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
        }

        Card(
            Modifier.widthIn(max = 320.dp),
            RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                if (isUser) TealPrimary.copy(0.12f) else SurfaceCard
            ),
            border = BorderStroke(
                1.dp,
                if (isUser) TealPrimary.copy(0.25f) else PurpleAccent.copy(0.15f)
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    msg.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    timeFormat.format(Date(msg.timestamp)),
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Typing Indicator ──
// ═══════════════════════════════════════════════════════

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(PurpleAccent.copy(0.2f)),
            Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, PurpleAccent.copy(0.1f))
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                repeat(3) { idx ->
                    val delay = idx * 200
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = delay),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$idx"
                    )
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent.copy(alpha = alpha))
                    )
                    if (idx < 2) Spacer(Modifier.width(5.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Input Bar ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ChatInputBar(
    inputText: String,
    isTyping: Boolean,
    currentMode: AiMode,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val placeholderText = when (currentMode) {
        AiMode.CHAT -> "Ask anything..."
        AiMode.SUMMARIZE -> "Paste notes to summarize..."
        AiMode.QUIZ -> "Enter a topic for quiz..."
        AiMode.STUDY_PLAN -> "Subject & exam details..."
        AiMode.EXPLAIN -> "What concept to explain?"
    }

    val accentColor = when (currentMode) {
        AiMode.CHAT -> TealPrimary
        AiMode.SUMMARIZE -> PurpleAccent
        AiMode.QUIZ -> AmberAccent
        AiMode.STUDY_PLAN -> GreenSuccess
        AiMode.EXPLAIN -> PinkAccent
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            placeholder = { Text(placeholderText, color = TextMuted, fontSize = 14.sp) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = TextMuted.copy(0.3f),
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                cursorColor = accentColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onSend,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            containerColor = if (inputText.isNotBlank() && !isTyping) accentColor else TextMuted.copy(0.3f),
            contentColor = if (inputText.isNotBlank() && !isTyping) NavyDark else TextMuted
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "Send", Modifier.size(22.dp))
        }
    }
}
