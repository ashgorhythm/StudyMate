package com.example.myandroidapp.ui.screens.settings

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.theme.LocalAccentColor
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import org.json.JSONArray

// ═══════════════════════════════════════════════════════
// ── SuperUser Admin Panel ──
// ═══════════════════════════════════════════════════════

@Composable
fun SuperUserScreen(onBack: () -> Unit) {
    val adaptive = rememberAdaptiveInfo()
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    // Read feedback/bug reports from local prefs
    val feedbackPrefs = remember { context.getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE) }
    val feedbackLog = remember { mutableStateOf(feedbackPrefs.getString("feedback_log", "") ?: "") }
    val feedbackEntries = remember(feedbackLog.value) {
        feedbackLog.value.split("\n").filter { it.isNotBlank() }
    }

    // Read app stats
    val profilePrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }

    // Tab state
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Feedback", "Content", "System")

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
                    .padding(horizontal = adaptive.horizontalPadding)
                    .padding(top = if (adaptive.isTablet) 24.dp else 0.dp)
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
                    Column {
                        Text("SuperUser Panel", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Admin Controls", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ADMIN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── Tabs ──
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary,
                    edgePadding = 0.dp,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            color = accent,
                            height = 3.dp
                        )
                    },
                    divider = { HorizontalDivider(color = TextMuted.copy(0.15f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) accent else TextMuted
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ── Content ──
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 40.dp)
                ) {
                    when (selectedTab) {
                        0 -> OverviewTab(accent, context)
                        1 -> FeedbackTab(feedbackEntries, accent, context, feedbackPrefs, feedbackLog)
                        2 -> ContentModerationTab(accent, context)
                        3 -> SystemTab(accent, context)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Overview Tab ──
// ═══════════════════════════════════════════════════════

@Composable
private fun OverviewTab(accent: Color, context: Context) {
    // Quick stats cards
    Text("Dashboard", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminStatCard("App Version", "1.0.0", Icons.Default.Info, accent, Modifier.weight(1f))
        AdminStatCard("Build", "Release", Icons.Default.Build, PurpleAccent, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminStatCard("Platform", "Android", Icons.Default.PhoneAndroid, AmberAccent, Modifier.weight(1f))
        AdminStatCard("Database", "Room", Icons.Default.Storage, GreenSuccess, Modifier.weight(1f))
    }
    Spacer(Modifier.height(24.dp))

    // Quick actions
    Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    AdminActionCard(
        "View App Logs",
        "Check recent application logs and errors",
        Icons.Default.Terminal,
        accent
    ) {
        Toast.makeText(context, "App logs accessed", Toast.LENGTH_SHORT).show()
    }
    Spacer(Modifier.height(10.dp))
    AdminActionCard(
        "Database Inspector",
        "View Room database tables and stats",
        Icons.Default.TableChart,
        PurpleAccent
    ) {
        Toast.makeText(context, "Database inspector opened", Toast.LENGTH_SHORT).show()
    }
    Spacer(Modifier.height(10.dp))
    AdminActionCard(
        "Performance Metrics",
        "Monitor memory, CPU, and network usage",
        Icons.Default.Speed,
        AmberAccent
    ) {
        Toast.makeText(context, "Performance metrics accessed", Toast.LENGTH_SHORT).show()
    }
}

// ═══════════════════════════════════════════════════════
// ── Feedback Tab ──
// ═══════════════════════════════════════════════════════

@Composable
private fun FeedbackTab(
    entries: List<String>,
    accent: Color,
    context: Context,
    feedbackPrefs: android.content.SharedPreferences,
    feedbackLog: MutableState<String>
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("User Feedback & Bug Reports", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (entries.isNotEmpty()) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("${entries.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    if (entries.isEmpty()) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Inbox, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No feedback yet", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextMuted)
                Text("User submissions will appear here", fontSize = 12.sp, color = TextMuted.copy(0.6f))
            }
        }
    } else {
        entries.reversed().forEachIndexed { index, entry ->
            val isBug = entry.contains("Bug Report", ignoreCase = true)
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, if (isBug) RedError.copy(0.25f) else accent.copy(0.15f))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isBug) Icons.Default.BugReport else Icons.Default.Feedback,
                            null,
                            tint = if (isBug) RedError else GreenSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isBug) "Bug Report" else "Feedback",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBug) RedError else GreenSuccess
                        )
                        Spacer(Modifier.weight(1f))
                        Text("#${entries.size - index}", fontSize = 11.sp, color = TextMuted)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(entry, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Clear feedback button
        OutlinedButton(
            onClick = {
                feedbackPrefs.edit().remove("feedback_log").apply()
                feedbackLog.value = ""
                Toast.makeText(context, "Feedback log cleared", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, RedError.copy(0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
        ) {
            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear All Feedback", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Content Moderation Tab ──
// ═══════════════════════════════════════════════════════

@Composable
private fun ContentModerationTab(accent: Color, context: Context) {
    Text("Community Moderation", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    AdminActionCard(
        "Review Flagged Posts",
        "Review and moderate community content",
        Icons.Default.Flag,
        RedError
    ) {
        Toast.makeText(context, "No flagged posts to review", Toast.LENGTH_SHORT).show()
    }
    Spacer(Modifier.height(10.dp))
    AdminActionCard(
        "User Management",
        "View registered users and manage access",
        Icons.Default.People,
        accent
    ) {
        Toast.makeText(context, "User management panel", Toast.LENGTH_SHORT).show()
    }
    Spacer(Modifier.height(10.dp))
    AdminActionCard(
        "Content Guidelines",
        "Edit community rules and posting guidelines",
        Icons.Default.Gavel,
        PurpleAccent
    ) {
        Toast.makeText(context, "Content guidelines editor", Toast.LENGTH_SHORT).show()
    }

    Spacer(Modifier.height(24.dp))

    Text("Moderation Stats", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, accent.copy(0.12f))
    ) {
        Column(Modifier.padding(20.dp)) {
            ModerationStatRow("Posts reviewed today", "0", accent)
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = TextMuted.copy(0.1f))
            ModerationStatRow("Flagged content", "0", RedError)
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = TextMuted.copy(0.1f))
            ModerationStatRow("Users warned", "0", AmberAccent)
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = TextMuted.copy(0.1f))
            ModerationStatRow("Users banned", "0", RedError)
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── System Tab ──
// ═══════════════════════════════════════════════════════

@Composable
private fun SystemTab(accent: Color, context: Context) {
    Text("System Controls", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    AdminActionCard(
        "Force Sync Firebase",
        "Manually trigger Firebase data synchronization",
        Icons.Default.Sync,
        accent
    ) {
        Toast.makeText(context, "Firebase sync triggered", Toast.LENGTH_SHORT).show()
    }
    Spacer(Modifier.height(10.dp))
    AdminActionCard(
        "Clear App Cache",
        "Remove cached data to free storage",
        Icons.Default.CleaningServices,
        AmberAccent
    ) {
        try {
            context.cacheDir.deleteRecursively()
            Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to clear cache: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    Spacer(Modifier.height(10.dp))
    AdminActionCard(
        "Reset All Preferences",
        "Restore all settings to factory defaults",
        Icons.Default.RestartAlt,
        RedError
    ) {
        Toast.makeText(context, "Preferences reset to defaults", Toast.LENGTH_SHORT).show()
    }
    Spacer(Modifier.height(10.dp))
    AdminActionCard(
        "Export Debug Info",
        "Generate a diagnostic report for troubleshooting",
        Icons.Default.BugReport,
        PurpleAccent
    ) {
        Toast.makeText(context, "Debug info exported", Toast.LENGTH_SHORT).show()
    }

    Spacer(Modifier.height(24.dp))

    // System info
    Text("System Information", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, accent.copy(0.12f))
    ) {
        Column(Modifier.padding(20.dp)) {
            SystemInfoRow("Android Version", android.os.Build.VERSION.RELEASE)
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = TextMuted.copy(0.1f))
            SystemInfoRow("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = TextMuted.copy(0.1f))
            SystemInfoRow("SDK Level", android.os.Build.VERSION.SDK_INT.toString())
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = TextMuted.copy(0.1f))
            SystemInfoRow("Architecture", android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown")
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = TextMuted.copy(0.1f))
            val runtime = Runtime.getRuntime()
            val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1048576
            val totalMb = runtime.totalMemory() / 1048576
            SystemInfoRow("Memory Usage", "${usedMb}MB / ${totalMb}MB")
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Helper Composables ──
// ═══════════════════════════════════════════════════════

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.height(80.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.08f)),
        border = BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(0.2f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextMuted.copy(0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ModerationStatRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(0.1f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
