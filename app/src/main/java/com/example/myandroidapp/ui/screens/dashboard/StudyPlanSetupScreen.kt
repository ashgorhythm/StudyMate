package com.example.myandroidapp.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanSetupScreen(onBack: () -> Unit) {
    val adaptive = rememberAdaptiveInfo()

    // Weekly plan state
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val dayAbbr = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember { mutableStateOf(setOf(0, 1, 2, 3, 4)) } // Mon-Fri default
    var hoursPerDay by remember { mutableStateOf(3f) }
    var focusGoal by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSaved) {
        if (showSaved) {
            snackbarHostState.showSnackbar("✅ Study plan saved!")
            showSaved = false
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
                    Text("Weekly Study Plan", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(Modifier.height(24.dp))

                // ── Introduction ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleAccent.copy(0.08f)),
                    border = BorderStroke(1.dp, PurpleAccent.copy(0.2f))
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EventNote, null, tint = PurpleAccent, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Plan Your Week", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "Set your study schedule to stay consistent and build momentum.",
                                fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))

                // ── Select Study Days ──
                Text("Study Days", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Select which days you plan to study", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayAbbr.forEachIndexed { index, abbr ->
                        val isSelected = index in selectedDays
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - index else selectedDays + index
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TealPrimary.copy(0.15f) else SurfaceCard
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) TealPrimary.copy(0.5f) else Color.Transparent
                            )
                        ) {
                            Column(
                                Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    abbr,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TealPrimary else TextMuted
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))

                // ── Hours Per Day ──
                Text("Study Hours per Day", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, AmberAccent.copy(0.15f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target hours", fontSize = 14.sp, color = TextSecondary)
                            Text(
                                "${hoursPerDay.toInt()}h ${((hoursPerDay % 1f) * 60).toInt()}m",
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmberAccent
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Slider(
                            value = hoursPerDay,
                            onValueChange = { hoursPerDay = it },
                            valueRange = 0.5f..8f,
                            steps = 15,
                            colors = SliderDefaults.colors(
                                thumbColor = AmberAccent,
                                activeTrackColor = AmberAccent,
                                inactiveTrackColor = NavyLight
                            )
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("30m", fontSize = 11.sp, color = TextMuted)
                            Text("8h", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))

                // ── Focus Goal ──
                Text("Focus Goal (optional)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = focusGoal,
                    onValueChange = { focusGoal = it },
                    placeholder = { Text("e.g. Prepare for midterms", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        cursorColor = TealPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Flag, null, tint = GreenSuccess) }
                )
                Spacer(Modifier.height(32.dp))

                // ── Weekly Summary ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(0.06f)),
                    border = BorderStroke(1.dp, TealPrimary.copy(0.2f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Weekly Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${selectedDays.size}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                Text("Study Days", fontSize = 11.sp, color = TextMuted)
                            }
                            Box(
                                Modifier.width(1.dp).height(44.dp)
                                    .background(TextMuted.copy(0.2f))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val totalHours = selectedDays.size * hoursPerDay
                                Text(
                                    "${totalHours.toInt()}h",
                                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PurpleAccent
                                )
                                Text("Total/Week", fontSize = 11.sp, color = TextMuted)
                            }
                            Box(
                                Modifier.width(1.dp).height(44.dp)
                                    .background(TextMuted.copy(0.2f))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${hoursPerDay.toInt()}h",
                                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AmberAccent
                                )
                                Text("Per Day", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))

                // ── Save Button ──
                Button(
                    onClick = { showSaved = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = NavyDark
                    ),
                    enabled = selectedDays.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Save Study Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(16.dp))

                // ── Reset Button ──
                OutlinedButton(
                    onClick = {
                        selectedDays = setOf(0, 1, 2, 3, 4)
                        hoursPerDay = 3f
                        focusGoal = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TextMuted.copy(0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.RestartAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Defaults")
                }
            }
        }
    }
}
