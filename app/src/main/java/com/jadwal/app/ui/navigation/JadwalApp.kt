package com.jadwal.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.res.stringResource
import com.jadwal.R

@Composable
fun JadwalApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = Screen.bottomBarScreens.any { it == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    val items = listOf(
                        BottomNavItem(stringResource(R.string.nav_home),      Screen.Home.route,      Icons.Rounded.Home),
                        BottomNavItem(stringResource(R.string.nav_schedule),  Screen.Schedule.route,  Icons.Rounded.DateRange),
                        BottomNavItem(stringResource(R.string.nav_assistant), Screen.AiChat.route,    Icons.Rounded.AutoAwesome),
                        BottomNavItem(stringResource(R.string.nav_analytics), Screen.Analytics.route, Icons.Rounded.BarChart),
                        BottomNavItem(stringResource(R.string.nav_settings),  Screen.Settings.route,  Icons.Rounded.Settings),
                    )

                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon  = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        JadwalNavGraph(
            navController = navController,
            startDestination = Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
)
