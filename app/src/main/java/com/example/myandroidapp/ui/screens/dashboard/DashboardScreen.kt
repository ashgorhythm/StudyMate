package com.example.myandroidapp.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 100.dp)
        ) {
            // ── Greeting Section ──
            GreetingSection(uiState.studentName)
            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Stats ──
            QuickStatsRow(uiState)
            Spacer(modifier = Modifier.height(28.dp))

            // ── Progress Ring ──
            ProgressRingSection(uiState.overallProgress)
            Spacer(modifier = Modifier.height(28.dp))

            // ── Urgent Tasks ──
            UrgentTasksSection(
                tasks = uiState.urgentTasks,
                onToggle = { id, done -> viewModel.toggleTaskCompletion(id, done) }
            )

            // ── Load Sample Data Button ──
            if (uiState.totalTasks == 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.addSampleData() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = NavyDark
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Sample Data", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GreetingSection(name: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column {
        Text(
            text = "$greeting, $name! 👋",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "\"Consistency is the key to mastery\"",
            fontSize = 14.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun QuickStatsRow(state: DashboardUiState) {
    data class Stat(val label: String, val value: String, val color: Color, val icon: @Composable () -> Unit)

    val stats = listOf(
        Stat("Topics\nLearned", "${state.completedTasks}", TealPrimary) {
            Icon(Icons.Default.CheckCircle, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
        },
        Stat("Pending\nTopics", "${state.totalTasks - state.completedTasks}", AmberAccent) {
            Icon(Icons.Default.Schedule, null, tint = AmberAccent, modifier = Modifier.size(20.dp))
        },
        Stat("Study\nHours", "${state.totalStudyMinutes / 60}h", PurpleAccent) {
            Icon(Icons.Default.Timer, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
        },
        Stat("Day\nStreak", "${state.streakDays}🔥", PinkAccent) {
            Icon(Icons.Default.LocalFireDepartment, null, tint = PinkAccent, modifier = Modifier.size(20.dp))
        }
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(stats) { stat ->
            Card(
                modifier = Modifier
                    .width(130.dp)
                    .height(100.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceCard
                ),
                border = BorderStroke(1.dp, stat.color.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        stat.icon()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stat.value,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = stat.color
                        )
                    }
                    Text(
                        stat.label,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRingSection(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Overall Progress",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                // Background ring
                drawArc(
                    color = NavyLight,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
                // Progress ring
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(TealPrimary, PurpleAccent, TealPrimary)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
                Text(
                    "Complete",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun UrgentTasksSection(
    tasks: List<com.example.myandroidapp.data.model.StudyTask>,
    onToggle: (Long, Boolean) -> Unit
) {
    Text(
        "Urgent To-Dos",
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (tasks.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = GreenSuccess,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("All caught up! 🎉", color = TextPrimary, fontSize = 16.sp)
                Text("No urgent tasks", color = TextSecondary, fontSize = 13.sp)
            }
        }
    } else {
        tasks.forEach { task ->
            TaskCard(task = task, onToggle = onToggle)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TaskCard(
    task: com.example.myandroidapp.data.model.StudyTask,
    onToggle: (Long, Boolean) -> Unit
) {
    val subjectColor = when (task.subject.lowercase()) {
        "mathematics" -> TealPrimary
        "physics" -> PurpleAccent
        "english" -> AmberAccent
        "history" -> PinkAccent
        else -> TextSecondary
    }

    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, subjectColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle(task.id, !task.isCompleted) },
                colors = CheckboxDefaults.colors(
                    checkedColor = TealPrimary,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = NavyDark
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(subjectColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            task.subject,
                            fontSize = 11.sp,
                            color = subjectColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        dateFormat.format(Date(task.dueDate)),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
            // Priority indicator
            val priorityColor = when (task.priority) {
                2 -> RedError
                1 -> AmberAccent
                else -> GreenSuccess
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
        }
    }
}
