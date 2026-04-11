package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.ui.navigation.AppNavGraph
import com.example.myandroidapp.ui.navigation.Screen
import com.example.myandroidapp.ui.navigation.bottomNavHiddenRoutes
import com.example.myandroidapp.ui.navigation.bottomNavItems
import com.example.myandroidapp.ui.screens.focus.FocusViewModel
import com.example.myandroidapp.ui.screens.focus.FocusViewModelFactory
import com.example.myandroidapp.ui.screens.onboarding.OnboardingScreen
import com.example.myandroidapp.ui.screens.onboarding.StudyBuddySetupScreen
import com.example.myandroidapp.ui.theme.*
import com.example.myandroidapp.ui.theme.LocalAccentColor
import com.example.myandroidapp.ui.theme.LocalIsDarkTheme
import com.example.myandroidapp.ui.util.rememberAdaptiveInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StudentCompanionApp

        setContent {
            StudentCompanionTheme {
                AppEntryPoint(app)
            }
        }
    }
}

private enum class AppScreen { LOADING, ONBOARDING, FOLDER_SETUP, MAIN }

@Composable
private fun AppEntryPoint(app: StudentCompanionApp) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(AppScreen.LOADING) }

    LaunchedEffect(Unit) {
        val onboardingDone = userPreferences.onboardingCompleted.first()
        val folderSetupDone = userPreferences.folderSetupShown.first()
        screen = when {
            !onboardingDone -> AppScreen.ONBOARDING
            !folderSetupDone -> AppScreen.FOLDER_SETUP
            else -> AppScreen.MAIN
        }
    }

    if (screen == AppScreen.LOADING) {
        Box(Modifier.fillMaxSize().background(NavyDark))
        return
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
        label = "appTransition"
    ) { currentScreen ->
        when (currentScreen) {
            AppScreen.ONBOARDING -> OnboardingScreen(
                onComplete = { name ->
                    scope.launch {
                        userPreferences.completeOnboarding(name)
                        screen = AppScreen.FOLDER_SETUP
                    }
                }
            )
            AppScreen.FOLDER_SETUP -> StudyBuddySetupScreen(
                onContinue = {
                    scope.launch {
                        userPreferences.markFolderSetupShown()
                        screen = AppScreen.MAIN
                    }
                }
            )
            AppScreen.MAIN -> MainApp(app)
            AppScreen.LOADING -> Box(Modifier.fillMaxSize().background(NavyDark))
        }
    }
}

@Composable
private fun MainApp(app: StudentCompanionApp) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val adaptive = rememberAdaptiveInfo()
    val accent = LocalAccentColor.current
    val isDark = LocalIsDarkTheme.current

    // ── Hoist FocusViewModel here so nav bar can observe focus lock ──
    val focusViewModel: FocusViewModel = viewModel(
        factory = FocusViewModelFactory(app.repository)
    )
    val focusState by focusViewModel.uiState.collectAsState()
    val isFocusLocked = focusState.isAppLocked

    // Should the bottom nav be hidden?
    val shouldHideBottomNav = currentRoute in bottomNavHiddenRoutes

    // Helper: navigate to a tab, unless focus is locked (then notify user)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun navigateTo(route: String) {
        if (isFocusLocked && route != Screen.Focus.route) {
            // Block navigation — show feedback
            scope.launch {
                snackbarHostState.showSnackbar(
                    "🔒 App locked during focus! Finish or reset the timer to switch tabs.",
                    duration = SnackbarDuration.Short
                )
            }
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (adaptive.isTablet) {
        // ── TABLET: NavigationRail ──
        Row(Modifier.fillMaxSize()) {
            if (!shouldHideBottomNav) {
                NavigationRail(
                    containerColor = if (isDark) NavyMedium else MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    header = {
                        Spacer(Modifier.height(12.dp))
                        Text("SM", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accent, modifier = Modifier.padding(vertical = 12.dp))
                    }
                ) {
                    Spacer(Modifier.height(24.dp))
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val icon = getNavIcon(screen, selected)
                        val locked = isFocusLocked && screen.route != Screen.Focus.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(screen.route) },
                            icon = {
                                BadgedBox(badge = {
                                    if (locked) Badge(containerColor = RedError.copy(0.7f)) {
                                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(8.dp))
                                    }
                                }) {
                                    Icon(icon, screen.title, Modifier.size(24.dp))
                                }
                            },
                            label = { Text(screen.title, fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = if (locked) TextMuted else accent,
                                selectedTextColor = accent,
                                unselectedIconColor = if (locked) TextMuted.copy(0.5f) else TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = accent.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AppNavGraph(navController = navController, repository = app.repository, focusViewModel = focusViewModel)
            }
        }
    } else {
        // ── PHONE: Bottom nav bar ──
        Scaffold(
            containerColor = if (isDark) NavyDark else MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!shouldHideBottomNav) {
                    NavigationBar(
                        containerColor = if (isDark) NavyMedium else MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val icon = getNavIcon(screen, selected)
                            val locked = isFocusLocked && screen.route != Screen.Focus.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(screen.route) },
                                icon = {
                                    BadgedBox(badge = {
                                        if (locked) Badge(containerColor = RedError.copy(0.7f)) {
                                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(8.dp))
                                        }
                                    }) {
                                        Icon(icon, screen.title, Modifier.size(24.dp))
                                    }
                                },
                                label = { Text(screen.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (locked) TextMuted else accent,
                                    selectedTextColor = accent,
                                    unselectedIconColor = if (locked) TextMuted.copy(0.5f) else TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = accent.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                AppNavGraph(navController = navController, repository = app.repository, focusViewModel = focusViewModel)
            }
        }
    }
}

private fun getNavIcon(screen: Screen, selected: Boolean): ImageVector {
    return when (screen) {
        Screen.Dashboard -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        Screen.Focus -> if (selected) Icons.Filled.Timer else Icons.Outlined.Timer
        Screen.Community -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
        Screen.Library -> if (selected) Icons.AutoMirrored.Filled.LibraryBooks else Icons.AutoMirrored.Outlined.LibraryBooks
        Screen.AiChat -> if (selected) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
        Screen.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
        Screen.Profile -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
        Screen.InterfaceSettings -> if (selected) Icons.Filled.Palette else Icons.Outlined.Palette
        Screen.About -> if (selected) Icons.Filled.Info else Icons.Outlined.Info
        Screen.StudyPlanSetup -> if (selected) Icons.Filled.EventNote else Icons.Outlined.EventNote
        Screen.SuperUser -> if (selected) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings
        Screen.UserProfile -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
        Screen.Inbox -> if (selected) Icons.Filled.Mail else Icons.Outlined.MailOutline
        Screen.ChatConversation -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
    }
}
