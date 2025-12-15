package com.shakilpatel.boloindia.distributor.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shakilpatel.boloindia.distributor.ui.auth.AuthScreen
import com.shakilpatel.boloindia.distributor.ui.assignments.AssignmentsScreen
import com.shakilpatel.boloindia.distributor.ui.chat.ChatScreen
import com.shakilpatel.boloindia.distributor.ui.dashboard.DashboardScreen
import com.shakilpatel.boloindia.distributor.ui.deployment.DeploymentScreen
import com.shakilpatel.boloindia.distributor.ui.profile.ProfileScreen
import com.shakilpatel.boloindia.distributor.ui.qr.QRCustomizeScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Assignments : Screen("assignments")
    object QRCustomize : Screen("qr_customize/{qrCodeId}") {
        fun createRoute(qrCodeId: Long) = "qr_customize/$qrCodeId"
    }
    object Deployment : Screen("deployment/{qrCodeId}") {
        fun createRoute(qrCodeId: Long) = "deployment/$qrCodeId"
    }
    object Chat : Screen("chat/{qrCodeId}/{qrCodeName}") {
        fun createRoute(qrCodeId: Long, qrCodeName: String) = "chat/$qrCodeId/$qrCodeName"
    }
    object Profile : Screen("profile")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Screen
        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard Screen
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToPending = {
                    navController.navigate(Screen.Assignments.route)
                },
                onNavigateToAssignments = {
                    navController.navigate(Screen.Assignments.route)
                },
                onNavigateToChat = {
                    // Navigate to assignments, user will select specific assignment
                    navController.navigate(Screen.Assignments.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                hiltViewModel()
            )
        }

        // Assignments Screen
        composable(Screen.Assignments.route) {
            AssignmentsScreen(
                onNavigateToQRCustomize = { qrCodeId ->
                    navController.navigate(Screen.QRCustomize.createRoute(qrCodeId))
                },
                onNavigateToChat = { qrCodeId ->
                    // For simplicity, using qrCodeId as name, you can pass actual name
                    navController.navigate(Screen.Chat.createRoute(qrCodeId, "Assignment"))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // QR Customize Screen
        composable(
            route = Screen.QRCustomize.route,
            arguments = listOf(
                navArgument("qrCodeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val qrCodeId = backStackEntry.arguments?.getLong("qrCodeId") ?: 0L
            QRCustomizeScreen(
                qrCodeId = qrCodeId,
                onNavigateToDeployment = { id ->
                    navController.navigate(Screen.Deployment.createRoute(id))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Deployment Screen
        composable(
            route = Screen.Deployment.route,
            arguments = listOf(
                navArgument("qrCodeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val qrCodeId = backStackEntry.arguments?.getLong("qrCodeId") ?: 0L
            DeploymentScreen(
                qrCodeId = qrCodeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDeploymentSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                hiltViewModel()
            )
        }

        // Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("qrCodeId") { type = NavType.LongType },
                navArgument("qrCodeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val qrCodeId = backStackEntry.arguments?.getLong("qrCodeId") ?: 0L
            val qrCodeName = backStackEntry.arguments?.getString("qrCodeName") ?: ""
            ChatScreen(
                qrCodeId = qrCodeId,
                qrCodeName = qrCodeName,
                onNavigateBack = {
                    navController.popBackStack()
                },
                hiltViewModel()
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                hiltViewModel()
            )
        }
    }
}
