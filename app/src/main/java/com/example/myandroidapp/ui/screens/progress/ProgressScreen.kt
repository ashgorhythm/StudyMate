package com.example.myandroidapp.ui.screens.progress

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.ui.screens.dashboard.AddEditTaskDialog
import com.example.myandroidapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Subject Dialogs
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

    // Add Task Dialog
    if (uiState.showAddTaskDialog) {
        AddEditTaskDialog(
            subjects = uiState.subjects,
            onDismiss = { viewModel.dismissAddTaskDialog() },
            onSave = { task -> viewModel.addTask(task) }
        )
    }

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
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "My Progress",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                FilterChip(
                    selected = true,
                    onClick = { /* expand filter */ },
                    label = { Text(uiState.selectedFilter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SurfaceCard,
                        selectedLabelColor = TealPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selectedBorderColor = TealPrimary.copy(alpha = 0.4f),
                        enabled = true,
                        selected = true
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // ── Overall Progress Bar ──
            OverallProgressBar(uiState.overallProgress, uiState.completedTasks, uiState.totalTasks)
            Spacer(modifier = Modifier.height(28.dp))

            // ── Subject Cards ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "By Subject",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(
                    onClick = { viewModel.showAddSubjectDialog() },
                    colors = ButtonDefaults.textButtonColors(contentColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Subject", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.subjects.isEmpty()) {
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
                            Icons.Default.School,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No subjects yet", color = TextPrimary, fontSize = 16.sp)
                        Text("Tap '+ Add Subject' to get started", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                uiState.subjects.forEach { subject ->
                    SubjectProgressCard(
                        subject = subject,
                        onEdit = { viewModel.showEditSubjectDialog(subject) },
                        onDelete = { viewModel.showDeleteConfirmation(subject) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Weekly Heatmap ──
            WeeklyHeatmap()
        }

        // FAB - now opens Add Task dialog
        FloatingActionButton(
            onClick = { viewModel.showAddTaskDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 110.dp),
            containerColor = TealPrimary,
            contentColor = NavyDark,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
private fun OverallProgressBar(progress: Float, completed: Int, total: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Overall Completion", color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = TealPrimary,
                trackColor = NavyLight,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$completed of $total tasks completed",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectProgressCard(
    subject: Subject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(subject.colorHex))
    } catch (e: Exception) {
        TealPrimary
    }

    val progress = if (subject.totalTopics > 0)
        subject.completedTopics.toFloat() / subject.totalTopics else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800),
        label = "subjectProgress"
    )

    var showContextMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdit() },
                onLongClick = { showContextMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(subject.icon, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        subject.name,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        // Context menu
                        Box {
                            IconButton(
                                onClick = { showContextMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    "More options",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false },
                                modifier = Modifier.background(NavyLight)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Edit, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Edit", color = TextPrimary)
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Delete, null, tint = RedError, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Delete", color = RedError)
                                        }
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = NavyLight,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${subject.completedTopics}/${subject.totalTopics} topics",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        "${subject.totalStudyMinutes / 60}h ${subject.totalStudyMinutes % 60}m",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyHeatmap() {
    Text(
        "Study Heatmap",
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val intensities = listOf(0.8f, 0.6f, 1.0f, 0.4f, 0.9f, 0.3f, 0.7f) // sample data

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEachIndexed { idx, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealPrimary.copy(alpha = intensities[idx]))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(day, fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 10.sp, color = TextMuted)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { a ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(TealPrimary.copy(alpha = a))
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("More", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}
