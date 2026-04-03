package com.example.myandroidapp.ui.screens.settings

import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.theme.LocalAccentColor
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val adaptive = rememberAdaptiveInfo()
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: PackageManager.NameNotFoundException) {
        "1.0.0"
    }

    Scaffold(containerColor = Color.Transparent) { contentPadding ->
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
            ) {
                // ── Header ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("About", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(Modifier.height(40.dp))

                // ── App Logo & Introduction ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.copy(0.8f), PurpleAccent.copy(0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📚", fontSize = 48.sp)
                    }
                    Spacer(Modifier.height(20.dp))

                    Text(
                        "StudyMate",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your AI-Powered Study Companion",
                        fontSize = 14.sp,
                        color = accent,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(20.dp))

                    // Introduction Text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, accent.copy(0.15f))
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "StudyMate is an all-in-one academic companion designed to help students organize their studies, track progress, and stay focused. "
                                    + "With powerful features like task management, a focus timer with DND mode, study analytics, an AI-powered chat assistant, "
                                    + "a community hub, and a built-in file library, StudyMate gives you everything you need to excel in your academic journey.",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))

                // ── Key Features ──
                Text("Key Features", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(14.dp))

                val features = listOf(
                    Triple(Icons.Default.Dashboard, "Smart Dashboard", "Overview of your study progress at a glance"),
                    Triple(Icons.Default.Timer, "Focus Timer", "Stay focused with DND mode & ambient sounds"),
                    Triple(Icons.Default.SmartToy, "AI Chat", "Get instant help powered by Gemini AI"),
                    Triple(Icons.Default.Forum, "Community", "Connect with fellow students"),
                    Triple(Icons.Default.FolderOpen, "File Library", "Manage PDFs, images, videos & documents"),
                    Triple(Icons.Default.CloudSync, "Cloud Backup", "Sync your data to Google Drive")
                )

                features.forEach { (icon, title, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, Color.Transparent)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(desc, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(24.dp))

                // ── App Version & Update ──
                Text("App Info", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, accent.copy(0.15f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Version", fontSize = 12.sp, color = TextMuted)
                                Text(versionName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GreenSuccess.copy(0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Latest ✓", fontSize = 12.sp, color = GreenSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "✅ You're on the latest version!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.copy(0.12f),
                                contentColor = accent
                            ),
                            border = BorderStroke(1.dp, accent.copy(0.3f))
                        ) {
                            Icon(Icons.Default.Update, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // ── Footer ──
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Made with ❤️ for Students", fontSize = 12.sp, color = TextMuted)
                    Text("© 2026 StudyMate", fontSize = 11.sp, color = TextMuted.copy(0.6f))
                }
            }
        }
    }
}
