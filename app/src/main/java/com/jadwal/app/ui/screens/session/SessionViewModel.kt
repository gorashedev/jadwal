package com.jadwal.ui.screens.session

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.data.repository.SessionRepository
import com.jadwal.domain.model.UnderstandingLevel
import com.jadwal.app.notifications.JadwalNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class TimerState { IDLE, RUNNING, PAUSED, BREAK, FINISHED }

data class SessionUiState(
    val subjectName: String = "",
    val subjectIcon: String = "📚",
    val totalMinutes: Int = 25,
    val timerState: TimerState = TimerState.IDLE,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val currentPomodoroIndex: Int = 0,
    val isBreak: Boolean = false,
    val showRatingSheet: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val dndEnabled: Boolean = false,
    // ─── إصلاح #4: إضافة حالة إذن DND ───
    val showDndPermissionDialog: Boolean = false,
    val dndPermissionGranted: Boolean = false,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val sessionRepository: SessionRepository,
    private val notificationManager: JadwalNotificationManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val scheduleItemId: String = checkNotNull(savedStateHandle["scheduleItemId"])
    private var subjectId: String = ""

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null

    private val pomodoroDuration = 25 * 60
    private val shortBreak = 5 * 60
    private val longBreak = 15 * 60

    init {
        loadScheduleItem()
        // ─── إصلاح #4: تحقق من إذن DND عند تحميل الشاشة ───
        checkDndPermission()
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
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        subjectName = "مذاكرة",
                        remainingSeconds = pomodoroDuration,
                    )
                }
            }
        }
    }

    // ─── إصلاح #4: فحص إذن DND ───────────────────────────────
    private fun checkDndPermission() {
        val nm = context.getSystemService(NotificationManager::class.java)
        val granted = nm?.isNotificationPolicyAccessGranted == true
        _uiState.update { it.copy(dndPermissionGranted = granted) }
    }

    /**
     * يُستدعى من الـ UI عندما يضغط المستخدم على أيقونة DND
     * إذا لم يكن الإذن ممنوحاً، يعرض dialog يطلب فيه
     */
    fun onDndToggleRequested() {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm?.isNotificationPolicyAccessGranted != true) {
            // ─── عرض dialog يشرح ويطلب الإذن ───
            _uiState.update { it.copy(showDndPermissionDialog = true) }
        } else {
            // الإذن موجود — فقط أخبر المستخدم أنه سيُفعَّل تلقائياً عند بدء الجلسة
            _uiState.update { it.copy(dndPermissionGranted = true) }
        }
    }

    /**
     * يفتح إعدادات الـ DND لمنح الإذن
     * يُستدعى عند ضغط "اذهب للإعدادات" في الـ dialog
     */
    fun openDndSettings() {
        _uiState.update { it.copy(showDndPermissionDialog = false) }
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun dismissDndDialog() {
        _uiState.update { it.copy(showDndPermissionDialog = false) }
    }

    // ─── Pomodoro Timer ──────────────────────────────────────
    fun toggleTimer() {
        when (_uiState.value.timerState) {
            TimerState.IDLE, TimerState.PAUSED -> startTimer()
            TimerState.RUNNING -> pauseTimer()
            else -> {}
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(timerState = TimerState.RUNNING) }
        // ─── إصلاح #4: DND يُفعَّل فقط إذا كان الإذن ممنوحاً ───
        if (!_uiState.value.isBreak) {
            enableDnd()
        }
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
        disableDnd()
    }

    private fun onTimerFinished() {
        val state = _uiState.value
        if (!state.isBreak) {
            disableDnd()
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
            val totalElapsed = state.elapsedSeconds / 60
            if (totalElapsed >= state.totalMinutes) {
                endSession()
            } else {
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

    fun endSession() {
        timerJob?.cancel()
        disableDnd()
        _uiState.update {
            it.copy(
                timerState = TimerState.FINISHED,
                showRatingSheet = true,
            )
        }
    }

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
                    startTime = System.currentTimeMillis() - (state.elapsedSeconds * 1000L),
                    endTime = System.currentTimeMillis(),
                    durationMinutes = minutesStudied,
                    pomodorosCompleted = state.currentPomodoroIndex,
                    understandingLevel = understanding,
                    notes = notes,
                )
                sessionRepository.insertSession(session)
                scheduleRepository.markCompleted(scheduleItemId, minutesStudied, understanding)
                notificationManager.showSessionComplete(
                    subjectName = state.subjectName,
                    minutesStudied = minutesStudied,
                )
                _uiState.update {
                    it.copy(isSaving = false, isSaved = true, showRatingSheet = false)
                }
            } catch (_: Exception) {
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

    // ─── DND Helpers ─────────────────────────────────────────
    private fun enableDnd() {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm?.isNotificationPolicyAccessGranted == true) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                _uiState.update { it.copy(dndEnabled = true, dndPermissionGranted = true) }
            }
        } catch (_: Exception) { }
    }

    private fun disableDnd() {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm?.isNotificationPolicyAccessGranted == true) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (_: Exception) { }
        _uiState.update { it.copy(dndEnabled = false) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        disableDnd()
    }
}
