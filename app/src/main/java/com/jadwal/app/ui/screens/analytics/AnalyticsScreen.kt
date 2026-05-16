package com.jadwal.ui.screens.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jadwal.ui.components.GlassCard
import com.jadwal.ui.components.JadwalBackground
import com.jadwal.ui.theme.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    JadwalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 100.dp),
        ) {
            // ===== العنوان =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "الإحصائيات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "تحديث",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (uiState.isLoading) {
                AnalyticsLoadingSkeleton()
                return@Column
            }

            // ===== بطاقات الملخص السريع =====
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

            // ===== تبويبات الأسبوع / الشهر =====
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
                listOf("هذا الأسبوع", "هذا الشهر").forEachIndexed { index, title ->
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
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ===== الرسم البياني =====
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 2 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 2 } + fadeOut()
                    }
                },
                label = "chart_tab",
            ) { tab ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = JadwalRadius.xl,
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (tab == 0) {
                            // ===== رسم بياني أسبوعي شريطي =====
                            WeeklyBarChartTitle(
                                bestDay = uiState.bestDayLabel,
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
                            // ===== رسم بياني شهري خطي =====
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

            // ===== إحصاء المواد =====
            if (uiState.subjectStats.isNotEmpty()) {
                SubjectStatsSection(
                    stats = uiState.subjectStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            // ===== بطاقة التحفيز =====
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

// ===== ملخص سريع (4 بطاقات صغيرة) =====
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
            value = String.format("%.1f", weekHours),
            unit = "ساعة",
            label = "هذا الأسبوع",
            icon = "⏱️",
            modifier = Modifier.weight(1f),
        )
        QuickStatCard(
            value = "$sessions",
            unit = "جلسة",
            label = "منجزة",
            icon = "✅",
            modifier = Modifier.weight(1f),
        )
        QuickStatCard(
            value = "$avgMinutes",
            unit = "د",
            label = "متوسط يومي",
            icon = "📊",
            modifier = Modifier.weight(1f),
        )
        QuickStatCard(
            value = "$streak",
            unit = "يوم",
            label = "السلسلة",
            icon = "🔥",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun QuickStatCard(
    value: String,
    unit: String,
    label: String,
    icon: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.lg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(icon, fontSize = 18.sp)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
            )
        }
    }
}

// ===== الرسم البياني الأسبوعي الشريطي =====
@Composable
fun WeeklyBarChartTitle(bestDay: String, bestMinutes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "دقائق المذاكرة اليومية",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (bestDay.isNotEmpty()) {
            Surface(
                color = JadwalIndigo.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "🏆 $bestDay",
                    style = MaterialTheme.typography.labelSmall,
                    color = JadwalIndigo,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    bars: List<DayBar>,
    modifier: Modifier = Modifier,
) {
    if (bars.isEmpty()) return

    val maxMinutes = bars.maxOf { it.minutes }.coerceAtLeast(1)
    val primaryColor = MaterialTheme.colorScheme.primary
    val todayColor = JadwalViolet
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // أنيميشن ارتفاع الأعمدة
    val animatedHeights = bars.mapIndexed { i, bar ->
        animateFloatAsState(
            targetValue = bar.minutes.toFloat() / maxMinutes,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            label = "bar_height_$i",
        )
    }

    Column(modifier = modifier) {
        // الرسم البياني
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val barCount = bars.size
            val totalSpacing = size.width * 0.4f
            val barWidth = (size.width - totalSpacing) / barCount
            val spacing = totalSpacing / (barCount + 1)
            val chartHeight = size.height - 4.dp.toPx()

            bars.forEachIndexed { i, bar ->
                val x = spacing + i * (barWidth + spacing)
                val heightFraction = animatedHeights[i].value
                val barHeight = chartHeight * heightFraction

                val barColor = when {
                    bar.isToday -> todayColor
                    bar.minutes > 0 -> primaryColor
                    else -> surfaceVariant
                }

                // ظل خفيف
                if (bar.minutes > 0) {
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.15f),
                        topLeft = Offset(x + 2.dp.toPx(), chartHeight - barHeight + 4.dp.toPx()),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )
                }

                // العمود الأساسي
                drawRoundRect(
                    brush = if (bar.minutes > 0) Brush.verticalGradient(
                        colors = listOf(
                            barColor.copy(alpha = 0.9f),
                            barColor,
                        ),
                        startY = chartHeight - barHeight,
                        endY = chartHeight,
                    ) else Brush.verticalGradient(
                        colors = listOf(surfaceVariant, surfaceVariant)
                    ),
                    topLeft = Offset(x, chartHeight - barHeight.coerceAtLeast(4.dp.toPx())),
                    size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // تسميات الأيام
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            bars.forEach { bar ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp),
                ) {
                    // دقائق فوق الاسم
                    if (bar.minutes > 0) {
                        Text(
                            text = "${bar.minutes}د",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bar.isToday) JadwalViolet
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        text = bar.label.take(3), // أخذ أول 3 حروف: "الأح"، "الاث"...
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bar.isToday) JadwalViolet
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                    // نقطة اليوم
                    if (bar.isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(JadwalViolet, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// ===== الرسم البياني الشهري الخطي =====
@Composable
fun MonthlyLineChartTitle(totalHours: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "ساعات المذاكرة الأسبوعية",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            color = JadwalSuccess.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "${String.format("%.1f", totalHours)}س إجمالي",
                style = MaterialTheme.typography.labelSmall,
                color = JadwalSuccess,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun MonthlyLineChart(
    points: List<MonthlyPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val maxHours = points.maxOf { it.hours }.coerceAtLeast(1f)
    val lineColor = JadwalIndigo
    val fillColor = JadwalIndigo

    val animatedFraction by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = EaseInOutCubic),
        label = "line_draw",
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height - 8.dp.toPx()
            val count = points.size
            if (count < 2) return@Canvas

            // حساب نقاط الرسم
            val xStep = chartWidth / (count - 1)
            val rawPoints = points.mapIndexed { i, p ->
                Offset(
                    x = i * xStep,
                    y = chartHeight - (p.hours / maxHours) * chartHeight,
                )
            }

            // عدد النقاط المرئية بناءً على التحريك
            val visibleIndex = (animatedFraction * (count - 1)).toInt().coerceIn(0, count - 2)
            val partial = (animatedFraction * (count - 1)) - visibleIndex
            val visiblePoints = rawPoints.take(visibleIndex + 1) +
                    if (visibleIndex < count - 1) listOf(
                        Offset(
                            x = rawPoints[visibleIndex].x + (rawPoints[visibleIndex + 1].x - rawPoints[visibleIndex].x) * partial,
                            y = rawPoints[visibleIndex].y + (rawPoints[visibleIndex + 1].y - rawPoints[visibleIndex].y) * partial,
                        )
                    ) else emptyList()

            if (visiblePoints.size < 2) return@Canvas

            // منطقة التعبئة تحت الخط
            val fillPath = Path().apply {
                moveTo(visiblePoints.first().x, chartHeight)
                visiblePoints.forEach { lineTo(it.x, it.y) }
                lineTo(visiblePoints.last().x, chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        fillColor.copy(alpha = 0.25f),
                        fillColor.copy(alpha = 0.0f),
                    ),
                    startY = 0f,
                    endY = chartHeight,
                ),
            )

            // الخط نفسه
            val linePath = Path().apply {
                moveTo(visiblePoints.first().x, visiblePoints.first().y)
                visiblePoints.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // نقاط على الخط
            visiblePoints.forEach { point ->
                drawCircle(Color.White, radius = 5.dp.toPx(), center = point)
                drawCircle(lineColor, radius = 4.dp.toPx(), center = point,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            }
        }

        Spacer(Modifier.height(8.dp))

        // تسميات الأسابيع
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            points.forEach { point ->
                Text(
                    text = point.weekLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ===== إحصاء المواد =====
@Composable
fun SubjectStatsSection(
    stats: List<SubjectStat>,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, cornerRadius = JadwalRadius.xl) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = "أداؤك في كل مادة",
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
    } catch (e: Exception) {
        JadwalIndigo
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // أيقونة المادة
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = subjectColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                )
        ) {
            Text(stat.subjectIcon, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stat.subjectName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${(stat.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = subjectColor,
                )
            }

            Spacer(Modifier.height(4.dp))

            // شريط التقدم
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
                text = "${stat.completedSessions} جلسات • ${stat.totalMinutes / 60} ساعة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ===== بطاقة التحفيز =====
@Composable
fun MotivationCard(
    weekHours: Float,
    sessions: Int,
    modifier: Modifier = Modifier,
) {
    val (emoji, message) = when {
        weekHours >= 15 -> "🌟" to "أداء رائع هذا الأسبوع! أنت في القمة"
        weekHours >= 10 -> "💪" to "عمل ممتاز! حافظ على هذا الإيقاع"
        weekHours >= 5 -> "📈" to "بداية جيدة! حاول زيادة وقت المذاكرة قليلاً"
        sessions > 0 -> "🌱" to "بداية كل رحلة خطوة — استمر!"
        else -> "🎯" to "اليوم هو أفضل يوم للبدء — انطلق الآن!"
    }

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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(emoji, fontSize = 36.sp)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
            )
        }
    }
}

// ===== Skeleton تحميل =====
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
        // ملخص سريع
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(skeletonColor)
                )
            }
        }
        // مخطط
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(skeletonColor)
        )
        // مواد
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(skeletonColor)
            )
        }
    }
}

// امتداد مساعد لتأخير Spring Animation
private fun <T> AnimationSpec<T>.delay(delayMillis: Int): AnimationSpec<T> =
    @Suppress("UNCHECKED_CAST")
    (this as? SpringSpec<T>)?.let {
        spring(it.dampingRatio, it.stiffness)
    } ?: this
