package com.jadwal.ui.screens.analytics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.R
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.screens.profile.ProfileContent
import com.jadwal.ui.screens.profile.ProfileViewModel
import com.jadwal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onViewProfile: () -> Unit = {},
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab   by viewModel.selectedTab.collectAsStateWithLifecycle()
    val profileState  by profileViewModel.uiState.collectAsStateWithLifecycle()

    var topTab by remember { mutableStateOf(1) }

    // ===== إصلاح: Photo Picker مع قراءة البايتات فورًا في الـ callback =====
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) { null }
            if (bytes != null && bytes.isNotEmpty()) {
                profileViewModel.onPhotoSelected(bytes)
            }
        }
    }

    // ===== إصلاح: dialog تعديل الاسم داخل AnalyticsScreen =====
    if (profileState.showEditNameDialog) {
        var localName by remember(profileState.editNameText) {
            mutableStateOf(profileState.editNameText)
        }
        AlertDialog(
            onDismissRequest = profileViewModel::dismissEditNameDialog,
            title = { Text(stringResource(R.string.edit_name_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = localName,
                    onValueChange = { localName = it },
                    label = { Text(stringResource(R.string.name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    profileViewModel.onEditNameChange(localName)
                    profileViewModel.saveNewName()
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = profileViewModel::dismissEditNameDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ===== التبويب العلوي: الملف الشخصي / الإحصائيات =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = JadwalRadius.xl,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                    ) {
                        listOf(
                            stringResource(R.string.profile),
                            stringResource(R.string.analytics),
                        ).forEachIndexed { index, title ->
                            val isSelected = topTab == index
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(JadwalRadius.lg))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { topTab = index }
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = topTab == 1) {
                    FilledTonalIconButton(onClick = viewModel::refresh) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // ===== المحتوى =====
            AnimatedContent(
                targetState = topTab,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 2 } + fadeOut()
                    else
                        slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 2 } + fadeOut()
                },
                label = "top_tab_content",
            ) { tab ->
                if (tab == 0) {
                    // ===== الملف الشخصي =====
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(bottom = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // إصلاح: callbacks حقيقية بدلاً من {} الفارغة
                        ProfileContent(
                            uiState = profileState,
                            onPickPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onShowEditName = profileViewModel::showEditNameDialog,
                        )
                    }
                } else {
                    // ===== الإحصائيات =====
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(bottom = 100.dp),
                    ) {
                        if (uiState.isLoading) {
                            AnalyticsLoadingSkeleton()
                            return@Column
                        }

                        QuickSummaryRow(
                            weekHours = uiState.weekTotalHours,
                            sessions = uiState.weekCompletedSessions,
                            avgMinutes = uiState.weekAvgMinutesPerDay,
                            streak = uiState.streakDays,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )

                        Spacer(Modifier.height(16.dp))

                        MeVsPastMeCard(
                            currentHours = uiState.weekTotalHours,
                            lastWeekHours = uiState.lastWeekTotalHours,
                            diff = uiState.weekVsPastDiff,
                            percent = uiState.weekVsPastPercent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )

                        Spacer(Modifier.height(16.dp))

                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = {},
                            divider = {},
                        ) {
                            listOf(
                                stringResource(R.string.this_week),
                                stringResource(R.string.this_month),
                            ).forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(index) },
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        ),
                                ) {
                                    Text(
                                        text = title,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold
                                        else FontWeight.Normal,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (targetState > initialState)
                                    slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { -it / 2 } + fadeOut()
                                else
                                    slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                            slideOutHorizontally { it / 2 } + fadeOut()
                            },
                            label = "chart_tab",
                        ) { chartTab ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                cornerRadius = JadwalRadius.xl,
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    if (chartTab == 0) {
                                        WeeklyBarChartTitle(
                                            bestDayRes = uiState.bestDayLabelRes,
                                            bestMinutes = uiState.bestDayMinutes,
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        WeeklyBarChart(
                                            bars = uiState.weekBars,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                        )
                                    } else {
                                        MonthlyLineChartTitle(totalHours = uiState.monthTotalHours)
                                        Spacer(Modifier.height(16.dp))
                                        MonthlyLineChart(
                                            points = uiState.monthPoints,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (uiState.subjectStats.isNotEmpty()) {
                            SubjectStatsSection(
                                stats = uiState.subjectStats,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                        }

                        MotivationCard(
                            weekHours = uiState.weekTotalHours,
                            sessions = uiState.weekCompletedSessions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ===== بطاقة أنا vs أنا الأسبوع الماضي =====
@Composable
fun MeVsPastMeCard(
    currentHours: Float,
    lastWeekHours: Float,
    diff: Float,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val isImproved = diff >= 0
    val accentColor = if (isImproved) JadwalSuccess else JadwalError
    val arrow = if (isImproved) "↑" else "↓"
    val emoji = when {
        percent >= 30  -> "🚀"
        percent >= 10  -> "📈"
        percent >= 0   -> "✅"
        percent >= -10 -> "📉"
        else           -> "⚠️"
    }

    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.xl) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.me_vs_past_me),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(emoji, fontSize = 20.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.this_week_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "%.1f".format(currentHours),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.hour_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            "$arrow ${if (isImproved) "+" else ""}${percent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Text(
                        if (isImproved) stringResource(R.string.improvement)
                        else stringResource(R.string.decline),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.last_week_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "%.1f".format(lastWeekHours),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.hour_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ===== ملخص سريع =====
@Composable
fun QuickSummaryRow(
    weekHours: Float,
    sessions: Int,
    avgMinutes: Int,
    streak: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            emoji = "⏱️",
            value = "%.1f".format(weekHours),
            label = stringResource(R.string.hour_short),
            color = JadwalIndigo,
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            emoji = "📖",
            value = "$sessions",
            label = stringResource(R.string.sessions_label),
            color = JadwalViolet,
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            emoji = "📊",
            value = "$avgMinutes",
            label = stringResource(R.string.minute_short),
            color = JadwalSuccess,
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            emoji = "🔥",
            value = "$streak",
            label = stringResource(R.string.day_label),
            color = JadwalWarning,
        )
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    label: String,
    color: Color,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                fontSize = if (value.length > 4) 16.sp else 20.sp,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                fontSize = 10.sp,
            )
        }
    }
}

// ===== عنوان الرسم الأسبوعي =====
@Composable
fun WeeklyBarChartTitle(bestDayRes: Int, bestMinutes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.daily_study_minutes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (bestMinutes > 0) {
            Surface(
                color = JadwalIndigo.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "🏆 ${stringResource(bestDayRes)} • ${bestMinutes}${stringResource(R.string.minute_abbrev)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = JadwalIndigo,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ===== الرسم البياني الأسبوعي =====
@Composable
fun WeeklyBarChart(bars: List<DayBar>, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return
    val maxMin = bars.maxOf { it.minutes }.coerceAtLeast(1)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val barWidth   = size.width / (bars.size * 2f)
            val spacing    = barWidth
            val chartHeight = size.height

            bars.forEachIndexed { i, bar ->
                val barHeight = (bar.minutes.toFloat() / maxMin) * chartHeight
                val x = i * (barWidth + spacing) + spacing / 2
                val barColor = try {
                    Color(android.graphics.Color.parseColor(bar.colorHex))
                } catch (_: Exception) { JadwalIndigo }

                drawRoundRect(
                    brush = if (bar.minutes > 0)
                        Brush.verticalGradient(
                            colors = listOf(barColor.copy(alpha = 0.9f), barColor),
                            startY = chartHeight - barHeight,
                            endY = chartHeight,
                        )
                    else Brush.verticalGradient(colors = listOf(surfaceVariant, surfaceVariant)),
                    topLeft = Offset(x, chartHeight - barHeight.coerceAtLeast(4.dp.toPx())),
                    size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            bars.forEach { bar ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp),
                ) {
                    if (bar.minutes > 0) {
                        Text(
                            "${bar.minutes}${stringResource(R.string.minute_abbrev)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bar.isToday) JadwalViolet
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                        )
                    }
                    Text(
                        stringResource(bar.labelRes).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bar.isToday) JadwalViolet
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ===== الرسم البياني الشهري =====
@Composable
fun MonthlyLineChartTitle(totalHours: Float) {
    Text(
        stringResource(R.string.monthly_study_hours_total, totalHours),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun MonthlyLineChart(points: List<MonthlyPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val maxHours  = points.maxOf { it.hours }.coerceAtLeast(1f)
    val lineColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val w    = size.width
            val h    = size.height - 8.dp.toPx()
            val step = w / (points.size - 1).coerceAtLeast(1)

            val path = Path()
            points.forEachIndexed { i, p ->
                val x = i * step
                val y = h - (p.hours / maxHours) * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path, lineColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            points.forEachIndexed { i, p ->
                val x = i * step
                val y = h - (p.hours / maxHours) * h
                drawCircle(lineColor, 6.dp.toPx(), Offset(x, y))
                drawCircle(Color.White, 3.dp.toPx(), Offset(x, y))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            points.forEach { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%.1f".format(p.hours),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                    )
                    Text(
                        p.weekLabel.replace(
                            stringResource(R.string.week_prefix),
                            stringResource(R.string.week_abbrev),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ===== إحصاء المواد =====
@Composable
fun SubjectStatsSection(stats: List<SubjectStat>, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.xl) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                stringResource(R.string.subject_performance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            stats.forEach { stat ->
                SubjectStatRow(stat = stat)
                if (stat != stats.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectStatRow(stat: SubjectStat) {
    val animatedCompletion by animateFloatAsState(
        targetValue = stat.completionRate,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "completion_${stat.subjectId}",
    )
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(stat.colorHex))
    } catch (_: Exception) { JadwalIndigo }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(subjectColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
        ) {
            Text(stat.subjectIcon, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stat.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${(stat.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = subjectColor,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedCompletion },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = subjectColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.sessions_hours_summary,
                    stat.completedSessions,
                    stat.totalMinutes / 60,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ===== بطاقة التحفيز =====
@Composable
fun MotivationCard(weekHours: Float, sessions: Int, modifier: Modifier = Modifier) {
    val (emoji, message) = when {
        weekHours >= 15 -> "🌟" to stringResource(R.string.improve_great)
        weekHours >= 10 -> "💪" to stringResource(R.string.improve_good)
        weekHours >= 5  -> "📈" to stringResource(R.string.improve_stable)
        sessions > 0    -> "🌱" to stringResource(R.string.improve_low)
        else            -> "🎯" to stringResource(R.string.improve_none)
    }
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg, glassAlpha = 0.2f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(emoji, fontSize = 36.sp)
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
            )
        }
    }
}

// ===== Skeleton =====
@Composable
fun AnalyticsLoadingSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = shimmer * 0.15f)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(skeletonColor),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(skeletonColor),
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(skeletonColor),
            )
        }
    }
}
