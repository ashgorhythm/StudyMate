package com.example.myandroidapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myandroidapp.StudentCompanionApp
import com.example.myandroidapp.data.repository.StudyRepository
import com.example.myandroidapp.ui.screens.aichat.AiChatScreen
import com.example.myandroidapp.ui.screens.aichat.AiChatViewModel
import com.example.myandroidapp.ui.screens.dashboard.DashboardScreen
import com.example.myandroidapp.ui.screens.dashboard.DashboardViewModel
import com.example.myandroidapp.ui.screens.dashboard.DashboardViewModelFactory
import com.example.myandroidapp.ui.screens.focus.FocusScreen
import com.example.myandroidapp.ui.screens.focus.FocusViewModel
import com.example.myandroidapp.ui.screens.focus.FocusViewModelFactory
import com.example.myandroidapp.ui.screens.library.LibraryScreen
import com.example.myandroidapp.ui.screens.library.LibraryViewModel
import com.example.myandroidapp.ui.screens.library.LibraryViewModelFactory
import com.example.myandroidapp.ui.screens.settings.SettingsScreen
import com.example.myandroidapp.ui.screens.settings.SettingsViewModel
import com.example.myandroidapp.ui.screens.settings.SettingsViewModelFactory

@Composable
fun AppNavGraph(
    navController: NavHostController,
    repository: StudyRepository
) {
    val context = LocalContext.current
    val app = context.applicationContext as StudentCompanionApp

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            val vm: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(repository, context))
            DashboardScreen(
                viewModel = vm,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Focus.route) {
            val vm: FocusViewModel = viewModel(factory = FocusViewModelFactory(repository))
            FocusScreen(viewModel = vm)
        }
        composable(Screen.Library.route) {
            val vm: LibraryViewModel = viewModel(factory = LibraryViewModelFactory(repository))
            LibraryScreen(viewModel = vm)
        }
        composable(Screen.AiChat.route) {
            val vm: AiChatViewModel = viewModel()
            AiChatScreen(viewModel = vm)
        }
        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(
                    taskDao = app.database.studyTaskDao(),
                    subjectDao = app.database.subjectDao(),
                    sessionDao = app.database.studySessionDao(),
                    fileDao = app.database.studyFileDao(),
                    context = context
                )
            )
            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
