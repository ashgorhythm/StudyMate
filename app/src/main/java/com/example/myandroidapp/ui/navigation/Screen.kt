package com.example.myandroidapp.ui.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    data object Dashboard : Screen("dashboard", "Home", "dashboard")
    data object Focus : Screen("focus", "Focus", "focus")
    data object Library : Screen("library", "Library", "library")
    data object Community : Screen("community", "Community", "community")
    data object AiChat : Screen("ai_chat", "AI Chat", "ai_chat")
    data object Settings : Screen("settings", "Settings", "settings")
    data object Profile : Screen("profile", "My Profile", "profile")
    data object InterfaceSettings : Screen("interface_settings", "Interface Settings", "interface_settings")
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Focus,
    Screen.Community,
    Screen.Library,
    Screen.AiChat
)
