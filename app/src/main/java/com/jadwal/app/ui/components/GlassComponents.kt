package com.jadwal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jadwal.ui.theme.*

/**
 * GlassCard — بطاقة بتأثير الزجاج (Glass Morphism)
 *
 * إصلاح: استبدال isSystemInDarkTheme() بـ LocalAppDarkTheme
 * حتى تستجيب البطاقات لتغيير الثيم من داخل الإعدادات فوراً
 * إصلاح: زيادة شفافية الوضع الفاتح لتظهر البطاقات بوضوح
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = JadwalRadius.lg,
    glassAlpha: Float = 0.15f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = LocalAppDarkTheme.current
    val shape = RoundedCornerShape(cornerRadius)

    // في الوضع الفاتح: نستخدم خلفية بيضاء شبه معتمة لإظهار البطاقات بوضوح
    // في الوضع الداكن: نستخدم الزجاج الداكن الشفاف كما كان
    val effectiveColor = if (isDark) {
        GlassSurfaceDark.copy(alpha = glassAlpha.coerceIn(0.05f, 0.95f))
    } else {
        Color.White.copy(alpha = glassAlpha.coerceIn(0.55f, 0.95f))
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        JadwalIndigo.copy(alpha = 0.08f)
    }

    val baseModifier = modifier
        .clip(shape)
        .background(color = effectiveColor, shape = shape)
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    if (isDark) Color.White.copy(alpha = 0.15f) else JadwalIndigo.copy(alpha = 0.10f),
                    borderColor,
                )
            ),
            shape = shape,
        )

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else baseModifier

    Box(
        modifier = finalModifier,
        content = content
    )
}

/**
 * JadwalBackground — الخلفية التدرجية الموحّدة للتطبيق
 * إصلاح: استبدال isSystemInDarkTheme() بـ LocalAppDarkTheme
 */
@Composable
fun JadwalBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = LocalAppDarkTheme.current
    val gradientColors = if (isDark) {
        listOf(GradientDarkStart, GradientDarkMid, GradientDarkEnd)
    } else {
        listOf(GradientLightStart, GradientLightMid, GradientLightEnd)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
    ) {
        // دائرة ضبابية — أعلى اليسار
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) JadwalViolet.copy(alpha = 0.22f) else JadwalIndigo.copy(alpha = 0.09f),
                            Color.Transparent,
                        )
                    ),
                    shape = CircleShape
                )
                .blur(radius = 80.dp)
        )

        // دائرة ضبابية — أسفل اليمين
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) JadwalIndigo.copy(alpha = 0.18f) else JadwalViolet.copy(alpha = 0.07f),
                            Color.Transparent,
                        )
                    ),
                    shape = CircleShape
                )
                .blur(radius = 70.dp)
        )

        content()
    }
}
