package com.jadwal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwal.ui.theme.JadwalIndigo

/**
 * ProgressRing — حلقة تقدم دائرية متحركة
 *
 * الاستخدام:
 * ```
 * ProgressRing(
 *     progress = 0.72f,
 *     color = JadwalIndigo,
 *     size = 120.dp,
 *     label = "72%"
 * )
 * ```
 */
@Composable
fun ProgressRing(
    progress: Float,             // 0.0f → 1.0f
    modifier: Modifier = Modifier,
    color: Color = JadwalIndigo,
    trackColor: Color = color.copy(alpha = 0.15f),
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    label: String = "",
    sublabel: String = "",
    animationDuration: Int = 1000,
) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress) {
        animatedProgress = progress
    }

    val animatedValue by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = animationDuration),
        label = "progress_ring"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.toPx() - strokePx
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(diameter, diameter)

            // الحلقة الخلفية (Track)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // حلقة التقدم
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedValue,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // النص في المنتصف
        if (label.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = (size.value * 0.18f).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
                if (sublabel.isNotEmpty()) {
                    Text(
                        text = sublabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (size.value * 0.11f).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * LinearProgressBar — شريط تقدم أفقي مخصص
 */
@Composable
fun JadwalLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = JadwalIndigo,
    trackColor: Color = color.copy(alpha = 0.15f),
    height: Dp = 8.dp,
    animationDuration: Int = 800,
) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress) {
        animatedProgress = progress.coerceIn(0f, 1f)
    }

    val animatedValue by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = animationDuration),
        label = "linear_progress"
    )

    Canvas(
        modifier = modifier.height(height)
    ) {
        val radius = height.toPx() / 2

        // الخلفية
        drawRoundRect(
            color = trackColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
        )

        // التقدم
        if (animatedValue > 0f) {
            drawRoundRect(
                color = color,
                size = Size(size.width * animatedValue, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            )
        }
    }
}
