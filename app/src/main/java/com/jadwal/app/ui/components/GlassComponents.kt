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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jadwal.ui.theme.*

/**
 * GlassCard — بطاقة بتأثير الزجاج
 *
 * إصلاح الوضع الفاتح: زيادة معتمية البطاقة البيضاء لتظهر بوضوح
 * على خلفية الألوان الفاتحة، مع تحسين الحدود وإضافة ظل خفيف.
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

    // الوضع الفاتح: بطاقة بيضاء معتمة بما يكفي لتتميز عن الخلفية
    // الوضع الداكن: الزجاج الداكن الشفاف
    val effectiveColor = if (isDark) {
        GlassSurfaceDark.copy(alpha = glassAlpha.coerceIn(0.05f, 0.90f))
    } else {
        Color.White.copy(alpha = 0.90f)
    }

    val borderGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.06f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                JadwalIndigo.copy(alpha = 0.18f),
                JadwalViolet.copy(alpha = 0.08f),
            )
        )
    }

    val shadowModifier = if (!isDark) {
        modifier.shadow(
            elevation = 2.dp,
            shape = shape,
            ambientColor = JadwalIndigo.copy(alpha = 0.08f),
            spotColor = JadwalIndigo.copy(alpha = 0.10f),
        )
    } else modifier

    val baseModifier = shadowModifier
        .clip(shape)
        .background(color = effectiveColor, shape = shape)
        .border(width = 1.dp, brush = borderGradient, shape = shape)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else baseModifier

    Box(modifier = finalModifier, content = content)
}

/**
 * JadwalBackground — الخلفية التدرجية الموحّدة للتطبيق
 *
 * إصلاح الوضع الفاتح: ألوان أكثر وضوحاً وتباعداً لتُشعر المستخدم
 * بالعمق بدلاً من خلفية بيضاء مسطّحة.
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
        // ألوان فاتحة ذات تدرج محسوس لتمنح عمقاً في الوضع الفاتح
        listOf(
            Color(0xFFECEEFD),  // بنفسجي فاتح مزرق
            Color(0xFFF0F3FF),  // أفتح قليلاً في المنتصف
            Color(0xFFF5EEFF),  // دفء خفيف في الأسفل
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = gradientColors))
    ) {
        // دائرة ضبابية أعلى اليسار
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) JadwalViolet.copy(alpha = 0.22f)
                            else JadwalIndigo.copy(alpha = 0.13f),
                            Color.Transparent,
                        )
                    ),
                    shape = CircleShape,
                )
                .blur(radius = 80.dp)
        )

        // دائرة ضبابية أسفل اليمين
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) JadwalIndigo.copy(alpha = 0.18f)
                            else JadwalViolet.copy(alpha = 0.11f),
                            Color.Transparent,
                        )
                    ),
                    shape = CircleShape,
                )
                .blur(radius = 70.dp)
        )

        content()
    }
}
