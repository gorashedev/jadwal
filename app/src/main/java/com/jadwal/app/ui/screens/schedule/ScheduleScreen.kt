package com.jadwal.ui.screens.schedule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*
import java.util.Calendar
import androidx.compose.ui.res.stringResource
import com.jadwal.R

@Composable
fun ScheduleScreen(
    onScanExams: () -> Unit = {},
    onManageSubjects: () -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    
    val arabicDays = listOf(
        stringResource(R.string.day_sun), stringResource(R.string.day_mon),
        stringResource(R.string.day_tue), stringResource(R.string.day_wed),
        stringResource(R.string.day_thu), stringResource(R.string.day_fri),
        stringResource(R.string.day_sat)
    )
    val arabicDaysShort = androidx.compose.ui.res.stringArrayResource(R.array.days_short)

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.RestartAlt, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.reset_schedule_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.reset_schedule_confirm)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetSchedule(); showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.reset_schedule_action), color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ===== العنوان =====
            Text(
                text = stringResource(R.string.weekly_schedule),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            )

            // ===== شريط أزرار الإجراءات (كتبويبات أيقونية) =====
            ScheduleActionBar(
                onManageSubjects = onManageSubjects,
                onScanExams = onScanExams,
                onReset = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // ===== بانر وضع الطوارئ (امتحان خلال 48 ساعة) =====
            if (uiState.examNightExams.isNotEmpty()) {
                ExamNightBanner(
                    exams = uiState.examNightExams,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            // ===== امتحانات قريبة (شارات) =====
            if (uiState.upcomingExams.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    items(uiState.upcomingExams) { exam ->
                        ExamBadgeChip(exam)
                    }
                }
            }

            // ===== شريط الأيام =====
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(7) { index ->
                    DayTab(
                        dayName = arabicDaysShort[index],
                        isSelected = uiState.selectedDayIndex == index,
                        isToday = index == todayIndex,
                        hasItems = uiState.weekItems[index]?.isNotEmpty() == true,
                        onClick = { viewModel.selectDay(index) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== زر توليد الجدول =====
            if (!uiState.isLoading && uiState.weekItems.isEmpty()) {
                GenerateScheduleCard(
                    isGenerating = uiState.isGenerating,
                    onGenerate = viewModel::generateSchedule,
                )
            }

            // ===== محتوى اليوم المختار (بدون AnimatedContent لتجنب تجميد التمرير) =====
            val dayIndex = uiState.selectedDayIndex
            val items = uiState.weekItems[dayIndex] ?: emptyList()
            when {
                uiState.isLoading || uiState.isGenerating -> ScheduleLoadingSkeleton()
                items.isEmpty() -> EmptyDayContent(dayName = arabicDays[dayIndex])
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        ScheduleItemCard(item = item)
                    }
                }
            }
        }
    }
}

// ===== شريط أزرار الإجراءات الأيقونية =====
@Composable
fun ScheduleActionBar(
    onManageSubjects: () -> Unit,
    onScanExams: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    data class ActionItem(val icon: ImageVector, val label: String, val action: () -> Unit, val color: Color)

    val actions = listOf(
        ActionItem(Icons.Rounded.CalendarMonth,  stringResource(R.string.schedule),   {},              JadwalIndigo),
        ActionItem(Icons.Rounded.MenuBook,       stringResource(R.string.manage_subjects),   onManageSubjects, JadwalViolet),
        ActionItem(Icons.Rounded.RestartAlt,     stringResource(R.string.reset_action),    onReset,         JadwalWarning),
        ActionItem(Icons.Rounded.PhotoCamera,    stringResource(R.string.scan_exam_schedule),      onScanExams,     JadwalSuccess),
    )

    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = item.action)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = item.color.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ),
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = item.color,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = item.color,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ===== بانر وضع طوارئ الامتحان =====
@Composable
fun ExamNightBanner(
    exams: List<ExamBadge>,
    modifier: Modifier = Modifier,
) {
    val firstExam = exams.first()
    val urgencyColor = if (firstExam.daysUntil == 0) MaterialTheme.colorScheme.error
                       else JadwalWarning

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = urgencyColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, urgencyColor.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (firstExam.daysUntil == 0) "🚨" else "⚠️",
                fontSize = 28.sp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.emergency_mode_title, firstExam.subjectName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = urgencyColor,
                )
                Text(
                    text = when (firstExam.daysUntil) {
                        0 -> stringResource(R.string.exam_today_msg)
                        1 -> stringResource(R.string.exam_tomorrow_msg)
                        else -> stringResource(R.string.exam_in_days_msg, firstExam.daysUntil)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = urgencyColor.copy(alpha = 0.8f),
                )
            }
            Text(firstExam.subjectIcon, fontSize = 26.sp)
        }
    }
}

// ===== تاب اليوم =====
@Composable
fun DayTab(
    dayName: String,
    isSelected: Boolean,
    isToday: Boolean,
    hasItems: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "day_tab_bg",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday    -> MaterialTheme.colorScheme.primary
            else       -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "day_tab_text",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    color = if (hasItems) {
                        if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else Color.Transparent,
                    shape = CircleShape,
                )
        )
    }
}

// ===== بطاقة عنصر الجدول =====
@Composable
fun ScheduleItemCard(item: ScheduleItem) {
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(item.colorHex))
    } catch (e: Exception) { JadwalIndigo }

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = JadwalRadius.md) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp).height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(subjectColor)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(subjectColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            ) {
                Text(item.subjectIcon, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${String.format("%02d:%02d", item.startHour, item.startMinute)}" +
                           " • ${item.durationMinutes} " + stringResource(R.string.minute_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.isCompleted) {
                Surface(color = JadwalSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null,
                            tint = JadwalSuccess, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.status_done), style = MaterialTheme.typography.labelSmall,
                            color = JadwalSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Surface(color = subjectColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(stringResource(R.string.status_upcoming),
                        style = MaterialTheme.typography.labelSmall,
                        color = subjectColor, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

// ===== شارة الامتحان القريب =====
@Composable
fun ExamBadgeChip(exam: ExamBadge) {
    Surface(
        color = when {
            exam.daysUntil <= 1 -> MaterialTheme.colorScheme.errorContainer
            exam.daysUntil <= 3 -> JadwalWarning.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(exam.subjectIcon, fontSize = 16.sp)
            Column {
                Text(exam.subjectName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = when (exam.daysUntil) {
                        0 -> stringResource(R.string.today_label)
                        1 -> stringResource(R.string.tomorrow_label)
                        else -> stringResource(R.string.days_left_format, exam.daysUntil)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        exam.daysUntil <= 1 -> MaterialTheme.colorScheme.error
                        exam.daysUntil <= 3 -> JadwalWarning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

// ===== بطاقة توليد الجدول =====
@Composable
fun GenerateScheduleCard(isGenerating: Boolean, onGenerate: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = JadwalRadius.xl) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🗓️", fontSize = 36.sp)
                Text(stringResource(R.string.schedule_empty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.schedule_empty_tip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onGenerate, enabled = !isGenerating, modifier = Modifier.fillMaxWidth()) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.generating_schedule))
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.generate_schedule_auto), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===== حالة اليوم الفارغ =====
@Composable
fun EmptyDayContent(dayName: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = JadwalRadius.xl) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("😌", fontSize = 48.sp)
                Text(stringResource(R.string.no_study_day, dayName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.rest_or_review),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

// ===== Skeleton =====
@Composable
fun ScheduleLoadingSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ), label = "alpha",
    )
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = shimmer * 0.15f)
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(4) {
            Box(modifier = Modifier.fillMaxWidth().height(84.dp)
                .clip(RoundedCornerShape(16.dp)).background(color))
        }
    }
}
