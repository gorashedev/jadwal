package com.jadwal.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.jadwal.R
import com.jadwal.ui.screens.analytics.AnalyticsScreen
import com.jadwal.ui.screens.auth.ForgotPasswordScreen
import com.jadwal.ui.screens.auth.LoginScreen
import com.jadwal.ui.screens.auth.RegisterScreen
import com.jadwal.ui.screens.home.HomeScreen
import com.jadwal.ui.screens.onboarding.OnboardingScreen
import com.jadwal.ui.screens.schedule.ScheduleScreen
import com.jadwal.ui.screens.session.SessionScreen
import com.jadwal.ui.screens.settings.SettingsScreen
import com.jadwal.ui.screens.setup.SetupScreen
import com.jadwal.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

// ===== نقاط التنقل في الـ BottomBar =====
data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val labelRes: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Rounded.Home, Icons.Rounded.Home, R.string.nav_home),
    BottomNavItem(Screen.Schedule, Icons.Rounded.CalendarMonth, Icons.Rounded.CalendarMonth, R.string.nav_schedule),
    BottomNavItem(Screen.Analytics, Icons.Rounded.BarChart, Icons.Rounded.BarChart, R.string.nav_analytics),
    BottomNavItem(Screen.Settings, Icons.Rounded.Settings, Icons.Rounded.Settings, R.string.nav_settings),
)

@Composable
fun JadwalApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hazeState = remember { HazeState() }

    val showBottomBar = currentRoute in Screen.bottomBarScreens

    Box(modifier = Modifier.fillMaxSize()) {
        // NavHost يمتد لكل الشاشة مع تأثير الـ haze
        NavHost(
            navController = navController,
            startDestination = Screen.Onboarding.route,
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(animationSpec = tween(300)) { it / 4 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) +
                        slideOutHorizontally(animationSpec = tween(200)) { -it / 4 }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(animationSpec = tween(300)) { -it / 4 }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) +
                        slideOutHorizontally(animationSpec = tween(200)) { it / 4 }
            }
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Setup.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onEmailSent = { navController.popBackStack() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Setup.route) {
                SetupScreen(
                    onSetupComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Setup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onStartSession = { id ->
                        navController.navigate(Screen.Session.route + "/$id")
                    },
                    onViewSchedule = { navController.navigate(Screen.Schedule.route) },
                    onViewReport = { navController.navigate(Screen.Analytics.route) }
                )
            }

            composable(
                route = Screen.Session.route + "/{scheduleItemId}",
            ) {
                val id = it.arguments?.getString("scheduleItemId") ?: return@composable
                SessionScreen(
                    scheduleItemId = id,
                    onSessionEnd = { navController.popBackStack() }
                )
            }

            composable(Screen.Schedule.route) {
                ScheduleScreen()
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }

        // ===== الـ BottomBar بتأثير الزجاج iOS 26 =====
        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it } +
                    fadeIn(tween(250)),
            exit = slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it } +
                    fadeOut(tween(200)),
        ) {
            JadwalBottomBar(
                navController = navController,
                currentRoute = currentRoute,
                hazeState = hazeState,
            )
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun JadwalBottomBar(
    navController: NavController,
    currentRoute: String?,
    hazeState: HazeState,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // بطاقة الزجاج الرئيسية
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(40.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeMaterials.ultraThin(
                        tintColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                )
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(40.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.screen.route

                    // زر التنقل بتأثير iOS 26
                    Ios26NavButton(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            navController.navigate(item.screen.route) {
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
}

/**
 * زر التنقل بتأثير الفقاعة المتحركة المستوحى من iOS 26
 * عند التحديد: يظهر خلف الأيقونة "فقاعة زجاجية" ملونة بشكل متحرك
 */
@Composable
fun Ios26NavButton(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    // تحريك حجم الفقاعة
    val bubbleSize by animateDpAsState(
        targetValue = if (isSelected) 52.dp else 44.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubble_size"
    )

    // تحريك لون الخلفية
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "bg_color"
    )

    // تحريك لون الأيقونة
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) onPrimaryColor
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "icon_color"
    )

    // تحريك حجم الأيقونة
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // بدون تأثير Ripple عشان يبدو iOS
            ) { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        // الفقاعة الزجاجية + الأيقونة
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(bubbleSize)
        ) {
            // خلفية الفقاعة
            if (isSelected) {
                // توهج خلفي للتأثير الزجاجي
                Box(
                    modifier = Modifier
                        .size(bubbleSize)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.4f),
                                    primaryColor.copy(alpha = 0.0f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .blur(8.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isSelected) 48.dp else 0.dp)
                    .background(
                        color = backgroundColor,
                        shape = CircleShape
                    )
            ) {}

            Icon(
                imageVector = if (isSelected) item.iconSelected else item.icon,
                contentDescription = stringResource(item.labelRes),
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }

        // التسمية — تظهر فقط للعنصر المحدد
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Text(
                text = stringResource(item.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
