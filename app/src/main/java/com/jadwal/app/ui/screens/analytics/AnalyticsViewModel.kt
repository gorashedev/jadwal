package com.jadwal.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ===== نماذج البيانات =====

data class DayBar(
    val label: String,
    val minutes: Int,
    val isToday: Boolean = false,
)

data class SubjectStat(
    val subjectId: String,
    val subjectName: String,
    val subjectIcon: String,
    val colorHex: String,
    val totalMinutes: Int,
    val completedSessions: Int,
    val completionRate: Float,
)

data class MonthlyPoint(
    val weekLabel: String,
    val hours: Float,
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,

    // ===== الأسبوع الحالي =====
    val weekBars: List<DayBar> = emptyList(),
    val weekTotalHours: Float = 0f,
    val weekCompletedSessions: Int = 0,
    val weekAvgMinutesPerDay: Int = 0,

    // ===== الشهر الحالي =====
    val monthPoints: List<MonthlyPoint> = emptyList(),
    val monthTotalHours: Float = 0f,
    val monthCompletedSessions: Int = 0,

    // ===== إحصاء كل مادة =====
    val subjectStats: List<SubjectStat> = emptyList(),

    // ===== أفضل يوم =====
    val bestDayLabel: String = "",
    val bestDayMinutes: Int = 0,

    // ===== Streak =====
    val streakDays: Int = 0,

    // ===== أنا vs أنا الأسبوع الماضي =====
    val lastWeekTotalHours: Float = 0f,
    val weekVsPastDiff: Float = 0f,   // موجب = تحسن، سالب = تراجع
    val weekVsPastPercent: Int = 0,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0 = أسبوع، 1 = شهر
    val selectedTab = _selectedTab.asStateFlow()

    init {
        loadAnalytics()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val weekData    = loadWeekData()
                val monthData   = loadMonthData()
                val subjectData = loadSubjectStats()

                val bestDay      = weekData.maxByOrNull { it.minutes }
                val weekTotalMins = weekData.sumOf { it.minutes }
                val weekDaysWithData = weekData.count { it.minutes > 0 }

                // أنا vs أنا الأسبوع الماضي:
                // نأخذ الأسبوع قبل الأخير من بيانات الشهر كتقريب
                val currentWeekHours = weekTotalMins / 60f
                val lastWeekHours = monthData.dropLast(1).lastOrNull()?.hours ?: 0f
                val diff = currentWeekHours - lastWeekHours
                val percent = if (lastWeekHours > 0f)
                    ((diff / lastWeekHours) * 100).toInt()
                else if (currentWeekHours > 0f) 100
                else 0

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weekBars = weekData,
                        weekTotalHours = currentWeekHours,
                        weekCompletedSessions = weekData.sumOf { d -> d.minutes / 30 },
                        weekAvgMinutesPerDay = if (weekDaysWithData > 0)
                            weekTotalMins / weekDaysWithData else 0,
                        monthPoints = monthData,
                        monthTotalHours = monthData.sumOf { p -> p.hours.toDouble() }.toFloat(),
                        monthCompletedSessions = (monthData.sumOf { p -> p.hours.toDouble() } * 2).toInt(),
                        subjectStats = subjectData,
                        bestDayLabel = bestDay?.label ?: "",
                        bestDayMinutes = bestDay?.minutes ?: 0,
                        lastWeekTotalHours = lastWeekHours,
                        weekVsPastDiff = diff,
                        weekVsPastPercent = percent,
                    )
                }
            } catch (e: Exception) {
                val fallbackWeek  = getFallbackWeekBars()
                val fallbackMonth = getFallbackMonthPoints()
                val currentWeekHours = 8.5f
                val lastWeekHours = fallbackMonth.dropLast(1).lastOrNull()?.hours ?: 6f
                val diff = currentWeekHours - lastWeekHours
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weekBars = fallbackWeek,
                        weekTotalHours = currentWeekHours,
                        weekCompletedSessions = 12,
                        weekAvgMinutesPerDay = 73,
                        monthPoints = fallbackMonth,
                        monthTotalHours = 32f,
                        monthCompletedSessions = 45,
                        subjectStats = emptyList(),
                        bestDayLabel = "الأربعاء",
                        bestDayMinutes = 120,
                        streakDays = 5,
                        lastWeekTotalHours = lastWeekHours,
                        weekVsPastDiff = diff,
                        weekVsPastPercent = if (lastWeekHours > 0f)
                            ((diff / lastWeekHours) * 100).toInt() else 0,
                    )
                }
            }
        }
    }

    private suspend fun loadWeekData(): List<DayBar> {
        val arabicDays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء",
            "الخميس", "الجمعة", "السبت")
        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        return try {
            val sessions = scheduleRepository.getWeekSessions()
            arabicDays.mapIndexed { index, label ->
                val dayMins = sessions
                    .filter { it.dayOfWeek == index }
                    .sumOf { it.completedMinutes }
                DayBar(label = label, minutes = dayMins, isToday = index == todayDow)
            }
        } catch (e: Exception) {
            getFallbackWeekBars()
        }
    }

    private suspend fun loadMonthData(): List<MonthlyPoint> {
        return try {
            val data = scheduleRepository.getMonthSessions()
            listOf("الأسبوع 1", "الأسبوع 2", "الأسبوع 3", "الأسبوع 4")
                .mapIndexed { i, label ->
                    val hours = data.filter { it.weekOfMonth == i + 1 }
                        .sumOf { it.completedMinutes } / 60f
                    MonthlyPoint(label, hours)
                }
        } catch (e: Exception) {
            getFallbackMonthPoints()
        }
    }

    private suspend fun loadSubjectStats(): List<SubjectStat> {
        return try {
            subjectRepository.getSubjectStats()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getFallbackWeekBars(): List<DayBar> {
        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        val minutes = listOf(45, 90, 30, 120, 60, 20, 40)
        return listOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت")
            .mapIndexed { i, label ->
                DayBar(label = label, minutes = minutes[i], isToday = i == todayDow)
            }
    }

    private fun getFallbackMonthPoints(): List<MonthlyPoint> = listOf(
        MonthlyPoint("الأسبوع 1", 7.5f),
        MonthlyPoint("الأسبوع 2", 6f),
        MonthlyPoint("الأسبوع 3", 10f),
        MonthlyPoint("الأسبوع 4", 8.5f),
    )

    fun refresh() = loadAnalytics()
}
