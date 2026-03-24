package com.example.myandroidapp.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.service.BackupService
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToInterface: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberAdaptiveInfo()

    // SAF launchers
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

    // Google Sign-In
    var authAction by remember { mutableStateOf<String?>(null) } // "upload" or "download"
    val context = androidx.compose.ui.platform.LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (authAction == "upload") {
                    viewModel.uploadToGoogleDrive(account)
                } else if (authAction == "download") {
                    viewModel.downloadFromGoogleDrive(account)
                }
            } catch (e: Exception) {
                // handle failure silently or via toast
            }
        }
    }

    val triggerGoogleSignIn = { action: String ->
        authAction = action
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    // State
    var showNameDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showDataBackup by remember { mutableStateOf(false) }
    var showProfileOverlay by remember { mutableStateOf(false) }

    // Dialogs
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

    // Snackbar
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
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
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
                if (showDataBackup) {
                    // ═══════════════════════════════════════
                    // ── Data & Backup Sub-screen ──
                    // ═══════════════════════════════════════
                    DataBackupSection(
                        uiState = uiState,
                        onBack = { showDataBackup = false },
                        onExport = { exportLauncher.launch(BackupService.BACKUP_FILE_NAME) },
                        onImport = { showImportConfirm = true },
                        onGoogleDriveUpload = { triggerGoogleSignIn("upload") },
                        onGoogleDriveDownload = { triggerGoogleSignIn("download") }
                    )
                } else {
                    // ═══════════════════════════════════════
                    // ── Main Settings ──
                    // ═══════════════════════════════════════

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
                        modifier = Modifier.fillMaxWidth().clickable { showProfileOverlay = true },
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

                    // ═══════════════════════════════════════
                    // ── Main Menu List ──
                    // ═══════════════════════════════════════
                    Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))

                    SettingsMenuItem(
                        title = "Interface & Personalization",
                        icon = Icons.Default.Palette,
                        accentColor = PurpleAccent,
                        onClick = onNavigateToInterface
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Sync & Data Backup",
                        icon = Icons.Default.CloudSync,
                        accentColor = TealPrimary,
                        onClick = { showDataBackup = true }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Security & Focus Modes",
                        icon = Icons.Default.Lock,
                        accentColor = AmberAccent,
                        onClick = { /* TODO */ }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "University Community",
                        icon = Icons.Default.Groups,
                        accentColor = PinkAccent,
                        onClick = onNavigateToCommunity
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Academic Privacy & Policy",
                        icon = Icons.Default.Policy,
                        accentColor = TextSecondary,
                        onClick = { /* TODO */ }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Feedback & Beta",
                        icon = Icons.Default.Feedback,
                        accentColor = GreenSuccess,
                        onClick = { /* TODO */ }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Share Companion App",
                        icon = Icons.Default.Share,
                        accentColor = TealPrimary,
                        onClick = { /* TODO */ }
                    )
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

            // ═══════════════════════════════════════════
            // ── Profile Card Overlay ──
            // ═══════════════════════════════════════════
            if (showProfileOverlay) {
                ProfileCardOverlay(
                    studentName = uiState.studentName,
                    onDismiss = { showProfileOverlay = false },
                    onManageProfile = {
                        showProfileOverlay = false
                        onNavigateToProfile()
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Profile Card Overlay ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ProfileCardOverlay(
    studentName: String,
    onDismiss: () -> Unit,
    onManageProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clickable(enabled = false) { /* consume click */ },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f))
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row: X (close) and gear pointing to Settings
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    // Gear icon here is just decorative/close since we're already in Settings
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, "Settings", tint = TealPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Avatar
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(3.dp, Brush.linearGradient(listOf(TealPrimary, PurpleAccent))),
                            CircleShape
                        )
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    "Hi, $studentName!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(20.dp))

                // Primary button
                Button(
                    onClick = onManageProfile,
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
// ── Data & Backup Section ──
// ═══════════════════════════════════════════════════════

@Composable
private fun DataBackupSection(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onGoogleDriveUpload: () -> Unit,
    onGoogleDriveDownload: () -> Unit
) {
    // Header
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text("Data & Backup", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    // Cloud Backup (Placeholder for future)
    SettingsActionCard(
        title = "Cloud Backup",
        subtitle = "Sync and restore your study data across devices",
        icon = Icons.Default.CloudDone,
        accentColor = TealPrimary,
        isLoading = false,
        onClick = { /* TODO: toggle cloud backup */ }
    )
    Spacer(Modifier.height(12.dp))

    // Google Drive Backup
    SettingsActionCard(
        title = "Google Drive Backup",
        subtitle = "Sync and restore your study data via Google Drive",
        icon = Icons.Default.CloudUpload,
        accentColor = TealPrimary,
        isLoading = uiState.isExporting,
        onClick = onGoogleDriveUpload
    )
    Spacer(Modifier.height(12.dp))

    // Restore from Google Drive
    SettingsActionCard(
        title = "Restore from Google Drive",
        subtitle = "Download and restore latest backup from Google Drive",
        icon = Icons.Default.CloudDownload,
        accentColor = TealPrimary,
        isLoading = uiState.isImporting,
        onClick = onGoogleDriveDownload
    )
    Spacer(Modifier.height(12.dp))

    // Create Local Backup
    SettingsActionCard(
        title = "Create Local Backup",
        subtitle = "Save backup to device local storage",
        icon = Icons.Default.SaveAlt,
        accentColor = AmberAccent,
        isLoading = false,
        onClick = onExport
    )
    Spacer(Modifier.height(12.dp))

    // Restore Local Backup
    SettingsActionCard(
        title = "Restore Local Backup",
        subtitle = "Import a previously saved local backup",
        icon = Icons.Default.Restore,
        accentColor = PurpleAccent,
        isLoading = uiState.isImporting,
        onClick = onImport
    )
    Spacer(Modifier.height(12.dp))

    // Export to CSV
    SettingsActionCard(
        title = "Export to CSV",
        subtitle = "Export your study data as a CSV spreadsheet",
        icon = Icons.Default.TableChart,
        accentColor = GreenSuccess,
        isLoading = false,
        onClick = { /* TODO: CSV export */ }
    )
    Spacer(Modifier.height(12.dp))

    // Clear all data
    SettingsActionCard(
        title = "Clear all data",
        subtitle = "Permanently delete all tasks, subjects, sessions, and files",
        icon = Icons.Default.DeleteForever,
        accentColor = RedError,
        isLoading = false,
        onClick = { /* TODO: clear all data confirmation */ }
    )
    Spacer(Modifier.height(12.dp))

    // History
    SettingsActionCard(
        title = "History",
        subtitle = "View past backup and restore operations",
        icon = Icons.Default.History,
        accentColor = TextSecondary,
        isLoading = false,
        onClick = { /* TODO: backup history */ }
    )
    Spacer(Modifier.height(28.dp))

    // How It Works
    Text("How It Works", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    HowItWorksCard()
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
private fun SettingsMenuItem(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted.copy(0.5f), modifier = Modifier.size(20.dp))
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
            HorizontalDivider(color = TextMuted.copy(0.15f))
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
