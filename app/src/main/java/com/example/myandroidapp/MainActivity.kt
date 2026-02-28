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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myandroidapp.data.preferences.UserPreferences
import com.example.myandroidapp.ui.navigation.AppNavGraph
import com.example.myandroidapp.ui.navigation.Screen
import com.example.myandroidapp.ui.navigation.bottomNavItems
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
        Box(
            Modifier
                .fillMaxSize()
                .background(NavyDark)
        )
    } else {
        AnimatedContent(
            targetState = showOnboarding,
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(300))
            },
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

    if (adaptive.isTablet) {
        // ── TABLET: NavigationRail on the left ──
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = NavyMedium,
                contentColor = TextPrimary,
                header = {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "SM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TealPrimary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            ) {
                Spacer(Modifier.height(24.dp))
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    val icon = getNavIcon(screen, selected)
                    NavigationRailItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, screen.title, Modifier.size(24.dp)) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = TealPrimary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AppNavGraph(navController = navController, repository = app.repository)
            }
        }
    } else {
        // ── PHONE: Bottom navigation bar ──
        Scaffold(
            containerColor = NavyDark,
            bottomBar = {
                NavigationBar(
                    containerColor = NavyMedium,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val icon = getNavIcon(screen, selected)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, screen.title, Modifier.size(24.dp)) },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = TealPrimary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                AppNavGraph(navController = navController, repository = app.repository)
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
