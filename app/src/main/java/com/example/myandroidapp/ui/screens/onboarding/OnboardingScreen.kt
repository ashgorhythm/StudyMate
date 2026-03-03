package com.example.myandroidapp.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.ui.theme.PinkAccent
import com.example.myandroidapp.ui.theme.PurpleAccent
import com.example.myandroidapp.ui.theme.TealPrimary
import com.example.myandroidapp.ui.theme.TextMuted
import com.example.myandroidapp.ui.theme.TextPrimary
import com.example.myandroidapp.ui.theme.TextSecondary
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════
// ── Data ──
// ═══════════════════════════════════════════════════════

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val icon: ImageVector,
    val accentColor: Color,
    val secondaryColor: Color,
    val features: List<Pair<String, String>> = emptyList() // emoji to label
)

private val pages = listOf(
    OnboardingPage(
        title = "Master Your\nStudies",
        subtitle = "Your AI-powered companion for smarter studying, better focus, and higher grades",
        emoji = "📚",
        icon = Icons.Default.School,
        accentColor = TealPrimary,
        secondaryColor = PurpleAccent,
        features = listOf("📊" to "Track", "⏱️" to "Focus", "🤖" to "AI Help")
    ),
    OnboardingPage(
        title = "AI-Powered\nLearning",
        subtitle = "Get instant summaries, quizzes, study plans, and concept explanations powered by Gemini AI",
        emoji = "🧠",
        icon = Icons.Default.AutoAwesome,
        accentColor = PurpleAccent,
        secondaryColor = PinkAccent,
        features = listOf("📝" to "Summarize", "🧠" to "Quiz", "📅" to "Plan")
    ),
    OnboardingPage(
        title = "Ready to\nExcel?",
        subtitle = "Track progress, stay focused, and ace your exams with your personal study companion",
        emoji = "🚀",
        icon = Icons.Default.RocketLaunch,
        accentColor = PinkAccent,
        secondaryColor = TealPrimary,
        features = listOf("🎯" to "Goals", "📈" to "Progress", "🏆" to "Achieve")
    )
)

// ═══════════════════════════════════════════════════════
// ── Main Screen ──
// ═══════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit // passes the student name
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    var studentName by remember { mutableStateOf("") }
    val adaptive = rememberAdaptiveInfo()

    // Background animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "bgRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E21),
                        Color(0xFF0D1025),
                        Color(0xFF0A0E21)
                    )
                )
            )
            .drawBehind { drawParticles(bgOffset) }
    ) {
        // ── Skip Button ──
        AnimatedVisibility(
            visible = pagerState.currentPage < pages.size - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 20.dp)
        ) {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pages.size - 1)
                    }
                }
            ) {
                Text(
                    "Skip",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(100.dp))

            // ── Pager ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                if (adaptive.isTablet) {
                    TabletOnboardingPageContent(
                        page = pages[pageIndex],
                        isLastPage = pageIndex == pages.size - 1,
                        studentName = studentName,
                        onNameChange = { studentName = it }
                    )
                } else {
                    OnboardingPageContent(
                        page = pages[pageIndex],
                        isLastPage = pageIndex == pages.size - 1,
                        studentName = studentName,
                        onNameChange = { studentName = it }
                    )
                }
            }

            // ── Page Indicators ──
            Row(
                Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.forEachIndexed { index, page ->
                    val isActive = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isActive) 32.dp else 10.dp,
                        animationSpec = spring(dampingRatio = 0.7f),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(10.dp)
                            .width(width)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (isActive) page.accentColor
                                else TextMuted.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // ── Bottom Button ──
            val currentPage = pages[pagerState.currentPage]
            val isLastPage = pagerState.currentPage == pages.size - 1

            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete(studentName.ifBlank { "Student" })
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp)
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(currentPage.accentColor, currentPage.secondaryColor)
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isLastPage) "Get Started" else "Next",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!isLastPage) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.RocketLaunch,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Page Content ──
// ═══════════════════════════════════════════════════════

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isLastPage: Boolean,
    studentName: String,
    onNameChange: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "page")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -12f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Glassmorphism Illustration Card ──
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(y = floatOffset.dp)
        ) {
            // Glow behind the card
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .blur(60.dp)
                    .alpha(glowAlpha)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                page.accentColor.copy(alpha = 0.5f),
                                page.secondaryColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            // Glassmorphism card
            Card(
                modifier = Modifier
                    .size(200.dp)
                    .rotate(rotationAngle),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.06f)
                ),
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.2f),
                            page.accentColor.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Background icon (subtle)
                    Icon(
                        page.icon, null,
                        tint = page.accentColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(140.dp)
                    )
                    // Main emoji
                    Text(
                        page.emoji,
                        fontSize = 72.sp
                    )
                }
            }

            // Orbiting accent dots
            val orbitAngle by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
                label = "orbit"
            )
            listOf(0f, 120f, 240f).forEach { offset ->
                val angle = orbitAngle + offset
                val x = (110 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                val y = (110 * sin(Math.toRadians(angle.toDouble()))).toFloat()
                Box(
                    modifier = Modifier
                        .offset(x = x.dp, y = y.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (offset) {
                                0f -> page.accentColor
                                120f -> page.secondaryColor
                                else -> Color.White.copy(alpha = 0.4f)
                            }
                        )
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // ── Title ──
        Text(
            page.title,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        Spacer(Modifier.height(14.dp))

        // ── Subtitle ──
        Text(
            page.subtitle,
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(24.dp))

        // ── Feature Pills ──
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            page.features.forEach { (emoji, label) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = page.accentColor.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, page.accentColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = page.accentColor
                        )
                    }
                }
            }
        }

        // ── Name Input (Last Page Only) ──
        if (isLastPage) {
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = studentName,
                onValueChange = onNameChange,
                placeholder = {
                    Text(
                        "What should we call you?",
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = PinkAccent,
                        modifier = Modifier.size(22.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkAccent,
                    unfocusedBorderColor = TextMuted.copy(0.3f),
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                    cursorColor = PinkAccent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// ── Background Particles ──
// ═══════════════════════════════════════════════════════

private data class Particle(val x: Float, val y: Float, val size: Float, val alpha: Float)

private val particles = List(35) {
    Particle(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        size = Random.nextFloat() * 3f + 1f,
        alpha = Random.nextFloat() * 0.5f + 0.1f
    )
}

private fun DrawScope.drawParticles(animOffset: Float) {
    particles.forEach { p ->
        val offsetX = sin(animOffset * 0.01 + p.x * 10).toFloat() * 10f
        val offsetY = sin(animOffset * 0.008 + p.y * 8).toFloat() * 8f
        drawCircle(
            color = Color.White.copy(alpha = p.alpha),
            radius = p.size.dp.toPx(),
            center = Offset(
                x = p.x * size.width + offsetX.dp.toPx(),
                y = p.y * size.height + offsetY.dp.toPx()
            )
        )
    }
}

// ═══════════════════════════════════════════════════════
// ── Tablet Page Content (Side by Side) ──
// ═══════════════════════════════════════════════════════

@Composable
private fun TabletOnboardingPageContent(
    page: OnboardingPage,
    isLastPage: Boolean,
    studentName: String,
    onNameChange: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tabletPage")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "rotate"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // ── Left: Glassmorphism Illustration ──
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .offset(y = floatOffset.dp)
        ) {
            // Glow
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .blur(60.dp)
                    .alpha(glowAlpha)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                page.accentColor.copy(alpha = 0.5f),
                                page.secondaryColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Card
            Card(
                modifier = Modifier
                    .size(260.dp)
                    .rotate(rotationAngle),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.2f),
                            page.accentColor.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(page.icon, null, tint = page.accentColor.copy(alpha = 0.1f), modifier = Modifier.size(180.dp))
                    Text(page.emoji, fontSize = 96.sp)
                }
            }

            // Orbiting dots
            val orbitAngle by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
                label = "orbit"
            )
            listOf(0f, 120f, 240f).forEach { offset ->
                val angle = orbitAngle + offset
                val x = (140 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                val y = (140 * sin(Math.toRadians(angle.toDouble()))).toFloat()
                Box(
                    modifier = Modifier
                        .offset(x = x.dp, y = y.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (offset) {
                                0f -> page.accentColor
                                120f -> page.secondaryColor
                                else -> Color.White.copy(alpha = 0.4f)
                            }
                        )
                )
            }
        }

        Spacer(Modifier.width(32.dp))

        // ── Right: Text Content ──
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                page.title,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                lineHeight = 48.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                page.subtitle,
                fontSize = 17.sp,
                color = TextSecondary,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(28.dp))

            // Feature pills
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                page.features.forEach { (emoji, label) ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = page.accentColor.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, page.accentColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = page.accentColor)
                        }
                    }
                }
            }

            // Name input on last page
            if (isLastPage) {
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = studentName,
                    onValueChange = onNameChange,
                    placeholder = { Text("What should we call you?", color = TextMuted, fontSize = 15.sp) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = PinkAccent, modifier = Modifier.size(22.dp)) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkAccent,
                        unfocusedBorderColor = TextMuted.copy(0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                        cursorColor = PinkAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }
        }
    }
}
