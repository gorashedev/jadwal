package com.jadwal.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jadwal.DeepLinkManager
import com.jadwal.R
import com.jadwal.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.delay

@Composable
fun JadwalApp(
    prefs: UserPreferencesDataStore,
    deepLinkManager: DeepLinkManager,
) {
    val startupViewModel: StartupViewModel = hiltViewModel()
    val startDestination by startupViewModel.startDestination.collectAsStateWithLifecycle()

    // انتظر حتى يُحدَّد startDestination
    if (startDestination == null) return

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = Screen.bottomBarScreens.any { it == currentDestination?.route }

    // ─── معالجة Deep Link عندما يكون التطبيق يعمل بالفعل (onNewIntent) ────
    // هذا يختلف عن حالة فتح التطبيق من صفر — تلك حالة يُعالجها StartupViewModel.
    // هذه الحالة: المستخدم يضغط الرابط والتطبيق مفتوح في الخلفية.
    val pendingUri by deepLinkManager.pendingUri.collectAsStateWithLifecycle()
    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        if (uri.scheme == "com.jadwal.app" && uri.host == "reset-password") {
            val fragment = uri.fragment ?: ""
            if (fragment.isNotBlank()) {
                // نُعطي NavHost وقتاً لإكمال التهيئة (frame واحد على الأقل)
                delay(200)
                navController.navigate(
                    Screen.ResetPassword.createRoute(Uri.encode(fragment))
                ) {
                    // أزل كل الـ backstack للحصول على back stack نظيف
                    popUpTo(0) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        deepLinkManager.consumeUri()
    }

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
            startDestination = startDestination!!,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
)