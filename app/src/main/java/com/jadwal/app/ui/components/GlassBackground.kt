package com.jadwal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jadwal.ui.theme.JadwalIndigo
import com.jadwal.ui.theme.JadwalViolet

/**
 * JadwalBackground — الخلفية الرئيسية لكل شاشة
 *
 * تتضمن:
 * - Gradient خلفية (Radial)
 * - دوائر ضبابية ملونة للعمق (Ambient Blobs)
 *
 * الاستخدام:
 * ```
 * JadwalBackground {
 *     // محتوى الشاشة
 * }
 * ```
 */
@Composable
fun JadwalBackground(
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = if (isDark) listOf(
                        Color(0xFF1A1040),
                        Color(0xFF0D0D1A),
                        Color(0xFF0A1628),
                    ) else listOf(
                        Color(0xFFE8EAF6),
                        Color(0xFFEDE7F6),
                        Color(0xFFE3F2FD),
                    ),
                    radius = 1800f,
                )
            )
    ) {
        // دائرة ضبابية — أعلى اليسار
        AmbientBlob(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-80).dp, y = (-80).dp),
            color = if (isDark)
                JadwalViolet.copy(alpha = 0.28f)
            else
                JadwalIndigo.copy(alpha = 0.13f)
        )

        // دائرة ضبابية — أسفل اليمين
        AmbientBlob(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 70.dp),
            color = if (isDark)
                JadwalIndigo.copy(alpha = 0.22f)
            else
                JadwalViolet.copy(alpha = 0.09f)
        )

        // دائرة ضبابية — المنتصف (خفية جداً)
        AmbientBlob(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .offset(x = 80.dp, y = (-120).dp),
            color = if (isDark)
                JadwalIndigo.copy(alpha = 0.10f)
            else
                JadwalViolet.copy(alpha = 0.05f)
        )

        // المحتوى الفعلي فوق الخلفية
        content()
    }
}

/**
 * AmbientBlob — دائرة ضبابية ملونة لإضافة عمق بصري
 */
@Composable
fun AmbientBlob(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent)
                ),
                shape = CircleShape
            )
            .blur(radius = 90.dp)
    )
}
