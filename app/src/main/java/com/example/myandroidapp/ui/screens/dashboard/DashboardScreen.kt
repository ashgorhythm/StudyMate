package com.example.myandroidapp.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.ui.screens.progress.AddEditSubjectDialog
import com.example.myandroidapp.ui.screens.progress.DeleteConfirmationDialog
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.WindowWidthSize
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToSettings: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberAdaptiveInfo()

    // ── Task Dialog ──
    if (uiState.showAddTaskDialog) {
        AddEditTaskDialog(
            existingTask = uiState.editingTask,
            subjects = uiState.subjects,
            onDismiss = { viewModel.dismissTaskDialog() },
            onSave = { task -> viewModel.saveTask(task) }
        )
    }

    // ── Subject Dialogs ──
    if (uiState.showAddSubjectDialog) {
        AddEditSubjectDialog(
            existingSubject = uiState.editingSubject,
            onDismiss = { viewModel.dismissSubjectDialog() },
            onSave = { subject -> viewModel.saveSubject(subject) }
        )
    }

    if (uiState.showDeleteConfirmation && uiState.subjectToDelete != null) {
        DeleteConfirmationDialog(
            subjectName = uiState.subjectToDelete!!.name,
            onDismiss = { viewModel.dismissDeleteConfirmation() },
            onConfirm = { viewModel.deleteSubject(uiState.subjectToDelete!!) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
    ) {
        if (adaptive.isTablet) {
            // ── TABLET LAYOUT: Two-column ──
            TabletDashboard(uiState, viewModel, adaptive.horizontalPadding, adaptive.progressRingSize, onNavigateToSettings)
        } else {
            // ── PHONE LAYOUT: Single column scroll ──
            PhoneDashboard(uiState, viewModel, onNavigateToSettings)
        }

        // ── FAB ──
        FloatingActionButton(
            onClick = { viewModel.showAddTaskDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = adaptive.horizontalPadding, bottom = if (adaptive.isTablet) 24.dp else 11.dp),
            containerColor = TealPrimary,
            contentColor = NavyDark,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Phone Layout (single column scroll) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun PhoneDashboard(uiState: DashboardUiState, viewModel: DashboardViewModel, onNavigateToSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 100.dp)
    ) {
        GreetingSection(uiState.studentName, onNavigateToSettings)
        Spacer(Modifier.height(24.dp))
        QuickStatsRow(uiState)
        Spacer(Modifier.height(28.dp))
        ProgressRingSection(uiState.overallProgress, 180.dp)
        Spacer(Modifier.height(28.dp))
        UrgentTasksSection(uiState.urgentTasks,
            onToggle = { id, done -> viewModel.toggleTaskCompletion(id, done) },
            onEdit = { viewModel.showEditTaskDialog(it) },
            onDelete = { viewModel.deleteTask(it) }
        )
        Spacer(Modifier.height(28.dp))
        SubjectProgressSection(uiState.subjects,
            onAddSubject = { viewModel.showAddSubjectDialog() },
            onEditSubject = { viewModel.showEditSubjectDialog(it) },
            onDeleteSubject = { viewModel.showDeleteConfirmation(it) }
        )
        Spacer(Modifier.height(28.dp))
        WeeklyHeatmap()
    }
}

// ═══════════════════════════════════════════════════════
// ── Tablet Layout (two-column) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun TabletDashboard(
    uiState: DashboardUiState,
    viewModel: DashboardViewModel,
    hPadding: androidx.compose.ui.unit.Dp,
    ringSize: androidx.compose.ui.unit.Dp,
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = hPadding)
            .padding(top = 32.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Left Column: Greeting, Stats, Progress, Heatmap ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            GreetingSection(uiState.studentName, onNavigateToSettings)
            Spacer(Modifier.height(20.dp))
            QuickStatsGrid(uiState)
            Spacer(Modifier.height(24.dp))
            ProgressRingSection(uiState.overallProgress, ringSize)
            Spacer(Modifier.height(24.dp))
            WeeklyHeatmap()
        }

        // ── Right Column: Tasks, Subjects ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            UrgentTasksSection(uiState.urgentTasks,
                onToggle = { id, done -> viewModel.toggleTaskCompletion(id, done) },
                onEdit = { viewModel.showEditTaskDialog(it) },
                onDelete = { viewModel.deleteTask(it) }
            )
            Spacer(Modifier.height(24.dp))
            SubjectProgressSection(uiState.subjects,
                onAddSubject = { viewModel.showAddSubjectDialog() },
                onEditSubject = { viewModel.showEditSubjectDialog(it) },
                onDeleteSubject = { viewModel.showDeleteConfirmation(it) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Quick Stats (Tablet Grid) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun QuickStatsGrid(state: DashboardUiState) {
    val stats = getStats(state)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { stat ->
                    StatCard(stat, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Shared Components ──
// ═══════════════════════════════════════════════════════

@Composable
private fun GreetingSection(name: String, onNavigateToSettings: () -> Unit = {}) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("$greeting, $name! 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("\"Consistency is the key to mastery\"", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Light)
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Default.Settings, "Settings", tint = TextSecondary)
        }
    }
}

// Stat data class
private data class StatItem(val label: String, val value: String, val color: Color, val icon: @Composable () -> Unit)

private fun getStats(state: DashboardUiState): List<StatItem> = listOf(
    StatItem("Topics\nLearned", "${state.completedTasks}", TealPrimary) {
        Icon(Icons.Default.CheckCircle, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
    },
    StatItem("Pending\nTopics", "${state.totalTasks - state.completedTasks}", AmberAccent) {
        Icon(Icons.Default.Schedule, null, tint = AmberAccent, modifier = Modifier.size(20.dp))
    },
    StatItem("Study\nHours", "${state.totalStudyMinutes / 60}h", PurpleAccent) {
        Icon(Icons.Default.Timer, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
    },
    StatItem("Day\nStreak", "${state.streakDays}🔥", PinkAccent) {
        Icon(Icons.Default.LocalFireDepartment, null, tint = PinkAccent, modifier = Modifier.size(20.dp))
    }
)

@Composable
private fun StatCard(stat: StatItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, stat.color.copy(alpha = 0.3f))
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                stat.icon()
                Spacer(Modifier.width(6.dp))
                Text(stat.value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = stat.color)
            }
            Text(stat.label, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun QuickStatsRow(state: DashboardUiState) {
    val stats = getStats(state)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stats) { stat -> StatCard(stat, Modifier.width(130.dp)) }
    }
}

@Composable
private fun ProgressRingSection(progress: Float, ringSize: androidx.compose.ui.unit.Dp) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Overall Progress", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
            Canvas(modifier = Modifier.size(ringSize)) {
                drawArc(color = NavyLight, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 14f, cap = StrokeCap.Round))
                drawArc(brush = Brush.sweepGradient(listOf(TealPrimary, PurpleAccent, TealPrimary)), startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false, style = Stroke(width = 14f, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(animatedProgress * 100).toInt()}%", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                Text("Complete", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun UrgentTasksSection(tasks: List<StudyTask>, onToggle: (Long, Boolean) -> Unit, onEdit: (StudyTask) -> Unit, onDelete: (StudyTask) -> Unit) {
    Text("Urgent To-Dos", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    if (tasks.isEmpty()) {
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard)) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TaskAlt, null, tint = GreenSuccess, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("All caught up! 🎉", color = TextPrimary, fontSize = 16.sp)
                Text("No urgent tasks", color = TextSecondary, fontSize = 13.sp)
            }
        }
    } else {
        tasks.forEach { task ->
            TaskCard(task, onToggle, onEdit = { onEdit(task) }, onDelete = { onDelete(task) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(task: StudyTask, onToggle: (Long, Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val subjectColor = when (task.subject.lowercase()) {
        "mathematics" -> TealPrimary; "physics" -> PurpleAccent; "english" -> AmberAccent; "history" -> PinkAccent; else -> TextSecondary
    }
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })

    SwipeToDismissBox(state = dismissState, backgroundContent = {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(RedError.copy(0.3f)).padding(horizontal = 20.dp), Alignment.CenterEnd) {
            Icon(Icons.Default.Delete, "Delete", tint = RedError, modifier = Modifier.size(24.dp))
        }
    }, enableDismissFromStartToEnd = false) {
        Card(Modifier.fillMaxWidth().clickable { onEdit() }, RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard), border = BorderStroke(1.dp, subjectColor.copy(0.2f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(task.isCompleted, { onToggle(task.id, !task.isCompleted) }, colors = CheckboxDefaults.colors(TealPrimary, TextSecondary, NavyDark))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (task.isCompleted) TextMuted else TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(subjectColor.copy(0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(task.subject, fontSize = 11.sp, color = subjectColor, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(dateFormat.format(Date(task.dueDate)), fontSize = 11.sp, color = TextMuted)
                    }
                }
                val pc = when (task.priority) { 2 -> RedError; 1 -> AmberAccent; else -> GreenSuccess }
                Box(Modifier.size(8.dp).clip(CircleShape).background(pc))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Subject Progress ──
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectProgressSection(subjects: List<Subject>, onAddSubject: () -> Unit, onEditSubject: (Subject) -> Unit, onDeleteSubject: (Subject) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("My Subjects", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        TextButton(onClick = onAddSubject, colors = ButtonDefaults.textButtonColors(contentColor = TealPrimary)) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(10.dp))
    if (subjects.isEmpty()) {
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard)) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.School, null, tint = PurpleAccent, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("No subjects yet", color = TextPrimary, fontSize = 16.sp)
                Text("Tap '+ Add' to get started", color = TextSecondary, fontSize = 13.sp)
            }
        }
    } else {
        subjects.forEach { subject ->
            SubjectCard(subject, onEdit = { onEditSubject(subject) }, onDelete = { onDeleteSubject(subject) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectCard(subject: Subject, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor(subject.colorHex)) } catch (e: Exception) { TealPrimary }
    val progress = if (subject.totalTopics > 0) subject.completedTopics.toFloat() / subject.totalTopics else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "sp")
    var showMenu by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth().combinedClickable(onClick = onEdit, onLongClick = { showMenu = true }),
        RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard), border = BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(0.15f)), Alignment.Center) { Text(subject.icon, fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(subject.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${(animatedProgress * 100).toInt()}%", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box {
                            IconButton({ showMenu = true }, Modifier.size(28.dp)) { Icon(Icons.Default.MoreVert, "More", tint = TextMuted, modifier = Modifier.size(18.dp)) }
                            DropdownMenu(showMenu, { showMenu = false }, Modifier.background(NavyLight)) {
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Edit, null, tint = TealPrimary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Edit", color = TextPrimary) } }, onClick = { showMenu = false; onEdit() })
                                DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Delete, null, tint = RedError, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Delete", color = RedError) } }, onClick = { showMenu = false; onDelete() })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { animatedProgress }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = color, trackColor = NavyLight)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("${subject.completedTopics}/${subject.totalTopics} topics", fontSize = 11.sp, color = TextSecondary)
                    Text("${subject.totalStudyMinutes / 60}h ${subject.totalStudyMinutes % 60}m", fontSize = 11.sp, color = TextMuted)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Weekly Heatmap ──
// ═══════════════════════════════════════════════════════

@Composable
private fun WeeklyHeatmap() {
    Text("Study Heatmap", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard)) {
        Column(Modifier.padding(16.dp)) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val intensities = listOf(0.8f, 0.6f, 1.0f, 0.4f, 0.9f, 0.3f, 0.7f)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                days.forEachIndexed { idx, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(TealPrimary.copy(intensities[idx])))
                        Spacer(Modifier.height(4.dp))
                        Text(day, fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                Text("Less", fontSize = 10.sp, color = TextMuted)
                Spacer(Modifier.width(4.dp))
                listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { a -> Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(TealPrimary.copy(a))); Spacer(Modifier.width(2.dp)) }
                Spacer(Modifier.width(4.dp))
                Text("More", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}
