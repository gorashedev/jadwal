package com.jadwal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jadwal.ui.theme.*

/**
 * GlassCard — بطاقة بتأثير الزجاج (Glass Morphism)
 *
 * @param modifier       Modifier الخارجي
 * @param cornerRadius   نصف قطر الزوايا
 * @param glassAlpha     شفافية الزجاج (0.0 = شفاف تماماً، 1.0 = معتم)
 * @param onClick        إذا حُدِّد، تصبح البطاقة قابلة للنقر
 * @param content        محتوى البطاقة
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = JadwalRadius.lg,
    glassAlpha: Float = 0.15f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
    val shape = RoundedCornerShape(cornerRadius)

    val baseModifier = modifier
        .clip(shape)
        .background(
            color = surfaceColor.copy(alpha = glassAlpha.coerceIn(0.01f, 0.95f)),
            shape = shape,
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.15f else 0.4f),
                    Color.White.copy(alpha = if (isDark) 0.05f else 0.1f),
                )
            ),
            shape = shape,
        )

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // يمكن إضافة Indication إذا لزم الأمر
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
 * تُستخدم كـ Root في كل شاشة
 */
@Composable
fun JadwalBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(GradientDarkStart, GradientDarkMid, GradientDarkEnd)
    } else {
        listOf(GradientLightStart, GradientLightMid, GradientLightEnd)
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            ),
        content = content,
    )
}

// (Removed custom luminance check in favor of isSystemInDarkTheme)
