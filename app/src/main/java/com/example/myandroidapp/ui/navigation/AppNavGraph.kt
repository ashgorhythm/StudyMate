package com.example.myandroidapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.example.myandroidapp.ui.screens.progress.ProgressScreen
import com.example.myandroidapp.ui.screens.progress.ProgressViewModel
import com.example.myandroidapp.ui.screens.progress.ProgressViewModelFactory

@Composable
fun AppNavGraph(
    navController: NavHostController,
    repository: StudyRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            val vm: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(repository))
            DashboardScreen(viewModel = vm)
        }
        composable(Screen.Progress.route) {
            val vm: ProgressViewModel = viewModel(factory = ProgressViewModelFactory(repository))
            ProgressScreen(viewModel = vm)
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
    }
}
