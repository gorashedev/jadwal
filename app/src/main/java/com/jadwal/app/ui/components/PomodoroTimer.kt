package com.jadwal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jadwal.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jadwal.R
import com.jadwal.ui.theme.JadwalIndigo
import com.jadwal.ui.theme.JadwalSuccess
import com.jadwal.ui.theme.JetBrainsMonoFont

/**
 * PomodoroTimerCircle — مؤقت Pomodoro الدائري الرئيسي
 *
 * الاستخدام:
 * ```
 * PomodoroTimerCircle(
 *     timeLeft = pomodoroState.timeLeftSeconds,
 *     totalTime = pomodoroState.totalSeconds,
 *     isWorking = pomodoroState.isWorkPhase,
 *     cycle = pomodoroState.currentCycle,
 *     modifier = Modifier.size(280.dp)
 * )
 * ```
 */
@Composable
fun PomodoroTimerCircle(
    timeLeft: Int,
    totalTime: Int,
    isWorking: Boolean,
    cycle: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime.toFloat() else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "pomodoro_progress"
    )

    val timerColor by animateColorAsState(
        targetValue = if (isWorking) JadwalIndigo else JadwalSuccess,
        animationSpec = tween(500),
        label = "timer_color"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // الدائرة الخلفية المضيئة
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            timerColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // الدوائر المتحركة
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // الحلقة الخارجية الفاتحة (track)
            drawCircle(
                color = timerColor.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // حلقة التقدم
            drawArc(
                color = timerColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // النصوص في المنتصف
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // الوقت المتبقي
            Text(
                text = formatPomodoroTime(timeLeft),
                fontFamily = JetBrainsMonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(4.dp))

            // نوع الفترة (تركيز / راحة)
            Text(
                text = if (isWorking)
                    stringResource(R.string.focus_time)
                else
                    stringResource(R.string.break_time),
                style = MaterialTheme.typography.bodyMedium,
                color = timerColor
            )

            Spacer(Modifier.height(2.dp))

            // رقم الجولة
            Text(
                text = stringResource(R.string.cycle_x, cycle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * PomodoroProgress — مؤشرات الجولات المنتهية
 */
@Composable
fun PomodoroProgress(
    completedPomodoros: Int,
    totalPomodoros: Int,
    modifier: Modifier = Modifier,
    color: Color = JadwalIndigo,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPomodoros) { index ->
            val isDone = index < completedPomodoros
            Box(
                modifier = Modifier
                    .size(if (isDone) 12.dp else 10.dp)
                    .background(
                        color = if (isDone) color else color.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            )
            if (index < totalPomodoros - 1) {
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

/** تحويل الثواني لصيغة mm:ss */
fun formatPomodoroTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
