package com.jadwal.ui.navigation

import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jadwal.DeepLinkManager
import com.jadwal.R
import com.jadwal.data.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.first

@Composable
fun JadwalApp(
    prefs: UserPreferencesDataStore,
    deepLinkManager: DeepLinkManager,
) {
    // ─── إصلاح #4: تطبيق اللغة قبل أي رسم ───────────────────────────────
    // المشكلة كانت: AppCompatDelegate.setApplicationLocales يُستدعى بعد
    // أن تُرسم الشاشات، لذا الشاشات الأولى تستخدم اللغة الخاطئة.
    // الحل: نقرأ اللغة المحفوظة ونطبقها أول شيء في LaunchedEffect(Unit)
    // هذا يضمن أن كل stringResource() يُقرأ بالـ locale الصحيح.
    LaunchedEffect(Unit) {
        val savedCode = prefs.languageCode.first()
        if (savedCode.isNotBlank()) {
            val desiredTags = when (savedCode) {
                "ar" -> "ar"
                "en" -> "en"
                else -> return@LaunchedEffect
            }
            val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (currentTags != desiredTags) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(desiredTags)
                )
                // setApplicationLocales ستُعيد بناء الـ Activity تلقائياً
                // مما يجعل كل stringResource() يُعاد قراءته بالـ locale الصحيح
            }
        }
    }

    // ─── إصلاح #1 & #2: startDestination صحيح ───────────────────────────
    // نستخدم StartupViewModel الذي يتحقق من جلسة Supabase الحقيقية
    val startupViewModel: StartupViewModel = hiltViewModel()
    val startDestination by startupViewModel.startDestination.collectAsStateWithLifecycle()

    // انتظر حتى يُحدَّد الوجهة الصحيحة
    if (startDestination == null) return

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = Screen.bottomBarScreens.any { it == currentDestination?.route }

    // ─── إصلاح #3: معالجة Deep Link إعادة تعيين كلمة المرور ─────────────
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        deepLinkManager.pendingUri.collect { uri ->
            if (uri.scheme == "com.jadwal.app" && uri.host == "reset-password") {
                // Supabase تُرسل الـ token في الـ fragment بعد "#"
                val fragment = uri.fragment ?: ""
                if (fragment.isNotBlank()) {
                    // نمرر الـ fragment للـ ViewModel عبر الـ route
                    navController.navigate(
                        Screen.ResetPassword.createRoute(Uri.encode(fragment))
                    ) {
                        // أزل شاشة Login إن كانت موجودة لتجنب الرجوع إليها
                        popUpTo(Screen.Login.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
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