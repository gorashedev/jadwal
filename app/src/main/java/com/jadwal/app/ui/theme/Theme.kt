package com.jadwal.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===== Light Color Scheme =====
private val LightColorScheme = lightColorScheme(
    primary = JadwalIndigo,
    onPrimary = NeutralWhite,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00105C),

    secondary = JadwalViolet,
    onSecondary = NeutralWhite,
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF21005D),

    tertiary = JadwalSuccess,
    onTertiary = NeutralWhite,
    tertiaryContainer = Color(0xFFD0F5E0),
    onTertiaryContainer = Color(0xFF00391E),

    error = JadwalError,
    onError = NeutralWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = GlassBackgroundLight,
    onBackground = Neutral900,

    surface = Color(0xFFFAFAFF),
    onSurface = Neutral900,
    surfaceVariant = Color(0xFFE3E5F5),
    onSurfaceVariant = Color(0xFF44474F),

    surfaceTint = JadwalIndigo,
    inverseSurface = Neutral800,
    inverseOnSurface = Neutral100,
    inversePrimary = JadwalIndigoLight,

    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC4C6D0),
    scrim = Color(0xFF000000),

    surfaceBright = NeutralWhite,
    surfaceContainer = Color(0xFFEBEDFD),
    surfaceContainerHigh = Color(0xFFE5E7F8),
    surfaceContainerHighest = Color(0xFFDFE1F2),
    surfaceContainerLow = Color(0xFFF1F3FE),
    surfaceContainerLowest = NeutralWhite,
    surfaceDim = Color(0xFFD9DBF0),
)

// ===== Dark Color Scheme =====
private val DarkColorScheme = darkColorScheme(
    primary = JadwalIndigoLight,
    onPrimary = Color(0xFF0E2278),
    primaryContainer = JadwalIndigoDark,
    onPrimaryContainer = Color(0xFFDDE1FF),

    secondary = JadwalVioletLight,
    onSecondary = Color(0xFF3700B3),
    secondaryContainer = JadwalVioletDark,
    onSecondaryContainer = Color(0xFFEADDFF),

    tertiary = Color(0xFF6EDBA6),
    onTertiary = Color(0xFF00391E),
    tertiaryContainer = Color(0xFF005234),
    onTertiaryContainer = Color(0xFFD0F5E0),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = GlassBackgroundDark,
    onBackground = Color(0xFFF0F0FF),

    surface = GlassSurfaceDark,
    onSurface = Color(0xFFEEEEFF),
    surfaceVariant = Color(0xFF2A2D3A),
    onSurfaceVariant = Color(0xFFCACDD8),

    surfaceTint = JadwalIndigoLight,
    inverseSurface = Color(0xFFE5E5F0),
    inverseOnSurface = Neutral800,
    inversePrimary = JadwalIndigo,

    outline = Color(0xFF9194A0),
    outlineVariant = Color(0xFF44474F),

    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2F3145),
    surfaceContainer = Color(0xFF1A1B2E),
    surfaceContainerHigh = Color(0xFF242539),
    surfaceContainerHighest = Color(0xFF2E2F45),
    surfaceContainerLow = Color(0xFF12132A),
    surfaceContainerLowest = Color(0xFF0A0B1C),
    surfaceDim = GlassBackgroundDark,
)

// ===== ألوان موسّعة لـ Glass Morphism =====
data class JadwalExtendedColors(
    val glassSurface: Color,
    val glassBorder: Color,
    val glassBackground: Color,
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
)

val LocalJadwalColors = staticCompositionLocalOf {
    JadwalExtendedColors(
        glassSurface = GlassSurfaceLight,
        glassBorder = GlassBorderLight,
        glassBackground = GlassBackgroundLight,
        gradientStart = GradientLightStart,
        gradientMid = GradientLightMid,
        gradientEnd = GradientLightEnd,
        success = JadwalSuccess,
        warning = JadwalWarning,
        info = JadwalInfo,
    )
}

/**
 * LocalAppDarkTheme — يعكس وضع الثيم الفعلي (مستخدَم في Glass components بدلاً من isSystemInDarkTheme)
 * حتى يتم تطبيق الألوان الصحيحة عند اختيار وضع فاتح/داكن من داخل الإعدادات
 */
val LocalAppDarkTheme = compositionLocalOf { false }

@Composable
fun JadwalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val extendedColors = if (darkTheme) {
        JadwalExtendedColors(
            glassSurface = GlassSurfaceDark,
            glassBorder = GlassBorderDark,
            glassBackground = GlassBackgroundDark,
            gradientStart = GradientDarkStart,
            gradientMid = GradientDarkMid,
            gradientEnd = GradientDarkEnd,
            success = Color(0xFF6EDBA6),
            warning = Color(0xFFFFCC80),
            info = Color(0xFF81D4FA),
        )
    } else {
        JadwalExtendedColors(
            glassSurface = GlassSurfaceLight,
            glassBorder = GlassBorderLight,
            glassBackground = GlassBackgroundLight,
            gradientStart = GradientLightStart,
            gradientMid = GradientLightMid,
            gradientEnd = GradientLightEnd,
            success = JadwalSuccess,
            warning = JadwalWarning,
            info = JadwalInfo,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalJadwalColors provides extendedColors,
        LocalAppDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JadwalTypography,
            shapes = JadwalShapes,
            content = content
        )
    }
}

val MaterialTheme.jadwalColors: JadwalExtendedColors
    @Composable get() = LocalJadwalColors.current
