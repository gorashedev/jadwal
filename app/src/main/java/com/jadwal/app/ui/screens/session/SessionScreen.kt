package com.jadwal.ui.screens.session

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.domain.model.UnderstandingLevel
import com.jadwal.ui.components.*
import com.jadwal.ui.theme.*
import androidx.compose.foundation.Canvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SessionScreen(
    scheduleItemId: String,
    viewModel: SessionViewModel = hiltViewModel(),
    onSessionEnd: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // الانتقال لخارج الشاشة بعد الحفظ
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSessionEnd()
    }

    // ورقة تقييم الفهم
    if (uiState.showRatingSheet) {
        UnderstandingRatingSheet(
            subjectName = uiState.subjectName,
            minutesStudied = uiState.elapsedSeconds / 60,
            pomodoroCount = uiState.currentPomodoroIndex,
            isSaving = uiState.isSaving,
            onRate = viewModel::saveSession,
            onSkip = viewModel::dismissRatingSheet,
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ===== شريط العنوان =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (uiState.timerState == TimerState.RUNNING) {
                        viewModel.toggleTimer()
                    }
                    viewModel.endSession()
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "إنهاء")
                }
                Text(
                    text = if (uiState.isBreak) "فترة الراحة ☕" else uiState.subjectName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                // عداد الدورات
                Surface(
                    color = JadwalIndigo.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(
                        text = "🍅 ×${uiState.currentPomodoroIndex}",
                        style = MaterialTheme.typography.labelMedium,
                        color = JadwalIndigo,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // ===== نوع الدورة الحالية =====
            AnimatedVisibility(visible = uiState.isBreak) {
                Surface(
                    color = JadwalSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text(
                        text = "استرح قليلاً، عقلك يستحق ذلك 🌿",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JadwalSuccess,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ===== المؤقت الدائري =====
            CircularTimer(
                remainingSeconds = uiState.remainingSeconds,
                totalSeconds = if (uiState.isBreak) 5 * 60 else 25 * 60,
                timerState = uiState.timerState,
                isBreak = uiState.isBreak,
                modifier = Modifier.size(280.dp),
            )

            Spacer(Modifier.weight(1f))

            // ===== أزرار التحكم =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // زر الإنهاء المبكر
                FilledTonalIconButton(
                    onClick = viewModel::endSession,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = "إنهاء", modifier = Modifier.size(24.dp))
                }

                // زر التشغيل / الإيقاف المؤقت
                val bgColor = if (uiState.isBreak) JadwalSuccess else JadwalIndigo
                FloatingActionButton(
                    onClick = viewModel::toggleTimer,
                    modifier = Modifier.size(80.dp),
                    containerColor = bgColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    AnimatedContent(
                        targetState = uiState.timerState == TimerState.RUNNING,
                        label = "play_pause_icon",
                    ) { isRunning ->
                        Icon(
                            imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isRunning) "إيقاف مؤقت" else "تشغيل",
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                // زر التخطي للراحة / الدورة التالية
                FilledTonalIconButton(
                    onClick = {
                        if (uiState.isBreak) {
                            // تخطي الراحة
                        }
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "تخطي", modifier = Modifier.size(24.dp))
                }
            }

            // ===== شريط التقدم الكلي =====
            SessionProgressBar(
                elapsedMinutes = uiState.elapsedSeconds / 60,
                totalMinutes = uiState.totalMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            )
        }
    }
}

// ===== المؤقت الدائري =====
@Composable
fun CircularTimer(
    remainingSeconds: Int,
    totalSeconds: Int,
    timerState: TimerState,
    isBreak: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = EaseInOutCubic),
        label = "timer_progress",
    )

    // نبض خفيف عند التشغيل
    val pulseScale by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 1f,
            targetValue = if (timerState == TimerState.RUNNING) 1.02f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse_scale",
        )

    val arcColor = if (isBreak) JadwalSuccess else JadwalIndigo
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            // المسار الخلفي
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // المسار المتقدم
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        arcColor.copy(alpha = 0.5f),
                        arcColor,
                        arcColor,
                    ),
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // نقطة نهاية المسار
            if (animatedProgress > 0.01f) {
                val angle = (-90f + 360f * animatedProgress) * (PI / 180f).toFloat()
                val radius = diameter / 2f
                val cx = size.width / 2f + radius * cos(angle)
                val cy = size.height / 2f + radius * sin(angle)
                drawCircle(
                    color = arcColor,
                    radius = strokeWidth / 2f,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                )
            }
        }

        // النص في وسط الدائرة
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60

            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 52.sp,
            )
            Text(
                text = when (timerState) {
                    TimerState.IDLE -> "اضغط للبدء"
                    TimerState.RUNNING -> if (isBreak) "استراحة 🌿" else "تركّز 🎯"
                    TimerState.PAUSED -> "متوقف مؤقتاً"
                    TimerState.BREAK -> "استراحة 🌿"
                    TimerState.FINISHED -> "أحسنت! 🎉"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ===== شريط التقدم الكلي =====
@Composable
fun SessionProgressBar(
    elapsedMinutes: Int,
    totalMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalMinutes > 0) elapsedMinutes.toFloat() / totalMinutes else 0f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "تقدم الجلسة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$elapsedMinutes / $totalMinutes د",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = JadwalIndigo,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// ===== ورقة تقييم الفهم =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnderstandingRatingSheet(
    subjectName: String,
    minutesStudied: Int,
    pomodoroCount: Int,
    isSaving: Boolean,
    onRate: (UnderstandingLevel) -> Unit,
    onSkip: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "رائع! أنهيت الجلسة 🎉",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(8.dp))

            // إحصاء الجلسة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SessionStatChip("⏱️", "$minutesStudied د", "مذاكرة")
                SessionStatChip("🍅", "$pomodoroCount", "دورات")
                SessionStatChip("📚", subjectName.take(8), "المادة")
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "كيف كان مستوى فهمك؟",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(16.dp))

            // مستويات الفهم
            val levels = listOf(
                UnderstandingLevel.POOR to Triple("😕", "ضعيف", MaterialTheme.colorScheme.error),
                UnderstandingLevel.PARTIAL to Triple("😐", "متوسط", JadwalWarning),
                UnderstandingLevel.GREAT to Triple("😊", "جيد", JadwalSuccess),
                UnderstandingLevel.EXCELLENT to Triple("🤩", "ممتاز!", JadwalIndigo),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                levels.forEach { (level, info) ->
                    val (emoji, label, color) = info
                    FilledTonalButton(
                        onClick = { if (!isSaving) onRate(level) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = color.copy(alpha = 0.12f),
                            contentColor = color,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp),
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 24.sp)
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("تخطي التقييم", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SessionStatChip(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
