package com.example.myandroidapp.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
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

    // Check if we already have MANAGE_EXTERNAL_STORAGE on Android 11+
    val hasFullAccess = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF060915), Color(0xFF0D1225), Color(0xFF060915)))
            )
            .drawBehind {
                // Animated glowing blobs
                val cx = size.width / 2f
                val cy = size.height * 0.3f
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
                .padding(top = if (adaptive.isTablet) 28.dp else 72.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Folder Icon with glow ──
            Box(contentAlignment = Alignment.Center) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .blur(50.dp)
                        .alpha(pulse * 0.8f)
                        .background(
                            Brush.radialGradient(
                                listOf(TealPrimary.copy(0.7f), PurpleAccent.copy(0.3f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
                // Glassmorphism card
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(0.07f))
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(TealPrimary.copy(0.5f), PurpleAccent.copy(0.3f), Color.White.copy(0.05f))
                            ),
                            RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Title ──
            Text(
                "One Last Step! 🎉",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Create your StudyBuddy folder so the app can manage your study files.",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            // ── Step 1: Create folder ──
            SetupStepCard(
                step = "1",
                icon = Icons.Default.CreateNewFolder,
                iconTint = TealPrimary,
                title = "Create the Folder",
                body = "Open your file manager and go to:\n\nInternal Storage → Documents\n\nCreate a folder named exactly:",
                highlight = "StudyBuddy"
            )

            Spacer(Modifier.height(16.dp))

            // ── Step 2: Subfolders ──
            SetupStepCard(
                step = "2",
                icon = Icons.Default.AccountTree,
                iconTint = PurpleAccent,
                title = "Optional Subfolders",
                body = "Organize your files by creating sub-folders inside StudyBuddy:",
                chips = listOf("📄 PDFs", "📝 Notes", "🖼️ Images", "🎬 Videos")
            )

            Spacer(Modifier.height(16.dp))

            // ── Step 3: Permission ──
            SetupStepCard(
                step = "3",
                icon = Icons.Default.Security,
                iconTint = PinkAccent,
                title = "Grant Full Access",
                body = "StudyMate needs permission to read & write files in the StudyBuddy folder so you can view, rename, and delete your study materials right from the app.",
                highlight = null,
                chips = null,
                note = "Your files never leave your device."
            )

            Spacer(Modifier.height(24.dp))

            // ── Grant Permission Button (Android 11+) ──
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasFullAccess) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(PurpleAccent, PinkAccent)),
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Grant File Access",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Continue / Done Button ──
            Button(
                onClick = onContinue,
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
                            Brush.horizontalGradient(listOf(TealPrimary, PurpleAccent.copy(0.8f))),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "I'm Ready – Let's Go!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.RocketLaunch, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "You can always add files to StudyBuddy later from the Library tab.",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
// Reusable Step Card
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupStepCard(
    step: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    body: String,
    highlight: String? = null,
    chips: List<String>? = null,
    note: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(iconTint.copy(0.25f), Color.White.copy(0.05f)))
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Step number badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(step, color = iconTint, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(Modifier.height(12.dp))

            Text(body, fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)

            // Highlighted folder name pill
            if (highlight != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(0.1f))
                        .border(1.dp, iconTint.copy(0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        highlight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTint,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Chips (subfolder names)
            if (!chips.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(iconTint.copy(0.08f))
                                .border(1.dp, iconTint.copy(0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(chip, fontSize = 11.sp, color = iconTint, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Privacy note
            if (note != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = GreenSuccess, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(note, fontSize = 11.sp, color = GreenSuccess)
                }
            }
        }
    }
}
