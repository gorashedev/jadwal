package com.jadwal.ui.navigation

sealed class Screen(val route: String) {
    // ===== شاشات لا تظهر فيها الـ BottomBar =====
    data object Onboarding     : Screen("onboarding")
    data object Login          : Screen("login")
    data object Register       : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Setup          : Screen("setup")

    // ─── إصلاح #3: إضافة شاشة إعادة تعيين كلمة المرور ───────────────────
    // تستقبل الـ fragment من Deep Link كـ argument
    data object ResetPassword  : Screen("reset_password/{fragment}") {
        fun createRoute(encodedFragment: String) = "reset_password/$encodedFragment"
    }

    // ===== شاشات الـ BottomBar الرئيسية =====
    data object Home      : Screen("home")
    data object Schedule  : Screen("schedule")
    data object AiChat    : Screen("ai_chat")
    data object Analytics : Screen("analytics")
    data object Settings  : Screen("settings")

    // ===== شاشات فرعية =====
    data object Session      : Screen("session")
    data object AISuggestion : Screen("ai_suggestion")
    data object Profile      : Screen("profile")
    data object Subjects     : Screen("subjects")
    data object ExamScan     : Screen("exam_scan")

    companion object {
        val bottomBarScreens by lazy {
            listOf(
                Home.route,
                Schedule.route,
                AiChat.route,
                Analytics.route,
                Settings.route,
            )
        }

        val fullScreenRoutes by lazy {
            listOf(
                Onboarding.route,
                Login.route,
                Register.route,
                ForgotPassword.route,
                Setup.route,
                ResetPassword.route,
                Session.route,
                AISuggestion.route,
                Profile.route,
                Subjects.route,
                ExamScan.route,
            )
        }
    }
}