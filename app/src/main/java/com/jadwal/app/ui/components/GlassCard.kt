package com.jadwal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jadwal.ui.theme.JadwalRadius
import com.jadwal.ui.theme.jadwalColors

/**
 * GlassCard — البطاقة الزجاجية الأساسية في التطبيق
 *
 * الاستخدام:
 * ```
 * GlassCard(modifier = Modifier.fillMaxWidth()) {
 *     Text("محتوى الكارد")
 * }
 * ```
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glassAlpha: Float = 0.15f,        // شفافية سطح الزجاج
    borderAlpha: Float = 0.3f,        // شفافية الحدود
    cornerRadius: Dp = JadwalRadius.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // لون الزجاج — أكثر شفافية في الداكن
    val glassColor = if (isDark) {
        Color.White.copy(alpha = glassAlpha * 0.4f)
    } else {
        Color.White.copy(alpha = glassAlpha + 0.6f)
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .background(color = glassColor, shape = shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha + 0.2f),
                        Color.White.copy(alpha = borderAlpha * 0.2f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = shape
            )
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        content = content
    )
}

/**
 * GlassCard بـ padding افتراضي للاستخدام السريع
 */
@Composable
fun GlassCardPadded(
    modifier: Modifier = Modifier,
    glassAlpha: Float = 0.15f,
    borderAlpha: Float = 0.3f,
    cornerRadius: Dp = JadwalRadius.lg,
    innerPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        glassAlpha = glassAlpha,
        borderAlpha = borderAlpha,
        cornerRadius = cornerRadius,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
