package com.example.myandroidapp.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple window size class detection based on screen width.
 * - COMPACT: phones (< 600dp)
 * - MEDIUM: small tablets, foldables (600dp - 839dp)
 * - EXPANDED: large tablets (≥ 840dp)
 */
enum class WindowWidthSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class AdaptiveInfo(
    val windowWidth: WindowWidthSize,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp
) {
    val isTablet: Boolean get() = windowWidth != WindowWidthSize.COMPACT
    val isExpanded: Boolean get() = windowWidth == WindowWidthSize.EXPANDED

    /** Number of columns for grid layouts */
    val gridColumns: Int get() = when (windowWidth) {
        WindowWidthSize.COMPACT -> 2
        WindowWidthSize.MEDIUM -> 3
        WindowWidthSize.EXPANDED -> 4
    }

    /** Horizontal padding for content */
    val horizontalPadding: Dp get() = when (windowWidth) {
        WindowWidthSize.COMPACT -> 20.dp
        WindowWidthSize.MEDIUM -> 32.dp
        WindowWidthSize.EXPANDED -> 48.dp
    }

    /** Max content width for centered layouts like chat */
    val maxContentWidth: Dp get() = when (windowWidth) {
        WindowWidthSize.COMPACT -> Dp.Unspecified
        WindowWidthSize.MEDIUM -> 600.dp
        WindowWidthSize.EXPANDED -> 720.dp
    }

    /** Progress ring size */
    val progressRingSize: Dp get() = when (windowWidth) {
        WindowWidthSize.COMPACT -> 180.dp
        WindowWidthSize.MEDIUM -> 220.dp
        WindowWidthSize.EXPANDED -> 260.dp
    }

    /** Bottom padding for content (above nav bar) */
    val bottomContentPadding: Dp get() = when (windowWidth) {
        WindowWidthSize.COMPACT -> 100.dp
        WindowWidthSize.MEDIUM -> 32.dp
        WindowWidthSize.EXPANDED -> 32.dp
    }
}

@Composable
fun rememberAdaptiveInfo(): AdaptiveInfo {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp.dp
    val heightDp = config.screenHeightDp.dp

    val windowWidth = when {
        config.screenWidthDp < 600 -> WindowWidthSize.COMPACT
        config.screenWidthDp < 840 -> WindowWidthSize.MEDIUM
        else -> WindowWidthSize.EXPANDED
    }

    return AdaptiveInfo(
        windowWidth = windowWidth,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp
    )
}
