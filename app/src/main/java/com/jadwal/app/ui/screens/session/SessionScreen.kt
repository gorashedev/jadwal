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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.domain.model.UnderstandingLevel
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.components.PomodoroProgress
import com.jadwal.ui.components.PomodoroTimerCircle
import com.jadwal.ui.components.UnderstandingDialog
import com.jadwal.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.jadwal.R

@Composable
fun SessionScreen(
    scheduleItemId: String,
    viewModel: SessionViewModel = hiltViewModel(),
    onSessionEnd: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSessionEnd()
    }

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
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.end_session))
                }
                Text(
                    text = if (uiState.isBreak) stringResource(R.string.rest_break) else uiState.subjectName,
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

            // نوع الدورة
            AnimatedVisibility(visible = uiState.isBreak) {
                Surface(
                    color = JadwalSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.break_encourage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = JadwalSuccess,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ===== مؤقت Pomodoro الدائري =====
            PomodoroTimerCircle(
                timeLeft = uiState.remainingSeconds,
                totalTime = if (uiState.isBreak) 5 * 60 else 25 * 60,
                isWorking = !uiState.isBreak,
                cycle = uiState.currentPomodoroIndex + 1,
                modifier = Modifier.size(280.dp),
            )

            Spacer(Modifier.height(16.dp))

            // ===== نقاط تقدم Pomodoro =====
            PomodoroProgress(
                completedPomodoros = uiState.currentPomodoroIndex,
                totalPomodoros = 4,
                modifier = Modifier.padding(horizontal = 24.dp),
                color = if (uiState.isBreak) JadwalSuccess else JadwalIndigo,
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
                FilledTonalIconButton(
                    onClick = viewModel::endSession,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = stringResource(R.string.end_session), modifier = Modifier.size(24.dp))
                }

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
                            contentDescription = if (isRunning) stringResource(R.string.pause) else stringResource(R.string.play),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = viewModel::skipBreak,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.skip_rating), modifier = Modifier.size(24.dp))
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
                text = stringResource(R.string.session_progress),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$elapsedMinutes / $totalMinutes " + stringResource(R.string.minute_abbrev),
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
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.session_rating_question, subjectName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            // إحصائيات الجلسة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SessionStatChip("⏱️", "$minutesStudied", stringResource(R.string.minute_short))
                SessionStatChip("🍅", "$pomodoroCount", stringResource(R.string.pomodoro))
            }

            Text(
                text = stringResource(R.string.rate_understanding),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // أزرار التقييم
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UnderstandingButton(
                    emoji = "🤩",
                    title = stringResource(R.string.understanding_excellent),
                    subtitle = stringResource(R.string.understanding_excellent_desc),
                    color = JadwalSuccess,
                    onClick = { onRate(UnderstandingLevel.EXCELLENT) },
                    isLoading = isSaving,
                )
                UnderstandingButton(
                    emoji = "👍",
                    title = stringResource(R.string.understanding_good),
                    subtitle = stringResource(R.string.understanding_good_desc),
                    color = JadwalIndigo,
                    onClick = { onRate(UnderstandingLevel.GOOD) },
                    isLoading = isSaving,
                )
                UnderstandingButton(
                    emoji = "😐",
                    title = stringResource(R.string.understanding_medium),
                    subtitle = stringResource(R.string.understanding_medium_desc),
                    color = JadwalWarning,
                    onClick = { onRate(UnderstandingLevel.AVERAGE) },
                    isLoading = isSaving,
                )
                UnderstandingButton(
                    emoji = "😟",
                    title = stringResource(R.string.understanding_weak),
                    subtitle = stringResource(R.string.understanding_weak_desc),
                    color = JadwalError,
                    onClick = { onRate(UnderstandingLevel.POOR) },
                    isLoading = isSaving,
                )
            }

            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip_rating), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SessionStatChip(emoji: String, value: String, unit: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun UnderstandingButton(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    isLoading: Boolean,
) {
    Surface(
        onClick = { if (!isLoading) onClick() },
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(emoji, fontSize = 26.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = color,
                )
            }
        }
    }
}
