package com.example.myandroidapp.ui.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    data object Dashboard : Screen("dashboard", "Dashboard", "dashboard")
    data object Progress : Screen("progress", "Progress", "progress")
    data object Focus : Screen("focus", "Focus", "focus")
    data object Library : Screen("library", "Library", "library")
    data object AiChat : Screen("ai_chat", "AI Chat", "ai_chat")
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Progress,
    Screen.Focus,
    Screen.Library,
    Screen.AiChat
)
