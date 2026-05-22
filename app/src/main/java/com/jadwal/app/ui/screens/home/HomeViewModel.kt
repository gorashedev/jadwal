package com.jadwal.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.app.util.LocaleHelper
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.data.repository.AIRepository
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.domain.model.ScheduleWithSubject
import com.jadwal.domain.model.StudentSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color

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
    val isAiLoading: Boolean = false,
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

    private var latestScheduleItems: List<ScheduleWithSubject> = emptyList()

    init {
        viewModelScope.launch {
            prefs.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }

        viewModelScope.launch {
            prefs.streakDays.collect { streak ->
                _uiState.update { it.copy(streakDays = streak) }
            }
        }

        viewModelScope.launch {
            scheduleRepository.observeTodayScheduleWithSubjects()
                .distinctUntilChanged()
                .collect { items ->
                    latestScheduleItems = items
                    applyTodayTasks(items)
                }
        }

        viewModelScope.launch {
            prefs.languageCode
                .distinctUntilChanged()
                .collect { applyTodayTasks(latestScheduleItems) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                latestScheduleItems = scheduleRepository.getTodayScheduleWithSubjects()
                applyTodayTasks(latestScheduleItems)
            } catch (_: Exception) { }
        }
    }

    private fun resolveSubjectName(arabicName: String, englishName: String): String =
        if (LocaleHelper.isEnglish() && englishName.isNotBlank()) englishName else arabicName

    private fun applyTodayTasks(items: List<ScheduleWithSubject>) {
        val todayItems = items.map { item ->
            TodayTask(
                scheduleItemId = item.id,
                subjectName = resolveSubjectName(item.subjectName, item.subjectNameEn),
                subjectIcon = item.subjectIcon,
                subjectColor = try {
                    Color(android.graphics.Color.parseColor(item.subjectColor))
                } catch (_: Exception) {
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
    }

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
                _aiSuggestion.value = aiRepository.getSmartSuggestion(summary)
            } catch (_: Exception) {
                _aiSuggestion.value = null
            } finally {
                _uiState.update { it.copy(isAiLoading = false) }
            }
        }
    }

    fun refreshAiSuggestion() = loadAiSuggestion()
}
