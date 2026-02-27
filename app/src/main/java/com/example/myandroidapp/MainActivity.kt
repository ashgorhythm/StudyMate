package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myandroidapp.ui.navigation.AppNavGraph
import com.example.myandroidapp.ui.navigation.Screen
import com.example.myandroidapp.ui.navigation.bottomNavItems
import com.example.myandroidapp.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StudentCompanionApp

        setContent {
            StudentCompanionTheme {
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

private fun getNavIcon(screen: Screen, selected: Boolean): ImageVector {
    return when (screen) {
        Screen.Dashboard -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
        Screen.Progress -> if (selected) Icons.Filled.TrendingUp else Icons.Outlined.TrendingUp
        Screen.Focus -> if (selected) Icons.Filled.Timer else Icons.Outlined.Timer
        Screen.Library -> if (selected) Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks
        Screen.AiChat -> if (selected) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
    }
}
