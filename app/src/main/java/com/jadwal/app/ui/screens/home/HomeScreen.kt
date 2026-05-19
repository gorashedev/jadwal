package com.jadwal.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.R
import com.jadwal.ui.components.GlassCard
import java.util.Calendar
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.components.NotificationPermissionHandler
import com.jadwal.ui.components.NotificationPermissionViewModel
import com.jadwal.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    permissionViewModel: NotificationPermissionViewModel = hiltViewModel(),
    onStartSession: (String) -> Unit,
    onViewSchedule: () -> Unit,
    onViewReport: () -> Unit,
    onViewAISuggestion: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiSuggestion by viewModel.aiSuggestion.collectAsStateWithLifecycle()
    val permissionHandled by permissionViewModel.permissionHandled.collectAsStateWithLifecycle()

    if (!permissionHandled) {
        NotificationPermissionHandler(
            onGranted = permissionViewModel::onPermissionGranted,
            onDenied = permissionViewModel::onPermissionDenied,
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 100.dp)
        ) {
            val greetingHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            val greeting = when {
                greetingHour in 5..11  -> "${stringResource(R.string.good_morning)} 🌅"
                greetingHour in 12..16 -> "${stringResource(R.string.good_afternoon)} ☀️"
                greetingHour in 17..20 -> "${stringResource(R.string.good_evening)} 🌆"
                else                   -> "${stringResource(R.string.good_night)} 🌙"
            }
            HomeHeader(
                greeting = greeting,
                userName = uiState.userName,
                streakDays = uiState.streakDays,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
            )

            AISuggestionCard(
                suggestion = aiSuggestion,
                isLoading = uiState.isAiLoading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            DailyProgressCard(
                completedMinutes = uiState.completedMinutes,
                totalMinutes = uiState.totalPlannedMinutes,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // ─── عنوان مهام اليوم ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // إصلاح #4: كان "مهام اليوم" — الآن stringResource
                    text = stringResource(R.string.today_tasks),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onViewSchedule) {
                    // إصلاح #4: كان "الجدول الكامل" — الآن stringResource
                    Text(stringResource(R.string.view_full_schedule))
                }
            }

            if (uiState.todayTasks.isEmpty()) {
                EmptyTasksCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            } else {
                uiState.todayTasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onStartSession = { onStartSession(task.scheduleItemId) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            uiState.upcomingExam?.let { exam ->
                if (exam.daysUntil <= 7) {
                    ExamAlertCard(
                        subjectName = exam.subjectName,
                        daysUntil = exam.daysUntil,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            WeeklySnapshotCard(
                completedSessions = uiState.weeklyCompletedSessions,
                totalHours = uiState.weeklyTotalHours,
                onViewReport = onViewReport,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

// ===== بطاقة اقتراح الذكاء الاصطناعي =====
@Composable
fun AISuggestionCard(
    suggestion: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg, glassAlpha = 0.2f) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = JadwalViolet.copy(alpha = 0.15f * glowAlpha),
                        shape = RoundedCornerShape(12.dp),
                    )
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = JadwalViolet.copy(alpha = glowAlpha),
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // إصلاح #4: كان "جدول يقول..." — الآن stringResource
                    text = stringResource(R.string.ai_says),
                    style = MaterialTheme.typography.labelMedium,
                    color = JadwalViolet,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                if (isLoading) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (it == 0) 0.9f else 0.6f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        )
                        if (it == 0) Spacer(Modifier.height(4.dp))
                    }
                } else {
                    Text(
                        // إصلاح #4: كانت رسالة تشجيع ثابتة بالعربية — الآن stringResource
                        text = suggestion ?: stringResource(R.string.ai_default_tip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

// ===== بطاقة التقدم اليومي =====
@Composable
fun DailyProgressCard(
    completedMinutes: Int,
    totalMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalMinutes > 0) completedMinutes.toFloat() / totalMinutes else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "daily_progress",
    )
    val percentage = (progress * 100).toInt()

    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    // إصلاح #4: كان "تقدم اليوم" — الآن stringResource
                    text = stringResource(R.string.today_progress),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    // إصلاح #4: كان "$completedMinutes / $totalMinutes دقيقة"
                    text = stringResource(R.string.from_minutes_format, completedMinutes, totalMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .width(180.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = JadwalIndigo,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(color = JadwalIndigo.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = JadwalIndigo,
                    )
                    Text(
                        // إصلاح #4: كان "منجز" — الآن stringResource
                        text = stringResource(R.string.completed),
                        style = MaterialTheme.typography.labelSmall,
                        color = JadwalIndigo.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ===== رأس الشاشة =====
@Composable
fun HomeHeader(
    greeting: String,
    userName: String,
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                // إصلاح #4: كان "طالب" hardcoded — الآن stringResource
                text = if (userName.isNotBlank()) userName else stringResource(R.string.student),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (streakDays > 0) {
            GlassCard(cornerRadius = JadwalRadius.md, glassAlpha = 0.2f) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🔥", fontSize = 20.sp)
                    Text(
                        text = "$streakDays",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = JadwalWarning,
                    )
                    Text(
                        // إصلاح #4: كان "يوم" — الآن stringResource
                        text = stringResource(R.string.day_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ===== بطاقة مهمة =====
@Composable
fun TaskCard(
    task: TodayTask,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.md) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = task.subjectColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                    )
            ) {
                Text(task.subjectIcon, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val timePrefix = if (task.startHour >= 0) {
                    "${String.format("%02d:%02d", task.startHour, task.startMinute)} • "
                } else ""
                Text(
                    // إصلاح #4: كان "${task.allocatedMinutes} دقيقة"
                    text = "$timePrefix${task.allocatedMinutes} ${stringResource(R.string.minutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                task.isCompleted -> Icon(
                    Icons.Rounded.CheckCircle,
                    // إصلاح #4: كان "منجزة" hardcoded
                    contentDescription = stringResource(R.string.completed),
                    tint = JadwalSuccess,
                    modifier = Modifier.size(32.dp),
                )
                else -> FilledTonalButton(
                    onClick = onStartSession,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    // إصلاح #4: كان "ابدأ" hardcoded
                    Text(stringResource(R.string.start), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ===== حالة فارغة =====
@Composable
fun EmptyTasksCard(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🎉", fontSize = 48.sp)
            Text(
                // إصلاح #4: كان "لا توجد مهام لليوم" hardcoded
                text = stringResource(R.string.no_tasks_today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                // إصلاح #4: كان "أنجزت كل شيء!..." hardcoded
                text = stringResource(R.string.all_done_rest),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ===== تنبيه الامتحان القريب =====
@Composable
fun ExamAlertCard(
    subjectName: String,
    daysUntil: Int,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.md, glassAlpha = 0.3f) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("⚠️", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // إصلاح #4: كان "امتحان قريب!" hardcoded
                    text = stringResource(R.string.exam_alert_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = JadwalWarning,
                )
                Text(
                    // إصلاح #4: كان نص عربي مع plurals hardcoded
                    text = if (daysUntil <= 1)
                        stringResource(R.string.exam_day_away, subjectName)
                    else
                        stringResource(R.string.exam_days_away, subjectName, daysUntil),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ===== ملخص الأسبوع =====
@Composable
fun WeeklySnapshotCard(
    completedSessions: Int,
    totalHours: Float,
    onViewReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // إصلاح #4: كان "ملخص الأسبوع" hardcoded
                    text = stringResource(R.string.weekly_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onViewReport) {
                    // إصلاح #4: كان "التفاصيل" hardcoded
                    Text(stringResource(R.string.details), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // إصلاح #4: كانت labels بالعربية hardcoded
                WeeklyStatItem(
                    value = "$completedSessions",
                    label = stringResource(R.string.sessions_label),
                    icon = "✅",
                )
                WeeklyStatItem(
                    value = String.format("%.1f", totalHours),
                    label = stringResource(R.string.hour),
                    icon = "⏱️",
                )
            }
        }
    }
}

@Composable
fun WeeklyStatItem(value: String, label: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}