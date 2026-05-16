package com.jadwal.ui.navigation

/**
 * Screen — تعريف جميع الشاشات في التطبيق
 */
sealed class Screen(val route: String) {
    // ===== شاشات لا تظهر فيها الـ BottomBar =====
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Setup : Screen("setup")

    // ===== شاشات الـ BottomBar الرئيسية =====
    data object Home : Screen("home")
    data object Schedule : Screen("schedule")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")

    // ===== شاشات فرعية =====
    data object Session : Screen("session")

    companion object {
        // الشاشات التي تظهر فيها الـ BottomBar
        val bottomBarScreens = listOf(
            Home.route,
            Schedule.route,
            Analytics.route,
            Settings.route,
        )

        // الشاشات التي لا تظهر فيها الـ BottomBar
        val fullScreenRoutes = listOf(
            Onboarding.route,
            Login.route,
            Register.route,
            ForgotPassword.route,
            Setup.route,
        )
    }
}
