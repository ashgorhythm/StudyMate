package com.example.myandroidapp.ui.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    data object Dashboard : Screen("dashboard", "Home", "dashboard")
    data object Focus : Screen("focus", "Focus", "focus")
    data object Library : Screen("library", "Library", "library")
    data object AiChat : Screen("ai_chat", "AI Chat", "ai_chat")
    data object Settings : Screen("settings", "Settings", "settings")
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Focus,
    Screen.Library,
    Screen.AiChat
)
