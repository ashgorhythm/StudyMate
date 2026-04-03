@file:Suppress("DEPRECATION")
package com.example.myandroidapp.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSuperUser: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberAdaptiveInfo()
    val context = LocalContext.current
    val accent = LocalAccentColor.current

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
        android.util.Log.d("SettingsScreen", "Sign-In result code: ${result.resultCode}")
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                android.util.Log.d("SettingsScreen", "Sign-In success: ${account.email}, action=$authAction")
                if (authAction == "upload") {
                    viewModel.uploadToGoogleDrive(account)
                } else if (authAction == "download") {
                    viewModel.downloadFromGoogleDrive(account)
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                android.util.Log.e("SettingsScreen", "Google Sign-In ApiException: statusCode=${e.statusCode}, message=${e.message}", e)
                val errorDetail = when (e.statusCode) {
                    12500 -> "Sign-in failed. Please check your Google Play Services and try again."
                    12501 -> "Sign-in was cancelled."
                    12502 -> "Sign-in is already in progress."
                    10 -> "Developer error: Check your OAuth client ID and SHA-1 fingerprint configuration."
                    else -> "Sign-in failed (code ${e.statusCode}): ${e.localizedMessage}"
                }
                Toast.makeText(context, errorDetail, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Sign-In unexpected error", e)
                Toast.makeText(context, "Sign-In error: ${e.localizedMessage ?: "Unknown"}", Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode == android.app.Activity.RESULT_CANCELED) {
            android.util.Log.w("SettingsScreen", "User cancelled sign-in")
            Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.w("SettingsScreen", "Unexpected result code: ${result.resultCode}")
            Toast.makeText(context, "Sign-in returned unexpected result", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerGoogleSignIn = { action: String ->
        authAction = action
        val webClientId = "916390567642-3qfj2ft31nlhgtc2t8d69eun0hnm56ta.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(webClientId)
            .requestScopes(
                Scope(DriveScopes.DRIVE_APPDATA),
                Scope(DriveScopes.DRIVE_FILE)
            )
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        // Sign out first to force the account picker to show every time
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    // State
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
                    Text("This will permanently delete ALL your study data including:", color = TextSecondary, fontSize = 14.sp)
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
                    onClick = { showClearDataConfirm = false; viewModel.clearAllData() },
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
                    Text("StudyMate stores all your data locally on this device. No data is sent to external servers.", color = TextSecondary, fontSize = 14.sp)
                    Text("To delete all local data:", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("1. Go to Data & Backup → Clear All Data\n2. Or uninstall the app from your device", color = TextSecondary, fontSize = 13.sp)
                    Text("If you've backed up to Google Drive, you must also manually delete the backup file from your Drive.", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDataDialog = false; currentSection = SettingsSection.DataBackup }) {
                    Text("Go to Data & Backup", color = TealPrimary, fontWeight = FontWeight.Bold)
                }
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
                    Text(if (uiState.betaEnrolled) "Leave Beta?" else "Join Beta Program", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (uiState.betaEnrolled) {
                    Text("You're currently a beta tester. Leaving the beta program will remove your access to upcoming features.", color = TextSecondary, fontSize = 14.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("As a beta tester, you'll get:", color = TextSecondary, fontSize = 14.sp)
                        listOf("🚀 Early access to new features", "🐛 Help us identify and fix bugs", "💡 Shape the future of StudyMate", "🏆 Exclusive beta tester badge").forEach {
                            Text(it, color = TextPrimary, fontSize = 13.sp)
                        }
                        Text("Note: Beta features may be unstable.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleBetaEnrollment(); showBetaDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.betaEnrolled) RedError else AmberAccent,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (uiState.betaEnrolled) "Leave Beta" else "Join Beta", fontWeight = FontWeight.Bold) }
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
                            Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), CardDefaults.cardColors(SurfaceCard)) {
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
    // Feedback success
    LaunchedEffect(uiState.feedbackSent) {
        if (uiState.feedbackSent) {
            snackbarHostState.showSnackbar("✅ Thank you for your feedback!")
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
                    .padding(top = if (adaptive.isTablet) 24.dp else 0.dp, bottom = 32.dp)
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
                            viewModel = viewModel,
                            onBack = { currentSection = null },
                            betaEnrolled = uiState.betaEnrolled,
                            onBetaToggle = { showBetaDialog = true }
                        )
                    }
                } else {
                    // ═══════════════════════════════════════
                    // ── Main Settings ──
                    // ═══════════════════════════════════════

                    // ── Header — fixed to top-left ──
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(Modifier.height(24.dp))

                    // ── Profile Card (no name edit button) ──
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToProfile() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, accent.copy(0.2f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(56.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(accent, PurpleAccent))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(uiState.studentName.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(uiState.studentName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (uiState.betaEnrolled) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(AmberAccent.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text("BETA", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = AmberAccent)
                                        }
                                    }
                                }
                                Text("🔥 ${uiState.streak} day streak", fontSize = 13.sp, color = TextSecondary)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextMuted.copy(0.5f))
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
                        accentColor = accent,
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
                    // Replaced "Share" with "About"
                    SettingsMenuItem(
                        title = "About StudyMate",
                        icon = Icons.Default.Info,
                        accentColor = accent,
                        onClick = onNavigateToAbout
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsMenuItem(
                        title = "SuperUser Panel",
                        icon = Icons.Default.AdminPanelSettings,
                        accentColor = CrimsonAccent,
                        onClick = onNavigateToSuperUser
                    )
                    Spacer(Modifier.height(28.dp))
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
            modifier = Modifier.widthIn(max = 340.dp).clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavyMedium),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, "Settings", tint = TealPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.size(80.dp).clip(CircleShape)
                        .border(BorderStroke(3.dp, Brush.linearGradient(listOf(TealPrimary, PurpleAccent))), CircleShape)
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(40.dp)) }
                Spacer(Modifier.height(16.dp))
                Text("Hi, $studentName!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onManageProfile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary.copy(alpha = 0.15f), contentColor = TealPrimary),
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
        Spacer(Modifier.width(8.dp))
        Text("Data & Backup", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    SettingsActionCard("Google Drive Backup", "Sync and restore your study data via Google Drive", Icons.Default.CloudDone, TealPrimary, uiState.isExporting, onGoogleDriveUpload)
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Restore from Google Drive", "Download and restore your latest cloud backup", Icons.Default.CloudDownload, TealPrimary, uiState.isImporting, onGoogleDriveDownload)
    Spacer(Modifier.height(16.dp))

    Text("Local Backup", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Create Local Backup", "Save backup to device local storage", Icons.Default.SaveAlt, AmberAccent, false, onExport)
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Restore Local Backup", "Import a previously saved local backup", Icons.Default.Restore, PurpleAccent, uiState.isImporting, onImport)
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Export to CSV", "Export your study data as a CSV spreadsheet", Icons.Default.TableChart, GreenSuccess, false, onCsvExport)
    Spacer(Modifier.height(16.dp))

    Text("Data Management", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Clear All Data", "Permanently delete all tasks, subjects, sessions, and files", Icons.Default.DeleteForever, RedError, false, onClearAllData)
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Backup History", "View past backup and restore operations", Icons.Default.History, TextSecondary, false, onShowHistory)
    Spacer(Modifier.height(28.dp))

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

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
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
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), CardDefaults.cardColors(TealPrimary.copy(0.06f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = TealPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("These settings are saved and will persist across app restarts.", fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Privacy & Policy Section (In-App) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun PrivacyPolicySection(onBack: () -> Unit, onDataDeletion: () -> Unit) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsOfUse by remember { mutableStateOf(false) }

    // Privacy Policy in-app dialog
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Privacy Policy", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Last Updated: April 2026", color = TextMuted, fontSize = 11.sp)

                    SectionTitle("1. Data Collection")
                    Text("StudyMate collects and stores the following data locally on your device:\n• Your display name\n• Study tasks, subjects, and sessions\n• Study files metadata\n• App preferences and settings", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("2. Data Storage")
                    Text("All your data is stored locally on your device using Android's Room database and DataStore. No personal data is transmitted to external servers without your explicit consent.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("3. Cloud Backup")
                    Text("When you choose to backup to Google Drive, your study data (tasks, subjects, sessions, files) is uploaded to your personal Google Drive account. StudyMate does not have access to your Google Drive beyond the files it creates.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("4. AI Chat")
                    Text("AI Chat uses the Gemini API to process your prompts. Prompts are sent to Google's servers for processing but are not stored permanently by StudyMate. Please refer to Google's AI policy for more details.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("5. Community Features")
                    Text("Community posts and interactions are stored on Firebase servers. Your display name and post content are visible to other users in the community.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("6. Data Deletion")
                    Text("You can delete all your local data at any time through Settings → Data & Backup → Clear All Data. You can also uninstall the app to remove all local data.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("7. Contact")
                    Text("For privacy concerns, contact us through the in-app Feedback feature.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) { Text("Close", color = TealPrimary) }
            },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    // Terms of Use in-app dialog
    if (showTermsOfUse) {
        AlertDialog(
            onDismissRequest = { showTermsOfUse = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = PurpleAccent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Terms of Use", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Last Updated: April 2026", color = TextMuted, fontSize = 11.sp)

                    SectionTitle("1. Acceptance of Terms")
                    Text("By using StudyMate, you agree to these Terms of Use. If you do not agree, please discontinue use of the application.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("2. Academic Disclaimer")
                    Text("StudyMate is a study aid and companion tool. It does not guarantee academic success. The AI chat feature provides general assistance and should not be relied upon as a sole source of academic information.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("3. User Responsibilities")
                    Text("• You are responsible for maintaining backups of your data\n• You agree not to use the app for any illegal or harmful purposes\n• You agree to use the community features responsibly\n• You will not post offensive, harmful, or inappropriate content", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("4. Community Guidelines")
                    Text("• Be respectful to other users\n• Do not share personal information of others\n• Spam, harassment, and inappropriate content will result in account restrictions\n• The developer reserves the right to moderate content and restrict users", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("5. Intellectual Property")
                    Text("StudyMate and its original content, features, and functionality are owned by the developer. The app is provided 'as is' without warranty of any kind.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("6. Limitation of Liability")
                    Text("The developer shall not be liable for any indirect, incidental, special, consequential, or punitive damages including loss of data.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)

                    SectionTitle("7. Changes to Terms")
                    Text("We reserve the right to modify these terms at any time. Continued use of the app after changes constitutes acceptance of the new terms.", color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsOfUse = false }) { Text("Close", color = TealPrimary) }
            },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
        Spacer(Modifier.width(8.dp))
        Text("Academic Privacy & Policy", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    SettingsActionCard("Privacy Policy", "Review how your study data is stored and protected", Icons.Default.Shield, TealPrimary, false) { showPrivacyPolicy = true }
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Terms of Use", "Read app usage terms and academic disclaimer", Icons.Default.Description, PurpleAccent, false) { showTermsOfUse = true }
    Spacer(Modifier.height(12.dp))
    SettingsActionCard("Data Deletion Request", "Get guidance to remove all local app data", Icons.Default.DeleteSweep, RedError, false, onDataDeletion)
    Spacer(Modifier.height(24.dp))

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard)) {
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
}

// ═══════════════════════════════════════════════════════
// ── Feedback & Beta Section (In-App) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun FeedbackBetaSection(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    betaEnrolled: Boolean,
    onBetaToggle: () -> Unit
) {
    var showFeedbackForm by remember { mutableStateOf(false) }
    var showBugForm by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var bugDescription by remember { mutableStateOf("") }
    var bugSteps by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Feedback Form Dialog
    if (showFeedbackForm) {
        AlertDialog(
            onDismissRequest = { showFeedbackForm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Feedback, null, tint = GreenSuccess, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Send Feedback", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Share your thoughts, suggestions, or feature requests:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Your feedback...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.3f),
                            focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark,
                            cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            viewModel.submitFeedback("Feedback", feedbackText)
                            feedbackText = ""
                            showFeedbackForm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = NavyDark),
                    shape = RoundedCornerShape(12.dp),
                    enabled = feedbackText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showFeedbackForm = false }) { Text("Cancel", color = TextMuted) } },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    // Bug Report Form Dialog
    if (showBugForm) {
        AlertDialog(
            onDismissRequest = { showBugForm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = RedError, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Report a Bug", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Bug Description", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        placeholder = { Text("What went wrong?", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.3f),
                            focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark,
                            cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 4
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Steps to Reproduce (optional)", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = bugSteps,
                        onValueChange = { bugSteps = it },
                        placeholder = { Text("1. Go to...\n2. Click...\n3. See error", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.3f),
                            focusedContainerColor = NavyDark, unfocusedContainerColor = NavyDark,
                            cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bugDescription.isNotBlank()) {
                            val fullReport = "Bug: $bugDescription\n\nSteps: $bugSteps"
                            viewModel.submitFeedback("Bug Report", fullReport)
                            bugDescription = ""
                            bugSteps = ""
                            showBugForm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    enabled = bugDescription.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Submit Bug", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showBugForm = false }) { Text("Cancel", color = TextMuted) } },
            containerColor = NavyMedium, shape = RoundedCornerShape(20.dp)
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
        Spacer(Modifier.width(8.dp))
        Text("Feedback & Beta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(Modifier.height(24.dp))

    // In-app feedback
    SettingsActionCard("Send Feedback", "Share suggestions or feature requests", Icons.Default.Feedback, GreenSuccess, false) { showFeedbackForm = true }
    Spacer(Modifier.height(12.dp))
    SettingsActionCard(if (betaEnrolled) "Beta Program (Enrolled ✅)" else "Join Beta Program", if (betaEnrolled) "You're currently testing upcoming features" else "Try upcoming features and help us improve", Icons.Default.Science, if (betaEnrolled) GreenSuccess else AmberAccent, false, onBetaToggle)
    Spacer(Modifier.height(12.dp))

    // In-app bug report
    SettingsActionCard("Report a Bug", "Help us fix issues you've encountered", Icons.Default.BugReport, RedError, false) { showBugForm = true }

    if (betaEnrolled) {
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), CardDefaults.cardColors(AmberAccent.copy(0.06f)), border = BorderStroke(1.dp, AmberAccent.copy(0.15f))) {
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
    Card(modifier.height(72.dp), RoundedCornerShape(14.dp), CardDefaults.cardColors(color.copy(0.08f)), border = BorderStroke(1.dp, color.copy(0.2f))) {
        Column(Modifier.fillMaxSize().padding(10.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun SettingsMenuItem(title: String, icon: ImageVector, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted.copy(0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsActionCard(title: String, subtitle: String, icon: ImageVector, accentColor: Color, isLoading: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(0.25f))
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(accentColor.copy(0.12f)), contentAlignment = Alignment.Center) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = accentColor, strokeWidth = 2.dp) else Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
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
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(SurfaceCard)) {
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
private fun SettingsToggleCard(title: String, subtitle: String, icon: ImageVector, accentColor: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(0.25f))
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(0.12f)), contentAlignment = Alignment.Center) {
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
