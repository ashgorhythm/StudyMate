package com.example.myandroidapp.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myandroidapp.data.model.StudyTask
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    existingTask: StudyTask? = null,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (StudyTask) -> Unit
) {
    val isEditing = existingTask != null

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var selectedSubject by remember { mutableStateOf(existingTask?.subject ?: (subjects.firstOrNull()?.name ?: "")) }
    var priority by remember { mutableIntStateOf(existingTask?.priority ?: 0) }
    var dueDate by remember { mutableLongStateOf(existingTask?.dueDate ?: (System.currentTimeMillis() + 86400000)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSubjectDropdown by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate
    )

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            // Preserve time component, update date
                            val cal = Calendar.getInstance().apply { timeInMillis = dueDate }
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            val minute = cal.get(Calendar.MINUTE)
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                            }
                            dueDate = newCal.timeInMillis
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TealPrimary)
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) { Text("Cancel") }
            },
            colors = DatePickerDefaults.colors(
                containerColor = NavyMedium
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = NavyMedium,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = TealPrimary,
                    selectedYearContainerColor = TealPrimary,
                    selectedYearContentColor = NavyDark,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = TealPrimary,
                    selectedDayContentColor = NavyDark,
                    todayContentColor = TealPrimary,
                    todayDateBorderColor = TealPrimary,
                    navigationContentColor = TextPrimary,
                    subheadContentColor = TextSecondary
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEditing) "Edit Task" else "New Task",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ── Title Field ──
                Text("Title", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Complete Ch.5 Problems", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = TealPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                // ── Description Field ──
                Text("Description (optional)", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Add details...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = TealPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 4
                )
                Spacer(Modifier.height(16.dp))

                // ── Subject Selector ──
                Text("Subject", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSubjectDropdown = true },
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                "Select Subject",
                                tint = TextSecondary,
                                modifier = Modifier.clickable { showSubjectDropdown = true }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = TextMuted.copy(0.3f),
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            cursorColor = TealPrimary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = TextMuted.copy(0.3f),
                            disabledContainerColor = SurfaceCard
                        ),
                        singleLine = true,
                        enabled = false
                    )
                    DropdownMenu(
                        expanded = showSubjectDropdown,
                        onDismissRequest = { showSubjectDropdown = false },
                        modifier = Modifier.background(NavyLight)
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(subject.icon, fontSize = 18.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(subject.name, color = TextPrimary)
                                    }
                                },
                                onClick = {
                                    selectedSubject = subject.name
                                    showSubjectDropdown = false
                                }
                            )
                        }
                        if (subjects.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No subjects yet. Add one first.", color = TextMuted) },
                                onClick = { showSubjectDropdown = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── Due Date ──
                Text("Due Date", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, TextMuted.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            "Pick Date",
                            tint = TealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                dateFormat.format(Date(dueDate)),
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                timeFormat.format(Date(dueDate)),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Default.Edit,
                            "Edit Date",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── Priority Selector ──
                Text("Priority", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    data class PriorityOption(val level: Int, val label: String, val color: Color, val icon: @Composable () -> Unit)

                    val options = listOf(
                        PriorityOption(0, "Low", GreenSuccess) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                        },
                        PriorityOption(1, "Medium", AmberAccent) {
                            Icon(Icons.Default.Remove, null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                        },
                        PriorityOption(2, "High", RedError) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = RedError, modifier = Modifier.size(18.dp))
                        }
                    )

                    options.forEach { opt ->
                        val isSelected = priority == opt.level
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = opt.level },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    opt.icon()
                                    Spacer(Modifier.width(4.dp))
                                    Text(opt.label, fontSize = 13.sp)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = opt.color.copy(alpha = 0.15f),
                                selectedLabelColor = opt.color,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                selectedBorderColor = opt.color.copy(0.5f),
                                borderColor = Color.Transparent,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // ── Save Button ──
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val task = StudyTask(
                                id = existingTask?.id ?: 0,
                                title = title.trim(),
                                subject = selectedSubject,
                                description = description.trim(),
                                isCompleted = existingTask?.isCompleted ?: false,
                                dueDate = dueDate,
                                priority = priority,
                                createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(task)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = NavyDark
                    ),
                    enabled = title.isNotBlank()
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEditing) "Save Changes" else "Add Task",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
