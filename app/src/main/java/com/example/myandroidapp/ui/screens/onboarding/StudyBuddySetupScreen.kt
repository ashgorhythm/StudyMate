package com.example.myandroidapp.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import com.example.myandroidapp.util.StudyBuddyFolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────
// StudyBuddy Folder Setup Screen
// Shown once right after onboarding completes.
// ─────────────────────────────────────────────────────────

@Composable
fun StudyBuddySetupScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val adaptive = rememberAdaptiveInfo()
    val scope = rememberCoroutineScope()

    // Background animated blob rotation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing)),
        label = "rot"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    // Setup states
    var folderCreated by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var setupComplete by remember { mutableStateOf(false) }
    var subfolderCount by remember { mutableStateOf(0) }

    // Check existing state on launch
    LaunchedEffect(Unit) {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val folder = File(documentsDir, "StudyBuddy")
        folderCreated = folder.exists()
        if (folderCreated) {
            subfolderCount = folder.listFiles()?.count { it.isDirectory } ?: 0
        }
        permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
        setupComplete = folderCreated && permissionGranted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF060915), Color(0xFF0D1225), Color(0xFF060915)))
            )
            .drawBehind {
                val cx = size.width / 2f
                val cy = size.height * 0.25f
                val r = size.minDimension * 0.55f
                listOf(
                    Triple(TealPrimary, 0f, 0.18f),
                    Triple(PurpleAccent, 120f, 0.12f),
                    Triple(PinkAccent, 240f, 0.10f)
                ).forEach { (color, offset, alpha) ->
                    val angle = Math.toRadians((rotation + offset).toDouble())
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = r * 0.5f,
                        center = Offset(
                            cx + (r * 0.4f * cos(angle)).toFloat(),
                            cy + (r * 0.25f * sin(angle)).toFloat()
                        )
                    )
                }
            }
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
                .padding(top = if (adaptive.isTablet) 28.dp else 56.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Large Animated Icon ──
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .blur(50.dp)
                        .alpha(pulse * 0.8f)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (setupComplete) GreenSuccess.copy(0.7f) else TealPrimary.copy(0.7f),
                                    PurpleAccent.copy(0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(0.07f))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    if (setupComplete) GreenSuccess.copy(0.5f) else TealPrimary.copy(0.5f),
                                    PurpleAccent.copy(0.3f),
                                    Color.White.copy(0.05f)
                                )
                            ),
                            RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = setupComplete,
                        transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                        label = "icon"
                    ) { complete ->
                        Icon(
                            if (complete) Icons.Default.CheckCircle else Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = if (complete) GreenSuccess else TealPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Title ──
            AnimatedContent(
                targetState = setupComplete,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "title"
            ) { complete ->
                Text(
                    if (complete) "You're All Set! 🎉" else "Quick Setup ✨",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (setupComplete) "Your StudyBuddy folder is ready. All your study files will be organized here."
                else "We'll create a folder to organize your study files. It takes just one tap!",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))

            // ── Step Cards (interactive checklist) ──
            StepChecklistItem(
                stepNumber = 1,
                icon = Icons.Default.CreateNewFolder,
                accentColor = TealPrimary,
                title = "Create StudyBuddy Folder",
                description = "Documents → StudyBuddy with subfolders for PDFs, Notes, Images, and Videos",
                isCompleted = folderCreated,
                isLoading = isCreating,
                actionLabel = if (folderCreated) "Created ✓" else "Create Now",
                onAction = {
                    if (!folderCreated && !isCreating) {
                        scope.launch {
                            isCreating = true
                            delay(600) // Brief animation
                            try {
                                StudyBuddyFolder.getOrCreate(context)
                                folderCreated = true
                                subfolderCount = 4
                                if (permissionGranted) setupComplete = true
                            } catch (_: Exception) { }
                            isCreating = false
                        }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // Step 2: Permission (only on Android 11+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                StepChecklistItem(
                    stepNumber = 2,
                    icon = Icons.Default.Security,
                    accentColor = PurpleAccent,
                    title = "Grant File Access",
                    description = "Allow the app to manage your study files. Your files never leave your device.",
                    isCompleted = permissionGranted,
                    actionLabel = if (permissionGranted) "Granted ✓" else "Grant Permission",
                    onAction = {
                        if (!permissionGranted) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            // Folder structure preview
            AnimatedVisibility(
                visible = folderCreated,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FolderPreviewCard(subfolderCount)
            }

            Spacer(Modifier.height(24.dp))

            // ── Privacy note ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenSuccess.copy(0.06f))
                    .border(1.dp, GreenSuccess.copy(0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("100% Private & Offline", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GreenSuccess)
                    Text(
                        "Your files stay on your device. Nothing is uploaded anywhere.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Continue Button ──
            Button(
                onClick = {
                    // Auto-create folder if user hasn't yet
                    if (!folderCreated) {
                        try { StudyBuddyFolder.getOrCreate(context) } catch (_: Exception) { }
                    }
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                if (setupComplete) listOf(GreenSuccess, TealPrimary) else listOf(TealPrimary, PurpleAccent.copy(0.8f))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (setupComplete) "Let's Go!" else "Continue Anyway",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (setupComplete) Icons.Default.RocketLaunch else Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!setupComplete) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "You can set this up later from the Library tab",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Step Checklist Item — Interactive card
// ─────────────────────────────────────────────────────────

@Composable
private fun StepChecklistItem(
    stepNumber: Int,
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String,
    isCompleted: Boolean,
    isLoading: Boolean = false,
    actionLabel: String,
    onAction: () -> Unit
) {
    val bgColor = if (isCompleted) GreenSuccess.copy(0.06f) else accentColor.copy(0.04f)
    val borderColor = if (isCompleted) GreenSuccess.copy(0.25f) else accentColor.copy(0.15f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCompleted && !isLoading) { onAction() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) GreenSuccess.copy(0.15f)
                        else accentColor.copy(0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = accentColor
                    )
                } else if (isCompleted) {
                    Icon(Icons.Default.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(28.dp))
                } else {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) GreenSuccess else TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            if (!isCompleted && !isLoading) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                }
            } else if (isCompleted) {
                Text(
                    actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenSuccess
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Folder Preview Card — Shows created folder structure
// ─────────────────────────────────────────────────────────

@Composable
private fun FolderPreviewCard(subfolderCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Folder Structure", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Spacer(Modifier.height(12.dp))

            // Tree visualization
            val folders = listOf(
                Triple("📁", "StudyBuddy", "Root folder"),
                Triple("📄", "PDFs", "Textbooks, papers"),
                Triple("📝", "Notes", "Written notes, docs"),
                Triple("🖼️", "Images", "Screenshots, diagrams"),
                Triple("🎬", "Videos", "Lectures, tutorials")
            )
            folders.forEachIndexed { index, (emoji, name, desc) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = if (index == 0) 0.dp else 20.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (index > 0) {
                        Text("└ ", fontSize = 13.sp, color = TextMuted)
                    }
                    Text(emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("· $desc", fontSize = 11.sp, color = TextMuted)
                }
            }
        }
    }
}
