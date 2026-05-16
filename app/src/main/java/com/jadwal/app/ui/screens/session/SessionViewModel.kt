package com.jadwal.ui.screens.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.data.repository.SessionRepository
import com.jadwal.domain.model.UnderstandingLevel
import com.jadwal.notifications.JadwalNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class TimerState { IDLE, RUNNING, PAUSED, BREAK, FINISHED }

data class SessionUiState(
    val subjectName: String = "",
    val subjectIcon: String = "📚",
    val totalMinutes: Int = 25,
    // ===== المؤقت =====
    val timerState: TimerState = TimerState.IDLE,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val currentPomodoroIndex: Int = 0,  // عدد دورات Pomodoro المكتملة
    val isBreak: Boolean = false,
    // ===== نهاية الجلسة =====
    val showRatingSheet: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val sessionRepository: SessionRepository,
    private val notificationManager: JadwalNotificationManager,
) : ViewModel() {

    private val scheduleItemId: String = checkNotNull(savedStateHandle["scheduleItemId"])
    private var subjectId: String = ""

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // ثوابت Pomodoro
    private val pomodoroDuration = 25 * 60   // 25 دقيقة
    private val shortBreak = 5 * 60          // 5 دقائق راحة
    private val longBreak = 15 * 60          // 15 دقيقة راحة طويلة (بعد 4 دورات)

    init {
        loadScheduleItem()
    }

    private fun loadScheduleItem() {
        viewModelScope.launch {
            try {
                val item = scheduleRepository.getScheduleItemWithSubject(scheduleItemId)
                subjectId = item?.subjectId ?: ""
                _uiState.update {
                    it.copy(
                        subjectName = item?.subjectName ?: "مذاكرة",
                        subjectIcon = item?.subjectIcon ?: "📚",
                        totalMinutes = item?.allocatedMinutes ?: 0,
                        remainingSeconds = pomodoroDuration,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        subjectName = "مذاكرة",
                        remainingSeconds = pomodoroDuration,
                    )
                }
            }
        }
    }

    // ===== تشغيل / إيقاف مؤقت =====
    fun toggleTimer() {
        when (_uiState.value.timerState) {
            TimerState.IDLE, TimerState.PAUSED -> startTimer()
            TimerState.RUNNING -> pauseTimer()
            else -> {}
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(timerState = TimerState.RUNNING) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 &&
                _uiState.value.timerState == TimerState.RUNNING
            ) {
                delay(1000L)
                _uiState.update {
                    it.copy(
                        remainingSeconds = it.remainingSeconds - 1,
                        elapsedSeconds = it.elapsedSeconds + 1,
                    )
                }
            }
            if (_uiState.value.remainingSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerState = TimerState.PAUSED) }
    }

    private fun onTimerFinished() {
        val state = _uiState.value
        if (!state.isBreak) {
            // انتهت دورة Pomodoro — ابدأ الراحة
            val nextPomodoro = state.currentPomodoroIndex + 1
            val breakDuration = if (nextPomodoro % 4 == 0) longBreak else shortBreak
            _uiState.update {
                it.copy(
                    timerState = TimerState.BREAK,
                    isBreak = true,
                    remainingSeconds = breakDuration,
                    currentPomodoroIndex = nextPomodoro,
                )
            }
            startTimer()
        } else {
            // انتهت فترة الراحة — تحقق إذا انتهى وقت الجلسة الكلي
            val totalElapsed = state.elapsedSeconds / 60
            if (totalElapsed >= state.totalMinutes) {
                endSession()
            } else {
                // دورة جديدة
                _uiState.update {
                    it.copy(
                        isBreak = false,
                        remainingSeconds = pomodoroDuration,
                        timerState = TimerState.IDLE,
                    )
                }
            }
        }
    }

    // ===== إنهاء الجلسة يدوياً =====
    fun endSession() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                timerState = TimerState.FINISHED,
                showRatingSheet = true,
            )
        }
    }

    // ===== حفظ نتائج الجلسة =====
    fun saveSession(understanding: UnderstandingLevel, notes: String = "") {
        val state = _uiState.value
        val minutesStudied = (state.elapsedSeconds / 60).coerceAtLeast(1)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val session = com.jadwal.domain.model.Session(
                    id = java.util.UUID.randomUUID().toString(),
                    scheduleItemId = scheduleItemId,
                    subjectId = subjectId,
                    startTime = System.currentTimeMillis() - (state.elapsedSeconds * 1000),
                    endTime = System.currentTimeMillis(),
                    durationMinutes = minutesStudied,
                    pomodorosCompleted = state.currentPomodoroIndex,
                    understandingLevel = understanding,
                    notes = notes
                )
                sessionRepository.insertSession(session)
                scheduleRepository.markCompleted(scheduleItemId, minutesStudied, understanding)

                // إشعار إتمام الجلسة
                notificationManager.showSessionComplete(
                    subjectName = state.subjectName,
                    minutesStudied = minutesStudied,
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true, showRatingSheet = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun dismissRatingSheet() {
        _uiState.update { it.copy(showRatingSheet = false, isSaved = true) }
    }

    fun skipBreak() {
        if (_uiState.value.isBreak) {
            timerJob?.cancel()
            _uiState.update {
                it.copy(
                    isBreak = false,
                    remainingSeconds = pomodoroDuration,
                    timerState = TimerState.IDLE,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
