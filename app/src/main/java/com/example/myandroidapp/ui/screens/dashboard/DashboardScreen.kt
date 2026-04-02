package com.example.myandroidapp.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.toColorInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberAdaptiveInfo()
    var showProfileOverlay by remember { mutableStateOf(false) }


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
            TabletDashboard(uiState, viewModel, adaptive.horizontalPadding, adaptive.progressRingSize,
                onProfileClick = { showProfileOverlay = true })
        } else {
            PhoneDashboard(uiState, viewModel,
                onProfileClick = { showProfileOverlay = true })
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

        // ── Profile Card Overlay ──
        if (showProfileOverlay) {
            DashboardProfileOverlay(
                studentName = uiState.studentName,
                onDismiss = { showProfileOverlay = false },
                onNavigateToSettings = {
                    showProfileOverlay = false
                    onNavigateToSettings()
                },
                onNavigateToProfile = {
                    showProfileOverlay = false
                    onNavigateToProfile()
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Phone Layout (single column scroll) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun PhoneDashboard(uiState: DashboardUiState, viewModel: DashboardViewModel, onProfileClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 100.dp)
    ) {
        GreetingSection(uiState.studentName, onProfileClick = onProfileClick)
        Spacer(Modifier.height(16.dp))
        SummaryRow(uiState)
        Spacer(Modifier.height(24.dp))
        StudyProgressHeroCard(uiState)
        Spacer(Modifier.height(24.dp))
        ThisMonthWeeklyGrid(uiState)
        Spacer(Modifier.height(28.dp))
        OverviewSection(uiState)
        Spacer(Modifier.height(28.dp))
        QuickStatsRow(uiState)
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
        StudyActivityHeatmapSection()
        Spacer(Modifier.height(24.dp))
        TrendSection()
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
    onProfileClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = hPadding)
            .padding(top = 32.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Left Column ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            GreetingSection(uiState.studentName, onProfileClick = onProfileClick)
            Spacer(Modifier.height(16.dp))
            SummaryRow(uiState)
            Spacer(Modifier.height(20.dp))
            StudyProgressHeroCard(uiState)
            Spacer(Modifier.height(20.dp))
            ThisMonthWeeklyGrid(uiState)
            Spacer(Modifier.height(24.dp))
            OverviewSection(uiState)
            Spacer(Modifier.height(24.dp))
            QuickStatsGrid(uiState)
        }

        // ── Right Column ──
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
            Spacer(Modifier.height(24.dp))
            StudyActivityHeatmapSection()
            Spacer(Modifier.height(20.dp))
            TrendSection()
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
// ── Greeting Section ──
// ═══════════════════════════════════════════════════════

@Composable
private fun GreetingSection(name: String, onProfileClick: () -> Unit = {}) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 4 -> "Good late night"
        hour < 8 -> "Good early morning"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("$greeting $name.", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("\"Consistency is the key to mastery\"", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.width(12.dp))
        // ── Animated Profile Avatar ──
        ProfileAvatarButton(
            name = name,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun ProfileAvatarButton(name: String, onClick: () -> Unit) {
    // Scalloped circle animation: slow rotation + subtle pulse
    val infiniteTransition = rememberInfiniteTransition(label = "profileScallop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing)
        ),
        label = "scallopRotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scallopPulse"
    )
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "profileStarRotation"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle rotating star halo behind the avatar for the requested “round star” accent
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = TealPrimary.copy(alpha = 0.22f),
            modifier = Modifier
                .matchParentSize()
                .rotate(starRotation)
        )

        // ── Rotating scalloped circle (smooth wavy ring) ──
        Canvas(
            modifier = Modifier
                .size(52.dp)
                .rotate(rotation)
        ) {
            val cx = size.minDimension / 2f
            val baseRadius = cx * pulse
            val scallops = 12
            val amplitude = cx * 0.13f
            val path = androidx.compose.ui.graphics.Path()
            val totalPoints = scallops * 8

            for (i in 0..totalPoints) {
                val angle = (2.0 * Math.PI * i / totalPoints) - Math.PI / 2
                val wave = amplitude * kotlin.math.sin(scallops * angle).toFloat()
                val r = baseRadius + wave
                val x = cx + r * kotlin.math.cos(angle).toFloat()
                val y = cx + r * kotlin.math.sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        TealPrimary.copy(alpha = 0.25f),
                        PurpleAccent.copy(alpha = 0.2f),
                        TealPrimary.copy(alpha = 0.25f)
                    )
                ),
                style = Stroke(width = size.minDimension * 0.08f)
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Dashboard Profile Overlay ──
// ═══════════════════════════════════════════════════════

@Composable
private fun DashboardProfileOverlay(
    studentName: String,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f))
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top row: X (close) and Settings icon ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, "Settings", tint = TealPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))

                // ── Avatar with scalloped animation ──
                ProfileAvatarButton(
                    name = studentName,
                    onClick = onNavigateToProfile
                )
                Spacer(Modifier.height(16.dp))

                // ── Greeting ──
                Text(
                    "Hi, $studentName!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(20.dp))

                // ── Manage Student Profile Button ──
                Button(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary.copy(alpha = 0.15f),
                        contentColor = TealPrimary
                    ),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Manage Student Profile",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Summary Row ──
// ═══════════════════════════════════════════════════════

@Composable
private fun SummaryRow(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "You have cloud backup, a ${state.streakDays}-day focus streak, and ${state.upcomingDeadlines} upcoming deadlines.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Backup", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${state.streakDays}", fontSize = 13.sp, color = AmberAccent, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = PinkAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${state.upcomingDeadlines}", fontSize = 13.sp, color = PinkAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Study Progress Hero Card ──
// ═══════════════════════════════════════════════════════

@Composable
private fun StudyProgressHeroCard(state: DashboardUiState) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.overallProgress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "heroProgress"
    )
    val focusProgress = if (state.focusHoursTarget > 0f) (state.focusHoursCurrent / state.focusHoursTarget).coerceIn(0f, 1f) else 0f
    val animatedFocusProgress by animateFloatAsState(
        targetValue = focusProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "focusProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AmberCrimsonStart.copy(alpha = 0.15f),
                            AmberCrimsonEnd.copy(alpha = 0.12f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Study Progress", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Current Progress", fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        String.format(Locale.US, "%.1f", animatedProgress * 100),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberAccent
                    )
                    Text(
                        "%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("complete", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎓", fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Focus Hours", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        "${state.focusHoursCurrent.toInt()}/${state.focusHoursTarget.toInt()} hrs",
                        fontSize = 12.sp,
                        color = AmberAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedFocusProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AmberAccent,
                    trackColor = NavyLight.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── This Month Weekly Grid ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ThisMonthWeeklyGrid(state: DashboardUiState) {
    Text("This month", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Weekly Tasks card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Weekly Tasks", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = GreenSuccess, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.weeklyTasksCompleted}/${state.weeklyTasksTotal} tasks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenSuccess
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Compared to last month", fontSize = 10.sp, color = TextMuted)
            }
        }
        // Weekly Focus card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, RedError.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Weekly Focus", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = RedError, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.weeklyFocusHours} hours",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedError
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Compared to last month", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Overview Section ──
// ═══════════════════════════════════════════════════════

@Composable
private fun OverviewSection(state: DashboardUiState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Overview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Icon(Icons.Default.GridView, null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Study Plan card
        Card(
            modifier = Modifier.weight(1f).height(140.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.2f))
        ) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Study Plan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(
                    Modifier.fillMaxWidth().height(40.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    listOf(0.3f, 0.6f, 0.4f, 0.8f, 0.5f).forEach { h ->
                        Box(
                            Modifier
                                .width(8.dp)
                                .fillMaxHeight(h)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(PurpleAccent.copy(alpha = 0.4f))
                        )
                    }
                }
                Text(
                    if (state.studyPlanSet) "Active" else "No plan set",
                    fontSize = 11.sp,
                    color = if (state.studyPlanSet) GreenSuccess else TextMuted
                )
            }
        }
        // Deadlines card
        Card(
            modifier = Modifier.weight(1f).height(140.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, PinkAccent.copy(alpha = 0.2f))
        ) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Deadlines", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Column {
                    Text("${state.pendingExams} Exams", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text("${state.pendingAssignments} Assignments", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    val totalPending = state.pendingExams + state.pendingAssignments
                    Text(
                        "$totalPending Total Pending",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonAccent
                    )
                }
                Text("Deadline Count", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Study Activity Heatmap Section ──
// ═══════════════════════════════════════════════════════

@Composable
private fun StudyActivityHeatmapSection() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Study Activity Heatmap", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        IconButton(onClick = { /* TODO: expand heatmap */ }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "More", tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
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

// ═══════════════════════════════════════════════════════
// ── Trend Section ──
// ═══════════════════════════════════════════════════════

@Composable
private fun TrendSection() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Trend", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        IconButton(onClick = { /* TODO: expand trend */ }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "More", tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
    Spacer(Modifier.height(8.dp))
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp),
        CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().height(60.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(0.3f, 0.5f, 0.4f, 0.7f, 0.6f, 0.8f, 0.9f).forEach { h ->
                    Box(
                        Modifier
                            .width(14.dp)
                            .fillMaxHeight(h)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(TealPrimary, PurpleAccent.copy(alpha = 0.6f))
                                )
                            )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Week 1", fontSize = 10.sp, color = TextMuted)
                Text("Week 4", fontSize = 10.sp, color = TextMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Study hours trending upward this month",
                fontSize = 12.sp,
                color = GreenSuccess,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Shared Components ──
// ═══════════════════════════════════════════════════════

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
    @Suppress("DEPRECATION")
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
    val color = try { Color(subject.colorHex.toColorInt()) } catch (e: Exception) { TealPrimary }
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

