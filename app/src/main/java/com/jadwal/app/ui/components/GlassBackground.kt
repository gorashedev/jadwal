package com.jadwal.ui.components

import androidx.compose.foundation.background
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
import com.jadwal.ui.theme.LocalAppDarkTheme

/**
 * JadwalBackground — الخلفية الرئيسية لكل شاشة
 *
 * إصلاح: استخدام LocalAppDarkTheme.current بدلاً من isSystemInDarkTheme()
 * حتى تعكس الخلفية اختيار المستخدم من الإعدادات (فاتح/داكن) وليس إعداد الجهاز فقط.
 *
 * الوضع الفاتح: خلفية بيضاء نقية مع تدرج خفيف — نص داكن واضح تماماً
 * الوضع الداكن: الخلفية الداكنة المألوفة ذات البنفسجي العميق
 */
@Composable
fun JadwalBackground(
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalAppDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = if (isDark) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A1040),
                            Color(0xFF0D0D1A),
                            Color(0xFF0A1628),
                        ),
                        radius = 1800f,
                    )
                } else {
                    // وضع فاتح: أبيض ناصع مع انتقال ناعم نحو الأسفل
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),   // أبيض ناصع في الأعلى
                            Color(0xFFF5F7FF),   // أبيض مع لمسة بنفسجية في المنتصف
                            Color(0xFFF0F4FF),   // أفتح قليلاً في الأسفل
                        )
                    )
                }
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
                JadwalIndigo.copy(alpha = 0.07f)
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
                JadwalViolet.copy(alpha = 0.05f)
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
                JadwalViolet.copy(alpha = 0.03f)
        )

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
