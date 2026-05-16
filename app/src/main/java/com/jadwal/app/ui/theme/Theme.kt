package com.jadwal.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
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

    surface = GlassSurfaceLight,
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
    onBackground = Neutral100,

    surface = GlassSurfaceDark,
    onSurface = Neutral100,
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),

    surfaceTint = JadwalIndigoLight,
    inverseSurface = Neutral100,
    inverseOnSurface = Neutral800,
    inversePrimary = JadwalIndigo,

    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),

    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF37393E),
    surfaceContainer = GlassSurfaceDark,
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF333540),
    surfaceContainerLow = Color(0xFF1C1E23),
    surfaceContainerLowest = Color(0xFF0F1115),
    surfaceDim = GlassBackgroundDark,
)

// ===== Extended Colors (ألوان إضافية خارج Material) =====
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

// ===== الثيم الرئيسي =====
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

    // status bar و navigation bar شفافة وتتكيف مع الثيم
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
        LocalJadwalColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JadwalTypography,
            shapes = JadwalShapes,
            content = content
        )
    }
}

// ===== Extension للوصول السريع =====
val MaterialTheme.jadwalColors: JadwalExtendedColors
    @Composable get() = LocalJadwalColors.current
