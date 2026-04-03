package com.example.myandroidapp.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.theme.LocalAccentColor
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────

data class ProfileUiState(
    val studentName: String = "Student",
    val profileImageUri: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(private val context: android.content.Context) : ViewModel() {

    private val prefs = UserPreferences(context)
    private val sharedPrefs = context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.studentName.collect { name ->
                _uiState.update { it.copy(studentName = name) }
            }
        }
        val savedUri = sharedPrefs.getString("profile_image_uri", null)
        _uiState.update { it.copy(profileImageUri = savedUri) }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            prefs.updateName(name.ifBlank { "Student" })
            _uiState.update { it.copy(isSaving = false, saveSuccess = true, studentName = name.ifBlank { "Student" }) }
        }
    }

    fun updateProfileImage(uri: String) {
        sharedPrefs.edit().putString("profile_image_uri", uri).apply()
        _uiState.update { it.copy(profileImageUri = uri) }
    }

    fun clearProfileImage() {
        sharedPrefs.edit().remove("profile_image_uri").apply()
        _uiState.update { it.copy(profileImageUri = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(saveSuccess = false, errorMessage = null) }
    }
}

class ProfileViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(context) as T
    }
}

// ─────────────────────────────────────────────────────────
//  Screen — deduplicated: only avatar click & dialog for photo,
//  only one name edit via the Profile Details card
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptive = rememberAdaptiveInfo()
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    var showNameDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var showImageOptions by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("✅ Profile updated!")
            viewModel.clearMessages()
        }
    }

    // Image picker from gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.updateProfileImage(it.toString())
        }
    }

    // Name dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Change Name", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your new display name:", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = { Text("Your name", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = accent) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = TextMuted.copy(0.3f),
                            focusedContainerColor = NavyDark,
                            unfocusedContainerColor = NavyDark,
                            cursorColor = accent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateName(editName)
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = NavyDark),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = NavyMedium,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Image options dialog
    if (showImageOptions) {
        Dialog(onDismissRequest = { showImageOptions = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NavyMedium),
                border = BorderStroke(1.dp, accent.copy(0.2f))
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Profile Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(20.dp))

                    ImageOptionRow(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Choose from Gallery",
                        color = accent
                    ) {
                        showImageOptions = false
                        imagePickerLauncher.launch("image/*")
                    }
                    Spacer(Modifier.height(12.dp))
                    if (uiState.profileImageUri != null) {
                        ImageOptionRow(
                            icon = Icons.Default.Delete,
                            label = "Remove Photo",
                            color = RedError
                        ) {
                            viewModel.clearProfileImage()
                            showImageOptions = false
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    TextButton(onClick = { showImageOptions = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
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
                    .padding(top = if (adaptive.isTablet) 24.dp else 0.dp, bottom = 40.dp)
                    .align(Alignment.TopCenter)
            ) {
                // ── Header — fixed to top ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("My Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(Modifier.height(32.dp))

                // ── Avatar section — single click opens image dialog ──
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScallopedAvatar(
                        name = uiState.studentName,
                        imageUri = uiState.profileImageUri,
                        size = 110,
                        onClick = { showImageOptions = true }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(uiState.studentName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Student", fontSize = 13.sp, color = TextMuted)
                }

                Spacer(Modifier.height(36.dp))

                // ── Profile Info Section — single edit each ──
                Text("Profile Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(14.dp))

                ProfileInfoCard(
                    label = "Display Name",
                    value = uiState.studentName,
                    icon = Icons.Default.Person,
                    accentColor = accent,
                    onEdit = {
                        editName = uiState.studentName
                        showNameDialog = true
                    }
                )
                Spacer(Modifier.height(10.dp))

                ProfileInfoCard(
                    label = "Profile Photo",
                    value = if (uiState.profileImageUri != null) "Custom photo set" else "Using initials avatar",
                    icon = Icons.Default.Image,
                    accentColor = PurpleAccent,
                    onEdit = { showImageOptions = true }
                )
                Spacer(Modifier.height(24.dp))

                // ── Stats card ──
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, accent.copy(0.12f))
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(TealPrimary.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Profile is device-local", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(
                                "Use Sync & Data Backup in Settings to save your profile across devices.",
                                fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Scalloped avatar with optional image
// ─────────────────────────────────────────────────────────

@Composable
fun ScallopedAvatar(
    name: String,
    imageUri: String?,
    size: Int = 110,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "profileScallop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
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

    Box(
        modifier = Modifier
            .size(size.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size.dp)
                .rotate(rotation)
        ) {
            val cx = this.size.minDimension / 2f
            val baseRadius = cx * pulse
            val scallops = 12
            val amplitude = cx * 0.10f
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
                        TealPrimary.copy(alpha = 0.7f),
                        PurpleAccent.copy(alpha = 0.55f),
                        TealPrimary.copy(alpha = 0.4f),
                        PurpleAccent.copy(alpha = 0.7f),
                        TealPrimary.copy(alpha = 0.7f)
                    )
                )
            )
        }

        // Inner avatar
        Box(
            modifier = Modifier
                .size((size * 0.78f).dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(imageUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.linearGradient(listOf(TealPrimary, PurpleAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name.take(1).uppercase(),
                        fontSize = (size * 0.3f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Camera overlay badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(TealPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = NavyDark, modifier = Modifier.size(14.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Helper Composables
// ─────────────────────────────────────────────────────────

@Composable
private fun ProfileInfoCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(0.15f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = TextMuted)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = accentColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ImageOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        border = BorderStroke(1.dp, color.copy(0.25f))
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted.copy(0.5f))
        }
    }
}
