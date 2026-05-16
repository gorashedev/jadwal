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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.domain.model.UnderstandingLevel
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStartSession: (String) -> Unit,
    onViewSchedule: () -> Unit,
    onViewReport: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiSuggestion by viewModel.aiSuggestion.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(bottom = 100.dp) // مسافة للـ BottomBar
        ) {
            // ===== الرأس =====
            HomeHeader(
                greeting = uiState.greeting,
                userName = uiState.userName,
                streakDays = uiState.streakDays,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
            )

            // ===== بطاقة الذكاء الاصطناعي — مرئية دائماً =====
            AISuggestionCard(
                suggestion = aiSuggestion,
                isLoading = uiState.isAiLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ===== بطاقة التقدم اليومي =====
            DailyProgressCard(
                completedMinutes = uiState.completedMinutes,
                totalMinutes = uiState.totalPlannedMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // ===== عنوان المهام =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "مهام اليوم",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onViewSchedule) {
                    Text("الجدول الكامل")
                }
            }

            // ===== قائمة المهام =====
            if (uiState.todayTasks.isEmpty()) {
                EmptyTasksCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            } else {
                uiState.todayTasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onStartSession = { onStartSession(task.scheduleItemId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // ===== تنبيه الامتحان القادم =====
            uiState.upcomingExam?.let { exam ->
                if (exam.daysUntil <= 7) {
                    ExamAlertCard(
                        subjectName = exam.subjectName,
                        daysUntil = exam.daysUntil,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            // ===== ملخص الأسبوع =====
            WeeklySnapshotCard(
                completedSessions = uiState.weeklyCompletedSessions,
                totalHours = uiState.weeklyTotalHours,
                onViewReport = onViewReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
    // أنيميشن نبض للأيقونة
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

    GlassCard(
        modifier = modifier,
        cornerRadius = JadwalRadius.lg,
        glassAlpha = 0.2f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // أيقونة الذكاء الاصطناعي
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
                    text = "جدول يقول...",
                    style = MaterialTheme.typography.labelMedium,
                    color = JadwalViolet,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                if (isLoading) {
                    // مؤشر تحميل
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (it == 0) 0.9f else 0.6f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                        )
                        if (it == 0) Spacer(Modifier.height(4.dp))
                    }
                } else {
                    Text(
                        text = suggestion ?: "شد حيلك، أنت قادر! كل جلسة تقربك أكثر من هدفك.",
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "تقدم اليوم",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$completedMinutes / $totalMinutes دقيقة",
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

            // النسبة المئوية
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = JadwalIndigo.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = JadwalIndigo,
                    )
                    Text(
                        text = "منجز",
                        style = MaterialTheme.typography.labelSmall,
                        color = JadwalIndigo.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ===== رأس الشاشة الرئيسية =====
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
                text = if (userName.isNotBlank()) userName else "طالب",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Streak
        if (streakDays > 0) {
            GlassCard(
                cornerRadius = JadwalRadius.md,
                glassAlpha = 0.2f,
            ) {
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
                        text = "يوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ===== بطاقة مهمة واحدة =====
@Composable
fun TaskCard(
    task: TodayTask,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.md) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // أيقونة المادة
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
                Text(
                    text = "${task.allocatedMinutes} دقيقة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // حالة المهمة
            when {
                task.isCompleted -> Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "منجزة",
                    tint = JadwalSuccess,
                    modifier = Modifier.size(32.dp),
                )
                else -> FilledTonalButton(
                    onClick = onStartSession,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("ابدأ", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ===== حالة فارغة لمهام اليوم =====
@Composable
fun EmptyTasksCard(modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🎉", fontSize = 48.sp)
            Text(
                text = "لا توجد مهام لليوم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "أنجزت كل شيء! خذ قسطاً من الراحة 😌",
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
    GlassCard(
        modifier = modifier,
        cornerRadius = JadwalRadius.md,
        glassAlpha = 0.3f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("⚠️", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "امتحان قريب!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = JadwalWarning,
                )
                Text(
                    text = "$subjectName — بعد $daysUntil ${if (daysUntil == 1) "يوم" else "أيام"}",
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
                    text = "ملخص الأسبوع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onViewReport) {
                    Text("التفاصيل", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WeeklyStatItem(
                    value = "$completedSessions",
                    label = "جلسة",
                    icon = "✅",
                )
                WeeklyStatItem(
                    value = String.format("%.1f", totalHours),
                    label = "ساعة",
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
