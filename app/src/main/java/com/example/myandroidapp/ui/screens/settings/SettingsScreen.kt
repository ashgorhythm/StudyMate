package com.example.myandroidapp.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import androidx.core.net.toUri

private enum class SettingsSection {
    DataBackup,
    SecurityFocus,
    PrivacyPolicy,
    FeedbackBeta
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToInterface: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberAdaptiveInfo()
    val context = LocalContext.current

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

    // CSV Export launcher
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportToCsv(it) }
    }

    // Google Sign-In
    var authAction by remember { mutableStateOf<String?>(null) }

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
            } catch (_: Exception) { }
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
    var showProfileOverlay by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showBetaDialog by remember { mutableStateOf(false) }
    var showBackupHistory by remember { mutableStateOf(false) }

    // ═══════════════════════════════════════
    // ── Dialogs ──
    // ═══════════════════════════════════════

    // Name dialog
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

    // Import confirm dialog
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

    // Clear all data confirm dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = RedError, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Delete All Data?", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "This will permanently delete ALL your study data including:",
                        color = TextSecondary, fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    listOf("• All tasks", "• All subjects", "• All study sessions", "• All file metadata", "• Your student name & settings").forEach {
                        Text(it, color = RedError.copy(0.8f), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("⚠️ This action CANNOT be undone!", color = RedError, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataConfirm = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    // Data deletion request dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteSweep, null, tint = RedError, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Data Deletion", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "StudyMate stores all your data locally on this device. No data is sent to external servers.",
                        color = TextSecondary, fontSize = 14.sp
                    )
                    Text("To delete all local data:", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("1. Go to Data & Backup → Clear All Data\n2. Or uninstall the app from your device", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "If you've backed up to Google Drive, you must also manually delete the backup file from your Drive.",
                        color = TextSecondary, fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDataDialog = false
                    currentSection = SettingsSection.DataBackup
                }) { Text("Go to Data & Backup", color = TealPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) { Text("Close", color = TextMuted) }
            },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    // Beta enrollment dialog
    if (showBetaDialog) {
        AlertDialog(
            onDismissRequest = { showBetaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, null, tint = AmberAccent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (uiState.betaEnrolled) "Leave Beta?" else "Join Beta Program",
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                if (uiState.betaEnrolled) {
                    Text(
                        "You're currently a beta tester. Leaving the beta program will remove your access to upcoming features.",
                        color = TextSecondary, fontSize = 14.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("As a beta tester, you'll get:", color = TextSecondary, fontSize = 14.sp)
                        listOf(
                            "🚀 Early access to new features",
                            "🐛 Help us identify and fix bugs",
                            "💡 Shape the future of StudyMate",
                            "🏆 Exclusive beta tester badge"
                        ).forEach {
                            Text(it, color = TextPrimary, fontSize = 13.sp)
                        }
                        Text("Note: Beta features may be unstable.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleBetaEnrollment()
                        showBetaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.betaEnrolled) RedError else AmberAccent,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (uiState.betaEnrolled) "Leave Beta" else "Join Beta",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBetaDialog = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    // Backup History Dialog
    if (showBackupHistory) {
        AlertDialog(
            onDismissRequest = { showBackupHistory = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Backup History", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (uiState.backupHistory.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("📋", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No backup history yet", color = TextSecondary, fontSize = 14.sp)
                        Text("Create a backup to see it here", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.backupHistory.takeLast(10).reversed().forEach { entry ->
                            Card(
                                Modifier.fillMaxWidth(),
                                RoundedCornerShape(12.dp),
                                CardDefaults.cardColors(SurfaceCard)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (entry.contains("Export") || entry.contains("Upload")) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
                                        null,
                                        tint = if (entry.contains("Export") || entry.contains("Upload")) TealPrimary else PurpleAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(entry, fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupHistory = false }) { Text("Close", color = TealPrimary) }
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
    LaunchedEffect(uiState.csvExportSuccess) {
        if (uiState.csvExportSuccess) {
            snackbarHostState.showSnackbar("✅ CSV exported successfully!")
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
                if (currentSection != null) {
                    when (currentSection!!) {
                        SettingsSection.DataBackup -> DataBackupSection(
                            uiState = uiState,
                            onBack = { currentSection = null },
                            onExport = { exportLauncher.launch(BackupService.BACKUP_FILE_NAME) },
                            onImport = { showImportConfirm = true },
                            onGoogleDriveUpload = { triggerGoogleSignIn("upload") },
                            onGoogleDriveDownload = { triggerGoogleSignIn("download") },
                            onCsvExport = { csvExportLauncher.launch("studymate_data.csv") },
                            onClearAllData = { showClearDataConfirm = true },
                            onShowHistory = { showBackupHistory = true }
                        )
                        SettingsSection.SecurityFocus -> SecurityFocusSection(
                            viewModel = viewModel,
                            onBack = { currentSection = null }
                        )
                        SettingsSection.PrivacyPolicy -> PrivacyPolicySection(
                            onBack = { currentSection = null },
                            onDataDeletion = { showDeleteDataDialog = true }
                        )
                        SettingsSection.FeedbackBeta -> FeedbackBetaSection(
                            onBack = { currentSection = null },
                            betaEnrolled = uiState.betaEnrolled,
                            onBetaToggle = { showBetaDialog = true }
                        )
                    }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(uiState.studentName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (uiState.betaEnrolled) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            Modifier.clip(RoundedCornerShape(4.dp)).background(AmberAccent.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("BETA", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = AmberAccent)
                                        }
                                    }
                                }
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
                        onClick = { currentSection = SettingsSection.DataBackup }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Security & Focus Modes",
                        icon = Icons.Default.Lock,
                        accentColor = AmberAccent,
                        onClick = { currentSection = SettingsSection.SecurityFocus }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Academic Privacy & Policy",
                        icon = Icons.Default.Policy,
                        accentColor = TextSecondary,
                        onClick = { currentSection = SettingsSection.PrivacyPolicy }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Feedback & Beta",
                        icon = Icons.Default.Feedback,
                        accentColor = GreenSuccess,
                        onClick = { currentSection = SettingsSection.FeedbackBeta }
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "Share Companion App",
                        icon = Icons.Default.Share,
                        accentColor = TealPrimary,
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Try StudyBuddy - your AI-powered study companion! 📚🎓"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share StudyBuddy"))
                        }
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("StudyMate", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                Spacer(Modifier.width(8.dp))
                                if (uiState.betaEnrolled) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(4.dp)).background(AmberAccent.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("BETA", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = AmberAccent)
                                    }
                                }
                            }
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
    onManageProfile: () -> Unit,
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
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f))
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, "Settings", tint = TealPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))

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
                    Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(16.dp))

                Text("Hi, $studentName!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(20.dp))

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
                    Text("Manage Student Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Data & Backup Section (Complete) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun DataBackupSection(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onGoogleDriveUpload: () -> Unit,
    onGoogleDriveDownload: () -> Unit,
    onCsvExport: () -> Unit,
    onClearAllData: () -> Unit,
    onShowHistory: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text("Data & Backup", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    // Cloud Backup
    SettingsActionCard(
        title = "Google Drive Backup",
        subtitle = "Sync and restore your study data via Google Drive",
        icon = Icons.Default.CloudDone,
        accentColor = TealPrimary,
        isLoading = uiState.isExporting,
        onClick = onGoogleDriveUpload
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Restore from Google Drive",
        subtitle = "Download and restore your latest cloud backup",
        icon = Icons.Default.CloudDownload,
        accentColor = TealPrimary,
        isLoading = uiState.isImporting,
        onClick = onGoogleDriveDownload
    )
    Spacer(Modifier.height(16.dp))

    Text("Local Backup", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Create Local Backup",
        subtitle = "Save backup to device local storage",
        icon = Icons.Default.SaveAlt,
        accentColor = AmberAccent,
        isLoading = false,
        onClick = onExport
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Restore Local Backup",
        subtitle = "Import a previously saved local backup",
        icon = Icons.Default.Restore,
        accentColor = PurpleAccent,
        isLoading = uiState.isImporting,
        onClick = onImport
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Export to CSV",
        subtitle = "Export your study data as a CSV spreadsheet",
        icon = Icons.Default.TableChart,
        accentColor = GreenSuccess,
        isLoading = false,
        onClick = onCsvExport
    )
    Spacer(Modifier.height(16.dp))

    Text("Data Management", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Clear All Data",
        subtitle = "Permanently delete all tasks, subjects, sessions, and files",
        icon = Icons.Default.DeleteForever,
        accentColor = RedError,
        isLoading = false,
        onClick = onClearAllData
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Backup History",
        subtitle = "View past backup and restore operations",
        icon = Icons.Default.History,
        accentColor = TextSecondary,
        isLoading = false,
        onClick = onShowHistory
    )
    Spacer(Modifier.height(28.dp))

    // How It Works
    Text("How It Works", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    HowItWorksCard()
}

// ═══════════════════════════════════════════════════════
// ── Security & Focus Section (Persisted) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun SecurityFocusSection(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text("Security & Focus Modes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    SettingsToggleCard(
        title = "Require PIN for app open",
        subtitle = "Add an extra lock before accessing study data",
        icon = Icons.Default.Password,
        accentColor = AmberAccent,
        checked = uiState.requirePin,
        onCheckedChange = { viewModel.updateRequirePin(it) }
    )
    Spacer(Modifier.height(12.dp))

    SettingsToggleCard(
        title = "Lock app during focus session",
        subtitle = "Block leaving the timer until session ends",
        icon = Icons.Default.Lock,
        accentColor = TealPrimary,
        checked = uiState.lockDuringFocus,
        onCheckedChange = { viewModel.updateLockDuringFocus(it) }
    )
    Spacer(Modifier.height(12.dp))

    SettingsToggleCard(
        title = "Enable DND during focus",
        subtitle = "Silence distractions while focus mode runs",
        icon = Icons.Default.NotificationsOff,
        accentColor = PurpleAccent,
        checked = uiState.dndDuringFocus,
        onCheckedChange = { viewModel.updateDndDuringFocus(it) }
    )

    Spacer(Modifier.height(24.dp))
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(12.dp),
        CardDefaults.cardColors(TealPrimary.copy(0.06f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("These settings are saved and will persist across app restarts.", fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Privacy & Policy Section (Complete) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun PrivacyPolicySection(onBack: () -> Unit, onDataDeletion: () -> Unit) {
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text("Academic Privacy & Policy", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    SettingsActionCard(
        title = "Privacy Policy",
        subtitle = "Review how your study data is stored and protected",
        icon = Icons.Default.Shield,
        accentColor = TealPrimary,
        isLoading = false,
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, "https://example.com/privacy".toUri())
            context.startActivity(intent)
        }
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Terms of Use",
        subtitle = "Read app usage terms and academic disclaimer",
        icon = Icons.Default.Description,
        accentColor = PurpleAccent,
        isLoading = false,
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, "https://example.com/terms".toUri())
            context.startActivity(intent)
        }
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Data Deletion Request",
        subtitle = "Get guidance to remove all local app data",
        icon = Icons.Default.DeleteSweep,
        accentColor = RedError,
        isLoading = false,
        onClick = onDataDeletion
    )

    Spacer(Modifier.height(24.dp))

    // Data summary card
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp),
        CardDefaults.cardColors(SurfaceCard)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("🔒 Your Privacy", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text("• All data is stored locally on your device", fontSize = 12.sp, color = TextSecondary)
            Text("• No personal data is sent to external servers", fontSize = 12.sp, color = TextSecondary)
            Text("• AI chat uses Gemini API (prompts are not stored)", fontSize = 12.sp, color = TextSecondary)
            Text("• You can delete all data at any time", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Feedback & Beta Section (Complete) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun FeedbackBetaSection(
    onBack: () -> Unit,
    betaEnrolled: Boolean,
    onBetaToggle: () -> Unit
) {
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text("Feedback & Beta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    SettingsActionCard(
        title = "Send Feedback",
        subtitle = "Report bugs or suggest features",
        icon = Icons.Default.Feedback,
        accentColor = GreenSuccess,
        isLoading = false,
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:support@studybuddy.app".toUri()
                putExtra(Intent.EXTRA_SUBJECT, "StudyMate Feedback")
                putExtra(Intent.EXTRA_TEXT, "Hi StudyMate team,\n\n")
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = if (betaEnrolled) "Beta Program (Enrolled ✅)" else "Join Beta Program",
        subtitle = if (betaEnrolled) "You're currently testing upcoming features" else "Try upcoming features and help us improve",
        icon = Icons.Default.Science,
        accentColor = if (betaEnrolled) GreenSuccess else AmberAccent,
        isLoading = false,
        onClick = onBetaToggle
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Rate this App",
        subtitle = "Share your experience with StudyMate",
        icon = Icons.Default.Star,
        accentColor = PurpleAccent,
        isLoading = false,
        onClick = {
            try {
                // Try Play Store first
                val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to browser
                val intent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri())
                context.startActivity(intent)
            }
        }
    )
    Spacer(Modifier.height(12.dp))

    SettingsActionCard(
        title = "Report a Bug",
        subtitle = "Help us fix issues you've encountered",
        icon = Icons.Default.BugReport,
        accentColor = RedError,
        isLoading = false,
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:bugs@studybuddy.app".toUri()
                putExtra(Intent.EXTRA_SUBJECT, "StudyMate Bug Report")
                putExtra(Intent.EXTRA_TEXT, "Bug description:\n\nSteps to reproduce:\n1. \n2. \n3. \n\nExpected behavior:\n\nActual behavior:\n\nDevice info:\n")
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    )

    if (betaEnrolled) {
        Spacer(Modifier.height(20.dp))
        Card(
            Modifier.fillMaxWidth(),
            RoundedCornerShape(12.dp),
            CardDefaults.cardColors(AmberAccent.copy(0.06f)),
            border = BorderStroke(1.dp, AmberAccent.copy(0.15f))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🧪", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Beta Tester", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                    Text("Thank you for helping improve StudyMate!", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Reusable Components ──
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
    onClick: () -> Unit,
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
    accentColor: Color, isLoading: Boolean, onClick: () -> Unit,
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

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(0.25f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NavyDark,
                    checkedTrackColor = TealPrimary,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = NavyLight
                )
            )
        }
    }
}
