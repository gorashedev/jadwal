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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*
import java.util.Calendar

val arabicDays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
val arabicDaysShort = listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

@Composable
fun ScheduleScreen(
    onScanExams: () -> Unit = {},
    onManageSubjects: () -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ===== العنوان =====
            var showResetDialog by remember { mutableStateOf(false) }

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    icon = { Icon(Icons.Rounded.RestartAlt, null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("إعادة تعيين الجدول", fontWeight = FontWeight.Bold) },
                    text = { Text("سيتم حذف الجدول الحالي بالكامل. هل أنت متأكد؟") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.resetSchedule(); showResetDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("إعادة تعيين", color = MaterialTheme.colorScheme.onError) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) { Text("إلغاء") }
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "الجدول الأسبوعي",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row {
                    // زر إدارة المواد
                    IconButton(onClick = onManageSubjects) {
                        Icon(Icons.Rounded.MenuBook,
                            contentDescription = "إدارة المواد",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    // زر مسح جدول الامتحانات بالكاميرا
                    IconButton(onClick = onScanExams) {
                        Icon(Icons.Rounded.PhotoCamera,
                            contentDescription = "مسح جدول الامتحانات",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    // زر إعادة التعيين
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "إعادة تعيين",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "تحديث",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ===== امتحانات قريبة =====
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

            // ===== زر توليد الجدول (يظهر فقط عندما لا توجد أي مهام) =====
            if (!uiState.isLoading && uiState.weekItems.isEmpty()) {
                GenerateScheduleCard(
                    isGenerating = uiState.isGenerating,
                    onGenerate = viewModel::generateSchedule,
                )
            }

            // ===== محتوى اليوم المختار =====
            AnimatedContent(
                targetState = uiState.selectedDayIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 3 } + fadeOut()
                    }
                },
                label = "day_content",
            ) { dayIndex ->
                val items = uiState.weekItems[dayIndex] ?: emptyList()
                if (uiState.isLoading || uiState.isGenerating) {
                    ScheduleLoadingSkeleton()
                } else if (items.isEmpty()) {
                    EmptyDayContent(dayName = arabicDays[dayIndex])
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 100.dp,
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
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        label = "day_tab_bg",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
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
        // نقطة إذا كان هناك مهام
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

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = JadwalRadius.md,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // شريط اللون الجانبي
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(subjectColor)
            )

            // أيقونة المادة
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = subjectColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                    )
            ) {
                Text(item.subjectIcon, fontSize = 24.sp)
            }

            // التفاصيل
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${String.format("%02d:%02d", item.startHour, item.startMinute)}" +
                            " • ${item.durationMinutes} دقيقة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // حالة الإنجاز
            if (item.isCompleted) {
                Surface(
                    color = JadwalSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = JadwalSuccess,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "منجز",
                            style = MaterialTheme.typography.labelSmall,
                            color = JadwalSuccess,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                Surface(
                    color = subjectColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "قادم",
                        style = MaterialTheme.typography.labelSmall,
                        color = subjectColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
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
                Text(
                    text = exam.subjectName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when (exam.daysUntil) {
                        0 -> "⚠️ اليوم!"
                        1 -> "⚠️ غداً"
                        else -> "بعد ${exam.daysUntil} أيام"
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
fun GenerateScheduleCard(
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = JadwalRadius.xl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🗓️", fontSize = 36.sp)
                Text(
                    text = "الجدول فارغ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "اضغط على الزر أدناه لتوليد جدول أسبوعي تلقائي بناءً على مواردك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("جارٍ التوليد...")
                    } else {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("توليد الجدول تلقائياً", fontWeight = FontWeight.Bold)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = JadwalRadius.xl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("😌", fontSize = 48.sp)
                Text(
                    text = "لا مذاكرة يوم $dayName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "استرح أو راجع ما درسته",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ===== Skeleton =====
@Composable
fun ScheduleLoadingSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = shimmer * 0.15f)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
            )
        }
    }
}
