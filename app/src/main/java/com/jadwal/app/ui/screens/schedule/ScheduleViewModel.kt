package com.jadwal.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.algorithm.ScheduleAlgorithm
import com.jadwal.domain.model.StudyTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ScheduleItem(
    val id: String,
    val subjectName: String,
    val subjectIcon: String,
    val colorHex: String,
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val isCompleted: Boolean,
)

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val selectedDayIndex: Int = 0,
    val weekItems: Map<Int, List<ScheduleItem>> = emptyMap(),
    val upcomingExams: List<ExamBadge> = emptyList(),
    val examNightExams: List<ExamBadge> = emptyList(), // امتحانات خلال 48 ساعة (وضع الطوارئ)
    val isGenerating: Boolean = false,
)

data class ExamBadge(
    val subjectName: String,
    val subjectIcon: String,
    val daysUntil: Int,
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val subjectRepository: SubjectRepository,
    private val scheduleAlgorithm: ScheduleAlgorithm,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            selectedDayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadSchedule()
    }

    fun selectDay(index: Int) {
        _uiState.update { it.copy(selectedDayIndex = index) }
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val items = scheduleRepository.getWeekScheduleWithSubjects().map { item ->
                    ScheduleItem(
                        id = item.id,
                        subjectName = item.subjectName,
                        subjectIcon = item.subjectIcon,
                        colorHex = item.subjectColor,
                        dayOfWeek = item.dayOfWeek,
                        startHour = item.startHour,
                        startMinute = item.startMinute,
                        durationMinutes = item.allocatedMinutes,
                        isCompleted = item.isCompleted
                    )
                }
                val grouped = items.groupBy { it.dayOfWeek }
                val exams = scheduleRepository.getUpcomingExamBadges()

                // وضع الطوارئ: امتحانات خلال 48 ساعة (daysUntil <= 2)
                val nightExams = exams.filter { it.daysUntil <= 2 }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weekItems = grouped,
                        upcomingExams = exams,
                        examNightExams = nightExams,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun generateSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val subjects = subjectRepository.getAllSubjects().first()
                if (subjects.isNotEmpty()) {
                    scheduleRepository.deleteAllItems()
                    val slots = scheduleAlgorithm.generateSchedule(
                        subjects = subjects,
                        dailyHours = 2,
                        preferredTime = StudyTime.MORNING,
                    )
                    scheduleRepository.saveScheduleSlots(slots)
                }
                _uiState.update { it.copy(isGenerating = false) }
                loadSchedule()
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false) }
                loadSchedule()
            }
        }
    }

    fun refresh() = loadSchedule()

    fun resetSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                scheduleRepository.deleteAllItems()
            } catch (e: Exception) {
                // تجاهل الخطأ
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
                loadSchedule()
            }
        }
    }
}
