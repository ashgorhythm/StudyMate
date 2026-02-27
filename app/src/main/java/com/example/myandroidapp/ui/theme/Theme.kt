package com.example.myandroidapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = NavyDark,
    primaryContainer = TealDark,
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

@Composable
fun StudentCompanionTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyDark.toArgb()
            window.navigationBarColor = NavyDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
