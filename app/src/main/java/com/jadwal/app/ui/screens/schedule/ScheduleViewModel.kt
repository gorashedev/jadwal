package com.jadwal.ui.screens.schedule

import androidx.appcompat.app.AppCompatDelegate
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
    val examNightExams: List<ExamBadge> = emptyList(),
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

    /**
     * إصلاح: AppCompatDelegate.getApplicationLocales() يعكس لغة التطبيق الفعلية
     * فور تغييرها، بعكس Locale.getDefault() الذي يعكس locale النظام.
     */
    private fun isEnglish(): Boolean {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) locales[0]?.language == "en"
        else java.util.Locale.getDefault().language == "en"
    }

    private fun resolveSubjectName(arabicName: String, englishName: String): String =
        if (isEnglish() && englishName.isNotBlank()) englishName else arabicName

    private fun loadSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val items = scheduleRepository.getWeekScheduleWithSubjects().map { item ->
                    ScheduleItem(
                        id = item.id,
                        subjectName = resolveSubjectName(item.subjectName, item.subjectNameEn),
                        subjectIcon = item.subjectIcon,
                        colorHex = item.subjectColor,
                        dayOfWeek = item.dayOfWeek,
                        startHour = item.startHour,
                        startMinute = item.startMinute,
                        durationMinutes = item.allocatedMinutes,
                        isCompleted = item.isCompleted,
                    )
                }
                val grouped = items.groupBy { it.dayOfWeek }
                val exams = scheduleRepository.getUpcomingExamBadges()
                val nightExams = exams.filter { it.daysUntil <= 2 }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weekItems = grouped,
                        upcomingExams = exams,
                        examNightExams = nightExams,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun generateSchedule() {
        if (_uiState.value.isGenerating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, isLoading = true) }
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

                val items = scheduleRepository.getWeekScheduleWithSubjects().map { item ->
                    ScheduleItem(
                        id = item.id,
                        subjectName = resolveSubjectName(item.subjectName, item.subjectNameEn),
                        subjectIcon = item.subjectIcon,
                        colorHex = item.subjectColor,
                        dayOfWeek = item.dayOfWeek,
                        startHour = item.startHour,
                        startMinute = item.startMinute,
                        durationMinutes = item.allocatedMinutes,
                        isCompleted = item.isCompleted,
                    )
                }
                val grouped = items.groupBy { it.dayOfWeek }
                val exams = scheduleRepository.getUpcomingExamBadges()
                val nightExams = exams.filter { it.daysUntil <= 2 }

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        weekItems = grouped,
                        upcomingExams = exams,
                        examNightExams = nightExams,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isGenerating = false, isLoading = false) }
            }
        }
    }

    fun refresh() = loadSchedule()

    fun resetSchedule() {
        if (_uiState.value.isGenerating || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, isLoading = true) }
            try {
                scheduleRepository.deleteAllItems()
            } catch (_: Exception) { }
            finally {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        isLoading = false,
                        weekItems = emptyMap(),
                        upcomingExams = emptyList(),
                        examNightExams = emptyList(),
                    )
                }
            }
        }
    }
}
