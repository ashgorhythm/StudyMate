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
import com.example.myandroidapp.ui.screens.library.LibraryScreen
import com.example.myandroidapp.ui.screens.library.LibraryViewModel
import com.example.myandroidapp.ui.screens.library.LibraryViewModelFactory
import com.example.myandroidapp.ui.screens.profile.ProfileScreen
import com.example.myandroidapp.ui.screens.profile.ProfileViewModel
import com.example.myandroidapp.ui.screens.profile.ProfileViewModelFactory
import com.example.myandroidapp.ui.screens.settings.SettingsScreen
import com.example.myandroidapp.ui.screens.settings.SettingsViewModel
import com.example.myandroidapp.ui.screens.settings.SettingsViewModelFactory
import com.example.myandroidapp.ui.screens.community.CommunityScreen
import com.example.myandroidapp.ui.screens.community.CommunityViewModel
import com.example.myandroidapp.ui.screens.community.CommunityViewModelFactory
import com.example.myandroidapp.ui.screens.inbox.InboxScreen
import com.example.myandroidapp.ui.screens.inbox.InboxViewModel
import com.example.myandroidapp.ui.screens.inbox.InboxViewModelFactory
import com.example.myandroidapp.ui.screens.profile.UserProfileScreen
import com.example.myandroidapp.ui.screens.profile.UserProfileViewModel
import com.example.myandroidapp.ui.screens.profile.UserProfileViewModelFactory
import com.example.myandroidapp.ui.screens.settings.AboutScreen
import com.example.myandroidapp.ui.screens.settings.SuperUserScreen
import com.example.myandroidapp.ui.screens.dashboard.StudyPlanSetupScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(
    navController: NavHostController,
    repository: StudyRepository,
    focusViewModel: FocusViewModel
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
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToStudyPlan = { navController.navigate(Screen.StudyPlanSetup.route) }
            )
        }
        composable(Screen.Focus.route) {
            FocusScreen(viewModel = focusViewModel)
        }
        composable(Screen.Library.route) {
            val vm: LibraryViewModel = viewModel(factory = LibraryViewModelFactory(repository))
            LibraryScreen(viewModel = vm)
        }
        composable(Screen.AiChat.route) {
            val vm: AiChatViewModel = viewModel()
            AiChatScreen(viewModel = vm)
        }
        composable(Screen.Community.route) {
            val vm: CommunityViewModel = viewModel(factory = CommunityViewModelFactory(app.firebaseSocialService, context))
            CommunityScreen(
                viewModel = vm,
                onNavigateToProfile = { memberId ->
                    navController.navigate(Screen.UserProfile.createRoute(memberId))
                },
                onNavigateToInbox = {
                    navController.navigate(Screen.Inbox.route)
                }
            )
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
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToInterface = { navController.navigate(Screen.InterfaceSettings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToSuperUser = { navController.navigate(Screen.SuperUser.route) }
            )
        }
        composable(Screen.Profile.route) {
            val vm: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(context))
            ProfileScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable(Screen.InterfaceSettings.route) {
            com.example.myandroidapp.ui.screens.settings.InterfaceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.StudyPlanSetup.route) {
            val dashVm: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(repository, context))
            StudyPlanSetupScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SuperUser.route) {
            SuperUserScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("memberId") { type = NavType.StringType })
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId") ?: return@composable
            val currentMemberId = app.firebaseSocialService.currentUserId ?: ""
            val vm: UserProfileViewModel = viewModel(
                factory = UserProfileViewModelFactory(
                    firebase = app.firebaseSocialService,
                    currentMemberId = currentMemberId,
                    targetMemberId = memberId
                )
            )
            UserProfileScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onChat = { targetMemberId ->
                    // Navigate to inbox — the inbox will handle opening the chat
                    navController.navigate(Screen.Inbox.route)
                }
            )
        }
        // ── Inbox Screen ──
        composable(Screen.Inbox.route) {
            val currentMemberId = app.firebaseSocialService.currentUserId ?: ""
            val vm: InboxViewModel = viewModel(
                factory = InboxViewModelFactory(
                    firebase = app.firebaseSocialService,
                    currentMemberId = currentMemberId
                )
            )
            InboxScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

