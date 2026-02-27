package com.example.myandroidapp.ui.screens.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.ui.theme.*

@Composable
fun FocusScreen(viewModel: FocusViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDark, Color(0xFF050714))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Focus Mode Banner ──
            FocusModeBanner(uiState.isRunning)
            Spacer(modifier = Modifier.height(12.dp))

            // ── Current Subject ──
            Text(
                "Studying: ${uiState.currentSubject}",
                fontSize = 16.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(32.dp))

            // ── Timer Ring ──
            TimerRing(
                seconds = uiState.timerSeconds,
                totalSeconds = uiState.totalSeconds,
                isRunning = uiState.isRunning
            )
            Spacer(modifier = Modifier.height(32.dp))

            // ── Controls ──
            TimerControls(
                isRunning = uiState.isRunning,
                onToggle = { viewModel.toggleTimer() },
                onReset = { viewModel.resetTimer() }
            )
            Spacer(modifier = Modifier.height(28.dp))

            // ── Duration Chips ──
            DurationSelector(
                selected = uiState.selectedDuration,
                onSelect = { viewModel.setDuration(it) },
                isRunning = uiState.isRunning
            )
            Spacer(modifier = Modifier.height(28.dp))

            // ── Ambient Sounds ──
            AmbientSoundSelector(
                selected = uiState.selectedAmbientSound,
                onSelect = { viewModel.setAmbientSound(it) }
            )
            Spacer(modifier = Modifier.height(28.dp))

            // ── Today's Stats ──
            TodayStats(
                sessions = uiState.todaySessionCount,
                minutes = uiState.todayTotalMinutes
            )
        }
    }
}

@Composable
private fun FocusModeBanner(isActive: Boolean) {
    val bgColor = if (isActive) TealPrimary.copy(alpha = 0.15f) else SurfaceCard
    val borderColor = if (isActive) TealPrimary.copy(alpha = 0.5f) else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (isActive) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (isActive) TealPrimary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isActive) "Focus Mode Active" else "Focus Mode",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) TealPrimary else TextPrimary
            )
        }
    }
}

@Composable
private fun TimerRing(seconds: Int, totalSeconds: Int, isRunning: Boolean) {
    val progress = if (totalSeconds > 0) seconds.toFloat() / totalSeconds else 1f
    val minutes = seconds / 60
    val secs = seconds % 60

    // Glow pulse animation when running
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isRunning) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            // Outer glow
            drawCircle(
                color = TealPrimary.copy(alpha = glowAlpha * 0.1f),
                radius = size.minDimension / 2
            )
            // Background ring
            drawArc(
                color = NavyLight,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
            // Progress ring
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(TealGlow, TealPrimary, TealGlow)
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                String.format("%02d:%02d", minutes, secs),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
                letterSpacing = 4.sp
            )
            if (isRunning) {
                Text(
                    "remaining",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TimerControls(isRunning: Boolean, onToggle: () -> Unit, onReset: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .height(52.dp)
                .width(120.dp),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, TextMuted),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset", fontWeight = FontWeight.Medium)
        }

        // Start/Pause button
        Button(
            onClick = onToggle,
            modifier = Modifier
                .height(52.dp)
                .width(160.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) AmberAccent else TealPrimary,
                contentColor = NavyDark
            )
        ) {
            Icon(
                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isRunning) "Pause" else "Start",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun DurationSelector(selected: Int, onSelect: (Int) -> Unit, isRunning: Boolean) {
    val durations = listOf(25, 30, 45, 60)

    Column {
        Text(
            "Session Duration",
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            durations.forEach { min ->
                val isSelected = selected == min
                FilterChip(
                    selected = isSelected,
                    onClick = { if (!isRunning) onSelect(min) },
                    label = { Text("${min}m", fontWeight = FontWeight.Medium) },
                    enabled = !isRunning,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = TealPrimary,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selectedBorderColor = TealPrimary,
                        borderColor = Color.Transparent,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}

@Composable
private fun AmbientSoundSelector(selected: String, onSelect: (String) -> Unit) {
    data class Sound(val name: String, val icon: String)

    val sounds = listOf(
        Sound("Rain", "🌧️"),
        Sound("Forest", "🌲"),
        Sound("Lo-fi", "🎵"),
        Sound("Silence", "🔇")
    )

    Column {
        Text(
            "Ambient Sound",
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            sounds.forEach { sound ->
                val isSelected = selected == sound.name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(sound.name) }
                        .background(
                            if (isSelected) TealPrimary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) TealPrimary.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(sound.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        sound.name,
                        fontSize = 11.sp,
                        color = if (isSelected) TealPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayStats(sessions: Int, minutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$sessions",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                Text("Sessions Today", fontSize = 12.sp, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(50.dp)
                    .background(TextMuted.copy(alpha = 0.3f))
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${minutes / 60}h ${minutes % 60}m",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
                Text("Total Focus Time", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
