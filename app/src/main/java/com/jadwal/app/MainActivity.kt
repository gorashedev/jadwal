package com.jadwal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jadwal.ui.theme.JadwalTheme
import com.jadwal.ui.screens.onboarding.OnboardingScreen
import com.jadwal.ui.screens.setup.SetupScreen
import com.jadwal.ui.screens.home.HomeScreen
import com.jadwal.ui.screens.session.SessionScreen
import com.jadwal.ui.screens.analytics.AnalyticsScreen
import com.jadwal.ui.screens.schedule.ScheduleScreen
import com.jadwal.ui.screens.settings.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // إضافة متغير يتحكم في الوضع الداكن لكل التطبيق
            var isDarkTheme by remember { mutableStateOf(false) }

            // تمرير المتغير للثيم الأساسي
            JadwalTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf("home", "schedule", "analytics", "settings")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                val items = listOf(
                                    Triple("home", Icons.Rounded.Home, "الرئيسية"),
                                    Triple("schedule", Icons.Rounded.CalendarMonth, "الجدول"),
                                    Triple("analytics", Icons.Rounded.BarChart, "التقرير"),
                                    Triple("settings", Icons.Rounded.Settings, "الإعدادات")
                                )

                                items.forEach { (route, icon, label) ->
                                    NavigationBarItem(
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label) },
                                        selected = currentRoute == route,
                                        onClick = {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "onboarding",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(onFinish = { navController.navigate("setup") })
                        }
                        composable("setup") {
                            SetupScreen(onSetupComplete = { navController.navigate("home") })
                        }
                        composable("home") {
                            HomeScreen(
                                onStartSession = { taskId -> navController.navigate("session/$taskId") },
                                onViewSchedule = { navController.navigate("schedule") },
                                onViewReport = { navController.navigate("analytics") }
                            )
                        }
                        composable(
                            route = "session/{scheduleItemId}",
                            arguments = listOf(navArgument("scheduleItemId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getString("scheduleItemId") ?: ""
                            SessionScreen(
                                scheduleItemId = taskId,
                                onSessionEnd = { navController.popBackStack() }
                            )
                        }
                        composable("analytics") { AnalyticsScreen() }
                        composable("schedule") { ScheduleScreen() }

                        // تمرير حالة الثيم لشاشة الإعدادات
                        composable("settings") {
                            SettingsScreen(
                                isDarkTheme = isDarkTheme,
                                onThemeChange = { isDarkTheme = it }
                            )
                        }
                    }
                }
            }
        }
    }
}