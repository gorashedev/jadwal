package com.jadwal.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jadwal.DeepLinkManager
import com.jadwal.R
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.ui.theme.GlassBorderDark
import com.jadwal.ui.theme.GlassBorderLight
import com.jadwal.ui.theme.GlassSurfaceDark
import com.jadwal.ui.theme.LocalAppDarkTheme
import kotlinx.coroutines.delay

@Composable
fun JadwalApp(
    prefs: UserPreferencesDataStore,
    deepLinkManager: DeepLinkManager,
) {
    val startupViewModel: StartupViewModel = hiltViewModel()
    val startDestination by startupViewModel.startDestination.collectAsStateWithLifecycle()

    if (startDestination == null) return

    val navController = rememberNavController()

    // currentBackStackEntryAsState() is a proper Compose State — Compose will
    // recompose this block whenever the back stack changes, giving a correct
    // showBottomBar value. Only a Boolean flip triggers a recomposition of
    // the Scaffold, so the cost is negligible.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = remember(navBackStackEntry) {
        Screen.bottomBarScreens.any { it == navBackStackEntry?.destination?.route }
    }

    val pendingUri by deepLinkManager.pendingUri.collectAsStateWithLifecycle()

    LaunchedEffect(pendingUri) {
        if (pendingUri == null || !deepLinkManager.hasPendingPasswordReset()) return@LaunchedEffect
        delay(150)
        navController.navigate(Screen.ResetPassword.route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
        deepLinkManager.consumeUri()
    }

    Scaffold(
        // Transparent wrapper — the floating pill itself carries its own
        // visible background; only the area outside the pill is see-through.
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                // FloatingNavBar owns its own currentBackStackEntryAsState so
                // only the bar recomposes when the selected tab changes.
                FloatingNavBar(navController = navController)
            }
        },
    ) { innerPadding ->
        // Consume only the top inset from Scaffold so the nav graph content
        // fills the full screen height and scrolls behind the floating pill.
        // Each screen is responsible for its own bottom spacing (via
        // WindowInsets or an explicit padding that matches the pill height).
        JadwalNavGraph(
            navController    = navController,
            startDestination = startDestination!!,
            modifier         = Modifier.padding(top = innerPadding.calculateTopPadding()),
        )
    }
}

@Composable
private fun FloatingNavBar(
    navController: NavHostController,
) {
    // Reading currentBackStackEntryAsState here means ONLY FloatingNavBar
    // recomposes on navigation events — the Scaffold and JadwalNavGraph are unaffected.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isDark = LocalAppDarkTheme.current

    val items = listOf(
        BottomNavItem(stringResource(R.string.nav_home),      Screen.Home.route,      Icons.Rounded.Home),
        BottomNavItem(stringResource(R.string.nav_schedule),  Screen.Schedule.route,  Icons.Rounded.DateRange),
        BottomNavItem(stringResource(R.string.nav_assistant), Screen.AiChat.route,    Icons.Rounded.AutoAwesome),
        BottomNavItem(stringResource(R.string.nav_analytics), Screen.Analytics.route, Icons.Rounded.BarChart),
        BottomNavItem(stringResource(R.string.nav_settings),  Screen.Settings.route,  Icons.Rounded.Settings),
    )

    // Resolve system navigation bar inset so the pill floats above the gesture bar.
    // asPaddingValues() is a @Composable call — must be called directly, not inside remember {}.
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // pillColor: visible in both themes (≥ 92 % alpha).
    // Dark  → GlassSurfaceDark tinted pill.
    // Light → theme surface so it always contrasts the background.
    val pillColor   = if (isDark) GlassSurfaceDark.copy(alpha = 0.92f)
                      else        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val glassBorder = if (isDark) GlassBorderDark else GlassBorderLight

    val pillShape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start  = 24.dp,
                end    = 24.dp,
                bottom = navBarInset + 24.dp,  // float 24 dp above gesture bar / buttons
                top    = 0.dp,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Floating pill surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation    = 10.dp,
                    shape        = pillShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    spotColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isDark) Color.White.copy(alpha = 0.18f)
                            else        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            glassBorder,
                        )
                    ),
                    shape = pillShape,
                ),
            shape           = pillShape,
            color           = pillColor,   // guaranteed visible — min 92% alpha
            tonalElevation  = 0.dp,
            shadowElevation = 0.dp,        // handled by Modifier.shadow above
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    FloatingNavItem(
                        item     = item,
                        selected = selected,
                        isDark   = isDark,
                        onClick  = {
                            // Guard via hierarchy so nested graphs don't re-navigate
                            val alreadyOn = currentDestination?.hierarchy
                                ?.any { it.route == item.route } == true
                            if (!alreadyOn) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState   = true
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: BottomNavItem,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val activeBackground by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
                      else          Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navItemBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                      else          MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navItemContent",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 22.dp else 20.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navItemIconSize",
    )

    val pillShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .clip(pillShape)
            .background(color = activeBackground, shape = pillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = item.title,
                tint               = contentColor,
                modifier           = Modifier.size(iconSize),
            )
            if (selected) {
                Text(
                    text  = item.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = contentColor,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

private data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
)
