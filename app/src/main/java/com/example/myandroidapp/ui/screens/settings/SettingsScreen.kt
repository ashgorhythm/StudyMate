package com.example.myandroidapp.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.service.BackupService
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberAdaptiveInfo()

    // SAF launchers — save to Google Drive / local
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupService.BACKUP_MIME_TYPE)
    ) { uri ->
        uri?.let { viewModel.exportToUri(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromUri(it) }
    }

    // Edit name dialog
    var showNameDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }

    // Import confirmation dialog
    var showImportConfirm by remember { mutableStateOf(false) }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Change Name", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    placeholder = { Text("Your name", color = TextMuted) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark,
                        cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateName(editName.ifBlank { "Student" })
                    showNameDialog = false
                }) { Text("Save", color = TealPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Restore Data?", color = TextPrimary) },
            text = {
                Text(
                    "This will replace ALL your current data (tasks, subjects, sessions, files) with the backed-up data. This action cannot be undone.",
                    color = TextSecondary, fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf(BackupService.BACKUP_MIME_TYPE, "application/octet-stream", "*/*"))
                }) { Text("Restore", color = RedError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    // Success / error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.exportSuccess) {
        if (uiState.exportSuccess) {
            snackbarHostState.showSnackbar("✅ Backup saved successfully!")
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { result ->
            snackbarHostState.showSnackbar("✅ Restored ${result.totalItems} items for ${result.studentName}")
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar("❌ $msg")
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd))),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (adaptive.maxContentWidth != androidx.compose.ui.unit.Dp.Unspecified)
                            Modifier.widthIn(max = adaptive.maxContentWidth)
                        else Modifier
                    )
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptive.horizontalPadding)
                    .padding(top = if (adaptive.isTablet) 24.dp else 48.dp, bottom = 32.dp)
            ) {
                // ── Header ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(Modifier.height(24.dp))

                // ── Profile Card ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, TealPrimary.copy(0.2f))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                uiState.studentName.take(1).uppercase(),
                                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(uiState.studentName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("🔥 ${uiState.streak} day streak", fontSize = 13.sp, color = TextSecondary)
                        }
                        IconButton(onClick = { editName = uiState.studentName; showNameDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit Name", tint = TealPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // ── Data Summary ──
                Text("Your Data", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DataStatChip("📋 ${uiState.totalTasks}", "Tasks", TealPrimary, Modifier.weight(1f))
                    DataStatChip("📚 ${uiState.totalSubjects}", "Subjects", PurpleAccent, Modifier.weight(1f))
                    DataStatChip("⏱️ ${uiState.totalSessions}", "Sessions", AmberAccent, Modifier.weight(1f))
                    DataStatChip("📁 ${uiState.totalFiles}", "Files", PinkAccent, Modifier.weight(1f))
                }
                Spacer(Modifier.height(28.dp))

                // ── Backup & Sync Section ──
                Text("Backup & Sync", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Save your data to Google Drive and restore it anytime",
                    fontSize = 13.sp, color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))

                // Export Button
                SettingsActionCard(
                    title = "Backup to Google Drive",
                    subtitle = "Export all data as JSON — save to Drive, OneDrive, or local storage",
                    icon = Icons.Default.CloudUpload,
                    accentColor = TealPrimary,
                    isLoading = uiState.isExporting,
                    onClick = {
                        exportLauncher.launch(BackupService.BACKUP_FILE_NAME)
                    }
                )
                Spacer(Modifier.height(12.dp))

                // Import Button
                SettingsActionCard(
                    title = "Restore from Backup",
                    subtitle = "Import a previously exported backup from Google Drive or storage",
                    icon = Icons.Default.CloudDownload,
                    accentColor = PurpleAccent,
                    isLoading = uiState.isImporting,
                    onClick = { showImportConfirm = true }
                )
                Spacer(Modifier.height(28.dp))

                // ── How It Works ──
                Text("How It Works", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                HowItWorksCard()
                Spacer(Modifier.height(28.dp))

                // ── About ──
                Text("About", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    RoundedCornerShape(16.dp),
                    CardDefaults.cardColors(SurfaceCard)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("StudyMate", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                        Text("v1.0.0 • AI-Powered Study Companion", fontSize = 13.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text("Your personal study companion with AI-powered learning, focus timer, file management, and smart analytics.", fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Components ──
// ═══════════════════════════════════════════════════════

@Composable
private fun DataStatChip(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier.height(72.dp),
        RoundedCornerShape(14.dp),
        CardDefaults.cardColors(color.copy(0.08f)),
        border = BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String, subtitle: String, icon: ImageVector,
    accentColor: Color, isLoading: Boolean, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(0.25f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(accentColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = accentColor, strokeWidth = 2.dp)
                } else {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted.copy(0.5f))
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp),
        CardDefaults.cardColors(SurfaceCard)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StepItem("1️⃣", "Tap 'Backup to Google Drive'", "The file picker opens — choose Google Drive as the save location")
            StepItem("2️⃣", "Your data is saved as JSON", "All tasks, subjects, study sessions, and files are exported")
            StepItem("3️⃣", "Reinstall or new device?", "Tap 'Restore from Backup' and pick the file from Google Drive")
            Divider(color = TextMuted.copy(0.15f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Works with Google Drive, OneDrive, and local storage", fontSize = 12.sp, color = TextMuted)
            }
        }
    }
}

@Composable
private fun StepItem(emoji: String, title: String, desc: String) {
    Row {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(desc, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}
