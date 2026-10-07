package com.uniflow.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uniflow.app.presentation.MainContainerScreen
import com.uniflow.app.presentation.MainDestination
import com.uniflow.app.presentation.MainViewModel
import com.uniflow.app.presentation.admin_applications.AdminApplicationsScreen
import com.uniflow.app.presentation.admin_container.AdminContainerScreen
import com.uniflow.app.presentation.club_application.ClubApplicationScreen
import com.uniflow.app.presentation.club_detail.ClubDetailScreen
import com.uniflow.app.presentation.club_management.ClubManagementScreen
import com.uniflow.app.presentation.club_settings.ClubSettingsScreen
import com.uniflow.app.presentation.complete_profile.CompleteProfileScreen
import com.uniflow.app.presentation.create_edit_event.CreateEditEventScreen
import com.uniflow.app.presentation.edit_profile.EditProfileScreen
import com.uniflow.app.presentation.event_detail.EventDetailScreen
import com.uniflow.app.presentation.forgot_password.ForgotPasswordScreen
import com.uniflow.app.presentation.past_events.PastEventsScreen
import com.uniflow.app.presentation.update_email.UpdateEmailScreen
import com.uniflow.app.presentation.update_password.UpdatePasswordScreen
import com.uniflow.app.presentation.first_university_select.FirstUniversitySelectScreen
import com.uniflow.app.presentation.login.LoginScreen
import com.uniflow.app.presentation.register.RegisterScreen
import com.uniflow.app.presentation.settings.SettingsScreen
import com.uniflow.app.presentation.university_select.UniversitySelectScreen
import com.uniflow.app.presentation.user_applications.UserApplicationsScreen
import com.uniflow.app.ui.theme.UniFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val destination = mainViewModel.destination.value

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }

                            LaunchedEffect(destination) {
                                when (destination) {
                                    is MainDestination.Login -> {
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                    is MainDestination.AdminContainer -> {
                                        navController.navigate("admin_container") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                    is MainDestination.FirstUniversitySelect -> {
                                        navController.navigate("first_university_select") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                    is MainDestination.CompleteProfile -> {
                                        navController.navigate("complete_profile") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                    is MainDestination.ClubList -> {
                                        val encodedId = Uri.encode(destination.universityId)
                                        val encodedName = Uri.encode(destination.universityName)
                                        navController.navigate("club_list/$encodedId/$encodedName") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                    MainDestination.Loading -> {}
                                }
                            }
                        }

                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { uniId, uniName, needsSelection, needsProfileCompletion, isAdmin ->
                                    if (isAdmin) {
                                        navController.navigate("admin_container") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else if (needsSelection || uniId.isNullOrBlank() || uniName.isNullOrBlank()) {
                                        navController.navigate("first_university_select") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else if (needsProfileCompletion) {
                                        navController.navigate("complete_profile") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        val encodedId = Uri.encode(uniId)
                                        val encodedName = Uri.encode(uniName)
                                        navController.navigate("club_list/$encodedId/$encodedName") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                },
                                onNavigateToForgotPassword = {
                                    navController.navigate("forgot_password")
                                }
                            )
                        }

                        composable("forgot_password") {
                            ForgotPasswordScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.popBackStack()
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("first_university_select") {
                            FirstUniversitySelectScreen(
                                onUniversitySelectedSuccess = { _, _ ->
                                    navController.navigate("complete_profile") {
                                        popUpTo("first_university_select") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("complete_profile") {
                            CompleteProfileScreen(
                                onProfileCompletedSuccess = { university ->
                                    val uniId = university?.id ?: ""
                                    val uniName = university?.name ?: "Üniversite"
                                    val encodedId = Uri.encode(uniId)
                                    val encodedName = Uri.encode(uniName)
                                    navController.navigate("club_list/$encodedId/$encodedName") {
                                        popUpTo("complete_profile") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("university_select") {
                            UniversitySelectScreen(
                                onUniversitySelected = { university ->
                                    val encodedId = Uri.encode(university.id)
                                    val encodedName = Uri.encode(university.name)
                                    navController.navigate("club_list/$encodedId/$encodedName")
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEditProfile = { navController.navigate("edit_profile") },
                                onNavigateToClubApplication = { navController.navigate("club_application") },
                                onNavigateToMyApplications = { navController.navigate("user_applications") },
                                onNavigateToAdminApplications = { navController.navigate("admin_applications") },
                                onLogoutSuccess = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("edit_profile") {
                            EditProfileScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToUpdateEmail = { navController.navigate("update_email") },
                                onNavigateToUpdatePassword = { navController.navigate("update_password") }
                            )
                        }

                        composable("update_email") {
                            UpdateEmailScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("update_password") {
                            UpdatePasswordScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("club_application") {
                            ClubApplicationScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToMyApplications = { navController.navigate("user_applications") }
                            )
                        }

                        composable("user_applications") {
                            UserApplicationsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToNewApplication = { navController.navigate("club_application") }
                            )
                        }

                        composable("admin_applications") {
                            AdminApplicationsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("admin_container") {
                            AdminContainerScreen(
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "club_list/{universityId}/{universityName}",
                            arguments = listOf(
                                navArgument("universityId") { type = NavType.StringType },
                                navArgument("universityName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val rawId = backStackEntry.arguments?.getString("universityId") ?: ""
                            val rawName = backStackEntry.arguments?.getString("universityName") ?: "Üniversite"
                            val universityId = Uri.decode(rawId)
                            val universityName = Uri.decode(rawName)
                            MainContainerScreen(
                                universityId = universityId,
                                universityName = universityName,
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToClubManagement = { clubId ->
                                    navController.navigate("club_management/$clubId")
                                },
                                onNavigateToEventDetail = { eventId ->
                                    navController.navigate("event_detail/$eventId")
                                },
                                onNavigateToClubDetail = { clubId ->
                                    navController.navigate("club_detail/$clubId")
                                },
                                onNavigateToPastEvents = { uniId ->
                                    val encodedUniId = Uri.encode(uniId)
                                    navController.navigate("past_events/$encodedUniId")
                                }
                            )
                        }

                        composable(
                            route = "past_events/{universityId}",
                            arguments = listOf(
                                navArgument("universityId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val rawId = backStackEntry.arguments?.getString("universityId") ?: ""
                            val universityId = Uri.decode(rawId)
                            PastEventsScreen(
                                universityId = universityId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEventDetail = { eventId ->
                                    navController.navigate("event_detail/$eventId")
                                }
                            )
                        }

                        composable(
                            route = "club_detail/{clubId}",
                            arguments = listOf(
                                navArgument("clubId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val clubId = backStackEntry.arguments?.getString("clubId") ?: ""
                            ClubDetailScreen(
                                clubId = clubId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToClubManagement = { cid ->
                                    navController.navigate("club_management/$cid")
                                },
                                onNavigateToEventDetail = { eid ->
                                    navController.navigate("event_detail/$eid")
                                }
                            )
                        }

                        composable(
                            route = "event_detail/{eventId}",
                            arguments = listOf(
                                navArgument("eventId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                            EventDetailScreen(
                                eventId = eventId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEditEvent = { eid ->
                                    navController.navigate("edit_event/$eid")
                                }
                            )
                        }

                        composable(
                            route = "club_management/{clubId}",
                            arguments = listOf(
                                navArgument("clubId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val clubId = backStackEntry.arguments?.getString("clubId") ?: ""
                            ClubManagementScreen(
                                clubId = clubId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToClubSettings = { cid ->
                                    navController.navigate("club_settings/$cid")
                                },
                                onNavigateToEventDetail = { eventId ->
                                    navController.navigate("event_detail/$eventId")
                                },
                                onNavigateToCreateEvent = { cid ->
                                    navController.navigate("create_event/$cid")
                                }
                            )
                        }

                        composable(
                            route = "create_event/{clubId}",
                            arguments = listOf(
                                navArgument("clubId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val clubId = backStackEntry.arguments?.getString("clubId") ?: ""
                            CreateEditEventScreen(
                                clubId = clubId,
                                eventId = null,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "edit_event/{eventId}",
                            arguments = listOf(
                                navArgument("eventId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                            CreateEditEventScreen(
                                clubId = "",
                                eventId = eventId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "club_settings/{clubId}",
                            arguments = listOf(
                                navArgument("clubId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val clubId = backStackEntry.arguments?.getString("clubId") ?: ""
                            ClubSettingsScreen(
                                clubId = clubId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}