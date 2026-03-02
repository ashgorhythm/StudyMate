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
import com.example.myandroidapp.ui.navigation.bottomNavItems
import com.example.myandroidapp.ui.screens.focus.FocusViewModel
import com.example.myandroidapp.ui.screens.focus.FocusViewModelFactory
import com.example.myandroidapp.ui.screens.onboarding.OnboardingScreen
import com.example.myandroidapp.ui.theme.*
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

@Composable
private fun AppEntryPoint(app: StudentCompanionApp) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val completed = userPreferences.onboardingCompleted.first()
        showOnboarding = !completed
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize().background(NavyDark))
    } else {
        AnimatedContent(
            targetState = showOnboarding,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(300)) },
            label = "appTransition"
        ) { shouldShowOnboarding ->
            if (shouldShowOnboarding) {
                OnboardingScreen(
                    onComplete = { name ->
                        scope.launch {
                            userPreferences.completeOnboarding(name)
                            showOnboarding = false
                        }
                    }
                )
            } else {
                MainApp(app)
            }
        }
    }
}

@Composable
private fun MainApp(app: StudentCompanionApp) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val adaptive = rememberAdaptiveInfo()

    // ── Hoist FocusViewModel here so nav bar can observe focus lock ──
    val focusViewModel: FocusViewModel = viewModel(
        factory = FocusViewModelFactory(app.repository)
    )
    val focusState by focusViewModel.uiState.collectAsState()
    val isFocusLocked = focusState.isAppLocked

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
            NavigationRail(
                containerColor = NavyMedium,
                contentColor = TextPrimary,
                header = {
                    Spacer(Modifier.height(12.dp))
                    Text("SM", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TealPrimary, modifier = Modifier.padding(vertical = 12.dp))
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
                            selectedIconColor = if (locked) TextMuted else TealPrimary,
                            selectedTextColor = TealPrimary,
                            unselectedIconColor = if (locked) TextMuted.copy(0.5f) else TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = TealPrimary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AppNavGraph(navController = navController, repository = app.repository, focusViewModel = focusViewModel)
            }
        }
    } else {
        // ── PHONE: Bottom nav bar ──
        Scaffold(
            containerColor = NavyDark,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = NavyMedium,
                    contentColor = TextPrimary,
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
                                selectedIconColor = if (locked) TextMuted else TealPrimary,
                                selectedTextColor = TealPrimary,
                                unselectedIconColor = if (locked) TextMuted.copy(0.5f) else TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = TealPrimary.copy(alpha = 0.12f)
                            )
                        )
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
        Screen.Library -> if (selected) Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks
        Screen.AiChat -> if (selected) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
        Screen.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
}
