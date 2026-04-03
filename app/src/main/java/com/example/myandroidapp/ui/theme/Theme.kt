package com.example.myandroidapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.myandroidapp.data.preferences.UserPreferences

// ═══════════════════════════════════════════════════════
// ── CompositionLocals for dynamic theming ──
// ═══════════════════════════════════════════════════════

/**
 * Provides the user-selected accent color throughout the entire
 * composition tree so screens can use it instead of hardcoded TealPrimary.
 */
val LocalAccentColor = compositionLocalOf { TealPrimary }

/**
 * Provides whether the app is currently in dark mode.
 * Screens can use this to choose the correct surface/background colors.
 */
val LocalIsDarkTheme = compositionLocalOf { true }

private fun createDarkColorScheme(accent: Color = TealPrimary) = darkColorScheme(
    primary = accent,
    onPrimary = NavyDark,
    primaryContainer = accent.copy(alpha = 0.3f),
    onPrimaryContainer = TextPrimary,
    secondary = PurpleAccent,
    onSecondary = TextPrimary,
    secondaryContainer = PurpleDeep,
    onSecondaryContainer = PurpleLight,
    tertiary = AmberAccent,
    onTertiary = NavyDark,
    background = NavyDark,
    onBackground = TextPrimary,
    surface = NavyMedium,
    onSurface = TextPrimary,
    surfaceVariant = NavyLight,
    onSurfaceVariant = TextSecondary,
    error = RedError,
    onError = TextPrimary,
    outline = TextMuted
)

private fun createLightColorScheme(accent: Color = TealPrimary) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = NavyDark,
    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = PurpleAccent.copy(alpha = 0.1f),
    onSecondaryContainer = PurpleDeep,
    tertiary = AmberAccent,
    onTertiary = NavyDark,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF4A5568),
    error = RedError,
    onError = Color.White,
    outline = Color(0xFFB0B8C4)
)

@Composable
fun StudentCompanionTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }

    // Observe theme mode and accent color from preferences
    val themeMode by prefs.themeMode.collectAsState(initial = "System")
    val accentColorHex by prefs.accentColorHex.collectAsState(initial = "#13ECEC")

    val accentColor = remember(accentColorHex) {
        try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (_: Exception) { TealPrimary }
    }

    val useDarkTheme = when (themeMode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) createDarkColorScheme(accentColor) else createLightColorScheme(accentColor)

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (useDarkTheme) NavyDark else Color(0xFFF5F7FA)
            @Suppress("DEPRECATION")
            window.statusBarColor = bgColor.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = bgColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(
        LocalAccentColor provides accentColor,
        LocalIsDarkTheme provides useDarkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content
        )
    }
}
