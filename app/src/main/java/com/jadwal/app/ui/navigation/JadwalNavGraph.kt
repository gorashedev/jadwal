package com.jadwal.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jadwal.ui.screens.ai.AISuggestionScreen
import com.jadwal.ui.screens.analytics.AnalyticsScreen
import com.jadwal.ui.screens.auth.ForgotPasswordScreen
import com.jadwal.ui.screens.auth.LoginScreen
import com.jadwal.ui.screens.auth.RegisterScreen
import com.jadwal.ui.screens.chat.AIChatScreen
import com.jadwal.ui.screens.home.HomeScreen
import com.jadwal.ui.screens.scan.ExamScanScreen
import com.jadwal.ui.screens.onboarding.OnboardingScreen
import com.jadwal.ui.screens.profile.ProfileScreen
import com.jadwal.ui.screens.schedule.ScheduleScreen
import com.jadwal.ui.screens.session.SessionScreen
import com.jadwal.ui.screens.settings.SettingsScreen
import com.jadwal.ui.screens.setup.SetupScreen
import com.jadwal.ui.screens.subjects.SubjectsScreen

@Composable
fun JadwalNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                    fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                    fadeOut(tween(200))
        },
    ) {
        // ===== Onboarding =====
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        // ===== Auth =====
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onEmailSent = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ===== Setup =====
        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
            )
        }

        // ===== الشاشات الرئيسية =====
        composable(Screen.Home.route) {
            HomeScreen(
                onStartSession   = { id -> navController.navigate("${Screen.Session.route}/$id") },
                onViewSchedule   = { navController.navigate(Screen.Schedule.route) },
                onViewReport     = { navController.navigate(Screen.Analytics.route) },
                onViewAISuggestion = { navController.navigate(Screen.AISuggestion.route) },
            )
        }

        composable(Screen.Schedule.route) {
            ScheduleScreen(
                onScanExams = { navController.navigate(Screen.ExamScan.route) },
                onManageSubjects = { navController.navigate(Screen.Subjects.route) },
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onViewProfile = { navController.navigate(Screen.Profile.route) },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ===== شاشة المساعد الذكي (دردشة) =====
        composable(Screen.AiChat.route) {
            AIChatScreen()
        }

        // ===== شاشة إدارة المواد =====
        composable(Screen.Subjects.route) {
            SubjectsScreen(onBack = { navController.popBackStack() })
        }

        // ===== شاشة الجلسة =====
        composable("${Screen.Session.route}/{scheduleItemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("scheduleItemId") ?: ""
            SessionScreen(
                scheduleItemId = itemId,
                onSessionEnd = { navController.popBackStack() },
            )
        }

        // ===== شاشة الذكاء الاصطناعي للجدول =====
        composable(Screen.AISuggestion.route) {
            AISuggestionScreen(
                onBack = { navController.popBackStack() },
                onScheduleSaved = {
                    navController.navigate(Screen.Schedule.route) {
                        popUpTo(Screen.AISuggestion.route) { inclusive = true }
                    }
                },
            )
        }

        // ===== شاشة الملف الشخصي =====
        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        // ===== مسح جدول الامتحانات بالذكاء الاصطناعي =====
        composable(Screen.ExamScan.route) {
            ExamScanScreen(
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Screen.Schedule.route) {
                        popUpTo(Screen.ExamScan.route) { inclusive = true }
                    }
                },
            )
        }
    }
}
