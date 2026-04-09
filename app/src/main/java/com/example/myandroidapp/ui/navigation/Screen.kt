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
    data object About : Screen("about", "About", "about")
    data object StudyPlanSetup : Screen("study_plan_setup", "Study Plan", "study_plan")
    data object SuperUser : Screen("super_user", "SuperUser", "super_user")
    data object UserProfile : Screen("user_profile/{memberId}", "Profile", "profile") {
        fun createRoute(memberId: String) = "user_profile/$memberId"
    }
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Focus,
    Screen.Community,
    Screen.Library,
    Screen.AiChat
)

// Routes where bottom nav should be hidden
val bottomNavHiddenRoutes = setOf(
    Screen.Settings.route,
    Screen.Profile.route,
    Screen.InterfaceSettings.route,
    Screen.About.route,
    Screen.StudyPlanSetup.route,
    Screen.SuperUser.route,
    "user_profile/{memberId}"
)

