package com.example.myandroidapp.ui.screens.focus

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myandroidapp.data.model.AllowedContact
import com.example.myandroidapp.data.model.Subject
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import kotlinx.coroutines.launch

@Composable
fun FocusScreen(viewModel: FocusViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Supply context to ViewModel
    LaunchedEffect(Unit) { viewModel.setAppContext(context) }

    // Refresh DND permission when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDndPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── App Lock: intercept back press during focus ──
    BackHandler(enabled = uiState.isAppLocked) {
        viewModel.onBackPressedDuringFocus()
        scope.launch {
            snackbarHostState.showSnackbar(
                "🔒 Focus mode is active! Finish or reset the timer to leave.",
                duration = SnackbarDuration.Short
            )
        }
    }

    // Exit blocked snack
    LaunchedEffect(uiState.showExitBlockedSnack) {
        if (uiState.showExitBlockedSnack) {
            snackbarHostState.showSnackbar(
                "🔒 Focus mode is active! Finish or reset the timer to leave.",
                duration = SnackbarDuration.Short
            )
            viewModel.clearExitBlockedSnack()
        }
    }

    // ── Contact permission launcher ──
    var pendingContactUri by remember { mutableStateOf<Uri?>(null) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingContactUri?.let { uri ->
                val contact = resolveContact(context, uri)
                contact?.let { viewModel.addAllowedContact(it) }
                pendingContactUri = null
            }
        }
    }

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val contact = resolveContact(context, uri)
            contact?.let { viewModel.addAllowedContact(it) }
        } else {
            pendingContactUri = uri
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Subject Picker Dialog
    if (uiState.showSubjectPicker) {
        SubjectPickerDialog(
            subjects = uiState.subjects,
            currentSubject = uiState.currentSubject,
            onSelect = { viewModel.setSubject(it) },
            onDismiss = { viewModel.dismissSubjectPicker() }
        )
    }

    // Allowed Contacts Dialog
    if (uiState.showAllowedContactsDialog) {
        AllowedContactsDialog(
            allowedContacts = uiState.allowedContacts,
            onDismiss = { viewModel.dismissAllowedContactsDialog() },
            onPickContact = { contactPickerLauncher.launch(null) },
            onRemoveContact = { viewModel.removeAllowedContact(it) }
        )
    }

    // Custom Duration Dialog
    if (uiState.showCustomDurationDialog) {
        CustomDurationDialog(
            currentMinutes = uiState.customDurationMinutes,
            onDismiss = { viewModel.dismissCustomDurationDialog() },
            onConfirm = { viewModel.setCustomDuration(it) }
        )
    }

    val adaptive = rememberAdaptiveInfo()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(NavyDark, Color(0xFF050714)))),
            contentAlignment = Alignment.TopCenter
        ) {
            // App-lock overlay indicator
            if (uiState.isAppLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    // Shown as the top bar banner
                }
            }

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
                    .padding(
                        top = if (adaptive.isTablet) 24.dp else 48.dp,
                        bottom = if (adaptive.isTablet) 32.dp else 100.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Focus Mode Banner ──
                FocusModeBanner(isActive = uiState.isRunning)
                Spacer(modifier = Modifier.height(12.dp))

                // ── App Lock Notice ──
                if (uiState.isAppLocked) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = RedError.copy(0.1f)),
                        border = BorderStroke(1.dp, RedError.copy(0.3f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Lock, null, tint = RedError, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "App locked until timer finishes",
                                color = RedError, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // ── Current Subject ──
                Card(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = !uiState.isRunning) { viewModel.showSubjectPicker() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MenuBook, null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Studying: ${uiState.currentSubject}",
                            fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium
                        )
                        if (!uiState.isRunning) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                // ── Timer Ring ──
                TimerRing(
                    seconds = uiState.timerSeconds,
                    totalSeconds = uiState.totalSeconds,
                    isRunning = uiState.isRunning
                )
                Spacer(modifier = Modifier.height(32.dp))

                // ── Controls ──
                TimerControls(
                    isRunning = uiState.isRunning,
                    onToggle = { viewModel.toggleTimer() },
                    onReset = { viewModel.resetTimer() }
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Duration Chips (with Custom) ──
                DurationSelector(
                    selected = uiState.selectedDuration,
                    customMinutes = uiState.customDurationMinutes,
                    isRunning = uiState.isRunning,
                    onSelect = { viewModel.setDuration(it) },
                    onCustom = { viewModel.showCustomDurationDialog() }
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Ambient Sounds ──
                AmbientSoundSelector(
                    selected = uiState.selectedAmbientSound,
                    onSelect = { viewModel.setAmbientSound(it) }
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Focus Settings (DND + Contacts) ──
                FocusSettingsCard(
                    isDndEnabled = uiState.isDndEnabled,
                    hasDndPermission = uiState.hasDndPermission,
                    onToggleDnd = { viewModel.toggleDnd(it) },
                    onRequestDndPermission = { viewModel.requestDndPermission(context) },
                    allowedContactsCount = uiState.allowedContacts.size,
                    onManageContacts = { viewModel.showAllowedContactsDialog() }
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Today's Stats ──
                TodayStats(
                    sessions = uiState.todaySessionCount,
                    minutes = uiState.todayTotalMinutes
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────
// Contact resolution helper
// ─────────────────────────────────────────────────

private fun resolveContact(context: android.content.Context, uri: Uri): AllowedContact? {
    return try {
        var name = "Unknown"
        var phone = ""
        var photoUri: String? = null

        // Get display name + photo
        context.contentResolver.query(
            uri, arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.PHOTO_URI
            ), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: "Unknown"
                val contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                photoUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))

                // Get phone number
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId.toString()),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        phone = phoneCursor.getString(
                            phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        ) ?: ""
                    }
                }
            }
        }

        if (phone.isNotBlank()) AllowedContact(name = name, phoneNumber = phone, photoUri = photoUri)
        else null
    } catch (e: Exception) {
        null
    }
}

// ─────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────

@Composable
private fun CustomDurationDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours by remember { mutableStateOf((currentMinutes / 60).toString()) }
    var mins by remember { mutableStateOf((currentMinutes % 60).toString()) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AlarmAdd, null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Custom Duration", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column {
                Text("Set session length (max 5 hours)", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hours
                    Column(Modifier.weight(1f)) {
                        Text("Hours", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = hours,
                            onValueChange = { if (it.length <= 1) hours = it.filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = TextMuted.copy(0.3f),
                                focusedContainerColor = NavyDark,
                                unfocusedContainerColor = NavyDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = TealPrimary
                            ),
                            suffix = { Text("h", color = TextMuted) }
                        )
                    }
                    // Minutes
                    Column(Modifier.weight(1f)) {
                        Text("Minutes", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = mins,
                            onValueChange = { if (it.length <= 2) mins = it.filter { c -> c.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = TextMuted.copy(0.3f),
                                focusedContainerColor = NavyDark,
                                unfocusedContainerColor = NavyDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = TealPrimary
                            ),
                            suffix = { Text("m", color = TextMuted) }
                        )
                    }
                }
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = RedError, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                // Quick presets row
                Text("Quick presets", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(45, 90, 120).forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                hours = (preset / 60).toString()
                                mins = (preset % 60).toString()
                            },
                            label = {
                                val h = preset / 60; val m = preset % 60
                                Text(if (h > 0) "${h}h ${m}m" else "${m}m", fontSize = 12.sp)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NavyLight, labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hours.toIntOrNull() ?: 0
                    val m = mins.toIntOrNull() ?: 0
                    val total = h * 60 + m
                    when {
                        total < 1 -> error = "Minimum 1 minute"
                        total > 300 -> error = "Maximum 5 hours (300 min)"
                        else -> onConfirm(total)
                    }
                },
                colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Set Duration", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

// ─────────────────────────────────────────────────
// UI Components
// ─────────────────────────────────────────────────


@Composable
private fun FocusModeBanner(isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) TealPrimary.copy(alpha = 0.15f) else SurfaceCard
        ),
        border = BorderStroke(1.dp, if (isActive) TealPrimary.copy(alpha = 0.5f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (isActive) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (isActive) TealPrimary else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isActive) "Focus Mode Active — Stay focused! 🔥" else "Focus Mode",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) TealPrimary else TextPrimary
            )
        }
    }
}

@Composable
private fun TimerRing(seconds: Int, totalSeconds: Int, isRunning: Boolean) {
    val progress = if (totalSeconds > 0) seconds.toFloat() / totalSeconds else 1f
    val minutes = seconds / 60
    val secs = seconds % 60

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isRunning) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
        Canvas(modifier = Modifier.size(240.dp)) {
            drawCircle(color = TealPrimary.copy(alpha = glowAlpha * 0.1f), radius = size.minDimension / 2)
            drawArc(color = NavyLight, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 12f, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.sweepGradient(colors = listOf(TealGlow, TealPrimary, TealGlow)),
                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(String.format("%02d:%02d", minutes, secs), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = TealPrimary, letterSpacing = 4.sp)
            if (isRunning) Text("remaining", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun TimerControls(isRunning: Boolean, onToggle: () -> Unit, onReset: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.height(52.dp).width(120.dp),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, TextMuted),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset", fontWeight = FontWeight.Medium)
        }
        Button(
            onClick = onToggle,
            modifier = Modifier.height(52.dp).width(160.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) AmberAccent else TealPrimary,
                contentColor = NavyDark
            )
        ) {
            Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRunning) "Pause" else "Start", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DurationSelector(
    selected: Int,
    customMinutes: Int,
    isRunning: Boolean,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit
) {
    val presets = listOf(25, 30, 45, 60)

    Column {
        Text("Session Duration", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { min ->
                val isSelected = selected == min
                FilterChip(
                    selected = isSelected,
                    onClick = { if (!isRunning) onSelect(min) },
                    label = { Text("${min}m", fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = TealPrimary,
                        containerColor = SurfaceCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selectedBorderColor = TealPrimary, borderColor = Color.Transparent,
                        enabled = !isRunning, selected = isSelected
                    )
                )
            }
            // Custom chip
            val isCustom = selected == 0
            FilterChip(
                selected = isCustom,
                onClick = { if (!isRunning) onCustom() },
                label = {
                    Text(
                        if (isCustom) {
                            val h = customMinutes / 60; val m = customMinutes % 60
                            if (h > 0) "${h}h${m}m" else "${m}m"
                        } else "Custom",
                        fontWeight = FontWeight.Medium, fontSize = 12.sp
                    )
                },
                enabled = !isRunning,
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PurpleAccent.copy(alpha = 0.2f),
                    selectedLabelColor = PurpleAccent,
                    containerColor = SurfaceCard,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    selectedBorderColor = PurpleAccent, borderColor = Color.Transparent,
                    enabled = !isRunning, selected = isCustom
                )
            )
        }
    }
}

@Composable
private fun AmbientSoundSelector(selected: String, onSelect: (String) -> Unit) {
    data class Sound(val name: String, val icon: String)
    val sounds = listOf(Sound("Rain", "🌧️"), Sound("Forest", "🌲"), Sound("Lo-fi", "🎵"), Sound("Silence", "🔇"))

    Column {
        Text("Ambient Sound", fontSize = 14.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            sounds.forEach { sound ->
                val isSelected = selected == sound.name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(sound.name) }
                        .background(if (isSelected) TealPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) TealPrimary.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(sound.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(sound.name, fontSize = 11.sp, color = if (isSelected) TealPrimary else TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun TodayStats(sessions: Int, minutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$sessions", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                Text("Sessions Today", fontSize = 12.sp, color = TextSecondary)
            }
            Box(modifier = Modifier.width(1.dp).height(50.dp).background(TextMuted.copy(alpha = 0.3f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${minutes / 60}h ${minutes % 60}m", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                Text("Total Focus Time", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
