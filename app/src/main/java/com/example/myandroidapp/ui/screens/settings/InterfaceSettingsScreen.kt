package com.example.myandroidapp.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
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
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSettingsScreen(onBack: () -> Unit) {
    val adaptive = rememberAdaptiveInfo()

    var themeMode by remember { mutableStateOf("System") } // System, Light, Dark
    var reduceAnimations by remember { mutableStateOf(false) }
    var accentColor by remember { mutableStateOf(TealPrimary) }
    var textScale by remember { mutableStateOf(1f) }

    Scaffold(
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
                    .padding(top = if (adaptive.isTablet) 24.dp else 48.dp, bottom = 40.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Interface & Personalization", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(Modifier.height(32.dp))

                // Theme Mode
                Text("Theme Mode", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val modes = listOf("System", "Light", "Dark")
                    modes.forEach { mode ->
                        val selected = themeMode == mode
                        ChoiceChip(
                            label = mode,
                            selected = selected,
                            onClick = { themeMode = mode },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
                // Accent Color
                Text("Accent Color", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, TealPrimary.copy(0.12f))
                ) {
                    Row(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf(TealPrimary, PurpleAccent, AmberAccent, PinkAccent, GreenSuccess).forEach { color ->
                            ColorCircle(color, selected = accentColor == color, onClick = { accentColor = color })
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                // Layout & Typography
                Text("Layout & Typography", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                SettingsToggleCard(
                    title = "Text Size Scale",
                    subtitle = "Adjust the app's overall font size",
                    icon = Icons.Default.FormatSize,
                    accentColor = PurpleAccent,
                    content = {
                        Slider(
                            value = textScale,
                            onValueChange = { textScale = it },
                            valueRange = 0.8f..1.3f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = TealPrimary,
                                activeTrackColor = TealPrimary,
                                inactiveTrackColor = NavyLight
                            )
                        )
                    }
                )

                Spacer(Modifier.height(32.dp))
                // Motion & Animations
                Text("Motion", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                SettingsToggleCard(
                    title = "Reduce Animations",
                    subtitle = "Minimize movement and transitions across the app",
                    icon = Icons.Default.Animation,
                    accentColor = AmberAccent,
                    trailing = {
                        Switch(
                            checked = reduceAnimations,
                            onCheckedChange = { reduceAnimations = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NavyDark,
                                checkedTrackColor = TealPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = NavyLight
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) TealPrimary.copy(alpha = 0.15f) else SurfaceCard
        ),
        border = BorderStroke(1.dp, if (selected) TealPrimary else TealPrimary.copy(0.1f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) TealPrimary else TextPrimary)
        }
    }
}

@Composable
private fun ColorCircle(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, "Selected", tint = NavyDark, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, accentColor.copy(0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(subtitle, fontSize = 12.sp, color = TextSecondary)
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
            if (content != null) {
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}
