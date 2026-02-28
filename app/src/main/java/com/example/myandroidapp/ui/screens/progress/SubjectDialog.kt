package com.example.myandroidapp.ui.screens.progress

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.ui.theme.*

@Composable
fun AddEditSubjectDialog(
    existingSubject: Subject? = null,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit
) {
    val isEditing = existingSubject != null

    var name by remember { mutableStateOf(existingSubject?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(existingSubject?.icon ?: "📚") }
    var selectedColor by remember { mutableStateOf(existingSubject?.colorHex ?: "#13ECEC") }
    var totalTopics by remember { mutableStateOf(existingSubject?.totalTopics?.toString() ?: "") }

    val emojiOptions = listOf(
        "📚", "📐", "🔬", "📖", "🏛️", "🎨", "🧮", "🌍",
        "💻", "🎵", "🧪", "📊", "✍️", "🔭", "🧬", "📝"
    )

    val colorOptions = listOf(
        "#13ECEC" to TealPrimary,
        "#7C4DFF" to PurpleAccent,
        "#FFAB40" to AmberAccent,
        "#FF4081" to PinkAccent,
        "#69F0AE" to GreenSuccess,
        "#FF5252" to RedError,
        "#FFD54F" to AmberLight,
        "#B388FF" to PurpleLight
    )

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
            border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isEditing) "Edit Subject" else "New Subject",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ── Name Field ──
                Text("Subject Name", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Mathematics", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = PurpleAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                // ── Total Topics ──
                Text("Total Topics", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = totalTopics,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) totalTopics = newValue
                    },
                    placeholder = { Text("e.g. 15", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = PurpleAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Tag, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                )
                Spacer(Modifier.height(20.dp))

                // ── Icon Selector ──
                Text("Icon", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojiOptions) { emoji ->
                        val isSelected = selectedIcon == emoji
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) PurpleAccent.copy(alpha = 0.2f)
                                    else SurfaceCard
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) PurpleAccent else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ── Color Selector ──
                Text("Color", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colorOptions.forEach { (hex, color) ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    "Selected",
                                    tint = NavyDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // ── Preview ──
                Text("Preview", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                val previewColor = try {
                    Color(android.graphics.Color.parseColor(selectedColor))
                } catch (e: Exception) {
                    TealPrimary
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, previewColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(previewColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedIcon, fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            name.ifBlank { "Subject Name" },
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // ── Save Button ──
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val subject = Subject(
                                id = existingSubject?.id ?: 0,
                                name = name.trim(),
                                icon = selectedIcon,
                                colorHex = selectedColor,
                                totalTopics = totalTopics.toIntOrNull() ?: 0,
                                completedTopics = existingSubject?.completedTopics ?: 0,
                                totalStudyMinutes = existingSubject?.totalStudyMinutes ?: 0
                            )
                            onSave(subject)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = TextPrimary
                    ),
                    enabled = name.isNotBlank()
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEditing) "Save Changes" else "Add Subject",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    subjectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    "Warning",
                    tint = RedError,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Delete Subject", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text("Are you sure you want to delete \"$subjectName\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedError,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancel")
            }
        }
    )
}
