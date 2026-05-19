package com.jadwal.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.data.repository.AIRepository
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.domain.model.ScheduleWithSubject
import com.jadwal.domain.model.StudentSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color  // ← هذا كان مفقوداً

// ===== نموذج المهمة اليومية =====
data class TodayTask(
    val scheduleItemId: String,
    val subjectName: String,
    val subjectIcon: String,
    val subjectColor: Color,
    val allocatedMinutes: Int,
    val isCompleted: Boolean,
    val isInProgress: Boolean = false,
    val progress: Float = 0f,
    val startHour: Int = -1,
    val startMinute: Int = -1,
)

// ===== نموذج الامتحان القادم =====
data class UpcomingExam(
    val subjectName: String,
    val subjectIcon: String,
    val daysUntil: Int,
)

data class HomeUiState(
    val userName: String = "",
    val streakDays: Int = 0,
    val todayTasks: List<TodayTask> = emptyList(),
    val completedMinutes: Int = 0,
    val totalPlannedMinutes: Int = 0,
    val upcomingExam: UpcomingExam? = null,
    val weeklyCompletedSessions: Int = 0,
    val weeklyTotalHours: Float = 0f,
    val isAiLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val aiRepository: AIRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _aiSuggestion = MutableStateFlow<String?>(null)
    val aiSuggestion = _aiSuggestion.asStateFlow()

    init {
        loadHomeData()
        loadAiSuggestion()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            prefs.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }

        viewModelScope.launch {
            prefs.streakDays.collect { streak ->
                _uiState.update { state -> state.copy(streakDays = streak) }
            }
        }

        viewModelScope.launch {
            // التحقق من لغة النظام الحالية
            val isEn = java.util.Locale.getDefault().language == "en"

            try {
                val todayItems = scheduleRepository.getTodayScheduleWithSubjects().map { item ->
                    TodayTask(
                        scheduleItemId = item.id,
                        // هنا التعديل: اختيار الاسم الإنجليزي إذا كان متاحاً وكانت اللغة إنجليزية
                        subjectName = if (isEn && item.subjectNameEn.isNotBlank()) item.subjectNameEn else item.subjectName,
                        subjectIcon = item.subjectIcon,
                        subjectColor = try {
                            Color(android.graphics.Color.parseColor(item.subjectColor))
                        } catch (e: Exception) {
                            Color.Gray
                        },
                        allocatedMinutes = item.allocatedMinutes,
                        isCompleted = item.isCompleted,
                        startHour = item.startHour,
                        startMinute = item.startMinute,
                    )
                }

                val completedMins = todayItems.filter { it.isCompleted }.sumOf { it.allocatedMinutes }
                val totalMins = todayItems.sumOf { it.allocatedMinutes }

                _uiState.update { state ->
                    state.copy(
                        todayTasks = todayItems,
                        completedMinutes = completedMins,
                        totalPlannedMinutes = totalMins,
                    )
                }
            } catch (e: Exception) {
                // تجاهل الخطأ
            }
        }
    }

    // ===== تحميل اقتراح الذكاء الاصطناعي =====
    private fun loadAiSuggestion() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            try {
                val state = _uiState.value
                val summary = StudentSummary(
                    subjects = state.todayTasks.map { it.subjectName },
                    weeklyHours = state.weeklyTotalHours.toInt(),
                    hardestSubject = state.todayTasks.firstOrNull()?.subjectName ?: "",
                    daysUntilNextExam = state.upcomingExam?.daysUntil ?: -1,
                    averageUnderstanding = 2.5f,
                )
                val suggestion = aiRepository.getSmartSuggestion(summary)
                _aiSuggestion.value = suggestion
            } catch (e: Exception) {
                // اقتراح افتراضي عند فشل الاتصال
                _aiSuggestion.value = "استمر في مذاكرتك، كل خطوة تقربك من النجاح!"
            } finally {
                _uiState.update { it.copy(isAiLoading = false) }
            }
        }
    }

    // ===== تحديث الاقتراح يدوياً =====
    fun refreshAiSuggestion() {
        loadAiSuggestion()
    }
}