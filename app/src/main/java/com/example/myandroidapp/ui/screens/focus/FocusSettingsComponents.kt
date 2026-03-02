package com.example.myandroidapp.ui.screens.focus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.data.model.AllowedContact
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.ui.theme.*

/**
 * Dialog to manage allowed contacts during focus mode.
 * Uses real contact picker via [onPickContact].
 */
@Composable
fun AllowedContactsDialog(
    allowedContacts: List<AllowedContact>,
    onDismiss: () -> Unit,
    onPickContact: () -> Unit, // Opens system contact picker
    onRemoveContact: (AllowedContact) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContactPhone, null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Allowed Contacts", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column {
                Text(
                    "These contacts can call you via DND priority mode while focus is active.",
                    color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                if (allowedContacts.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(60.dp).clip(CircleShape).background(TealPrimary.copy(0.1f)),
                                Alignment.Center
                            ) {
                                Icon(Icons.Default.PersonOff, null, tint = TealPrimary, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("No contacts added", color = TextMuted, fontSize = 14.sp)
                            Text("All calls silenced during focus", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allowedContacts, key = { it.phoneNumber }) { contact ->
                            AllowedContactItem(contact = contact, onRemove = { onRemoveContact(contact) })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onDismiss(); onPickContact() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add from Contacts", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TealPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AllowedContactItem(contact: AllowedContact, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(TealPrimary.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.name.take(1).uppercase(),
                    color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    contact.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(contact.phoneNumber, fontSize = 12.sp, color = TextMuted)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close, "Remove", tint = RedError,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Card showing DND toggle and Allowed Contacts settings.
 */
@Composable
fun FocusSettingsCard(
    isDndEnabled: Boolean,
    hasDndPermission: Boolean,
    onToggleDnd: (Boolean) -> Unit,
    onRequestDndPermission: () -> Unit,
    allowedContactsCount: Int,
    onManageContacts: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Focus Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            HorizontalDivider(color = TextMuted.copy(0.15f))

            // DND Toggle
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (hasDndPermission) Icons.Default.DoNotDisturb else Icons.Default.DoNotDisturbOff,
                        null,
                        tint = if (hasDndPermission) TealPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Do Not Disturb", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (hasDndPermission) "Silence notifications during focus" else "Tap to grant permission",
                            color = if (hasDndPermission) TextSecondary else AmberAccent,
                            fontSize = 11.sp
                        )
                    }
                }
                if (hasDndPermission) {
                    Switch(
                        checked = isDndEnabled,
                        onCheckedChange = onToggleDnd,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyDark, checkedTrackColor = TealPrimary
                        )
                    )
                } else {
                    TextButton(onClick = onRequestDndPermission) {
                        Text("Grant", color = TealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = TextMuted.copy(0.1f))

            // Allowed Contacts row
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onManageContacts() }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContactPhone, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Allowed Contacts", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (allowedContactsCount == 0) "No contacts — all calls silenced"
                            else "$allowedContactsCount contact${if (allowedContactsCount > 1) "s" else ""} can reach you",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (allowedContactsCount > 0) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(PurpleAccent.copy(0.2f)),
                            Alignment.Center
                        ) {
                            Text("$allowedContactsCount", color = PurpleAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * Dialog for picking the study subject for a focus session.
 */
@Composable
fun SubjectPickerDialog(
    subjects: List<Subject>,
    currentSubject: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        titleContentColor = TextPrimary,
        title = { Text("Select Subject", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column {
                if (subjects.isEmpty()) {
                    Text(
                        "No subjects added yet.\nAdd subjects from the Dashboard tab.",
                        color = TextSecondary, fontSize = 14.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        subjects.forEach { subject ->
                            val isSelected = currentSubject == subject.name
                            val color = try {
                                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(subject.colorHex))
                            } catch (e: Exception) { TealPrimary }

                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(subject.name) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) color.copy(alpha = 0.15f) else SurfaceCard
                                ),
                                border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null
                            ) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) { Text(subject.icon, fontSize = 18.sp) }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        subject.name, color = TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp, modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TealPrimary)) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}
