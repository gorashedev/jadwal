package com.jadwal.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.data.repository.SessionRepository
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.model.Session
import com.jadwal.domain.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementBadge(
    val emoji: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
)

data class ProfileUiState(
    val userName: String = "",
    val profilePhotoPath: String = "",
    val totalStudyHours: Float = 0f,
    val totalStudyMinutes: Int = 0,
    val totalSessions: Int = 0,
    val streakDays: Int = 0,
    val topSubjectName: String = "",
    val topSubjectIcon: String = "📚",
    val topSubjectMinutes: Int = 0,
    val badges: List<AchievementBadge> = emptyList(),
    val isLoading: Boolean = true,
    val showEditNameDialog: Boolean = false,
    val editNameText: String = "",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: UserPreferencesDataStore,
    private val sessionRepository: SessionRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            combine(
                prefs.userName,
                prefs.streakDays,
                prefs.profilePhotoPath,
            ) { userName, streak, photoPath ->
                Triple(userName, streak, photoPath)
            }.collect { (userName, streak, photoPath) ->
                _uiState.update {
                    it.copy(
                        userName = userName,
                        streakDays = streak,
                        profilePhotoPath = photoPath,
                    )
                }
            }
        }

        viewModelScope.launch {
            try {
                // جلب كل الجلسات لحساب الإجمالي
                sessionRepository.getAllSessions().collect { sessions ->
                    val totalMinutes = sessions.sumOf { it.durationMinutes }
                    val totalHours = totalMinutes / 60f

                    // أكثر مادة مذاكرة
                    val subjectMinutes = sessions.groupBy { it.subjectId }
                        .mapValues { (_, s) -> s.sumOf { it.durationMinutes } }
                    val topSubjectId = subjectMinutes.maxByOrNull { it.value }?.key

                    var topName = ""
                    var topIcon = "📚"
                    var topMins = 0

                    if (topSubjectId != null) {
                        val subject = subjectRepository.getSubjectById(topSubjectId)
                        topName = subject?.name ?: ""
                        topIcon = subject?.icon ?: "📚"
                        topMins = subjectMinutes[topSubjectId] ?: 0
                    }

                    val badges = buildBadges(
                        totalHours = totalHours,
                        totalSessions = sessions.size,
                        streakDays = _uiState.value.streakDays,
                    )

                    _uiState.update {
                        it.copy(
                            totalStudyHours = totalHours,
                            totalStudyMinutes = totalMinutes,
                            totalSessions = sessions.size,
                            topSubjectName = topName,
                            topSubjectIcon = topIcon,
                            topSubjectMinutes = topMins,
                            badges = badges,
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun buildBadges(
        totalHours: Float,
        totalSessions: Int,
        streakDays: Int,
    ): List<AchievementBadge> = listOf(
        AchievementBadge(
            emoji = "🌱",
            title = "البداية",
            description = "أول جلسة مذاكرة",
            isUnlocked = totalSessions >= 1,
        ),
        AchievementBadge(
            emoji = "⚡",
            title = "المجتهد",
            description = "10 ساعات مذاكرة",
            isUnlocked = totalHours >= 10f,
        ),
        AchievementBadge(
            emoji = "🔥",
            title = "المثابر",
            description = "3 أيام متتالية",
            isUnlocked = streakDays >= 3,
        ),
        AchievementBadge(
            emoji = "🎯",
            title = "المركّز",
            description = "أسبوع كامل متتالي",
            isUnlocked = streakDays >= 7,
        ),
        AchievementBadge(
            emoji = "💎",
            title = "النجم",
            description = "50 ساعة مذاكرة",
            isUnlocked = totalHours >= 50f,
        ),
        AchievementBadge(
            emoji = "🏆",
            title = "الأسطورة",
            description = "100 ساعة مذاكرة",
            isUnlocked = totalHours >= 100f,
        ),
        AchievementBadge(
            emoji = "🌙",
            title = "المداوم",
            description = "30 يوم متتالي",
            isUnlocked = streakDays >= 30,
        ),
        AchievementBadge(
            emoji = "📚",
            title = "المثقف",
            description = "25 جلسة مذاكرة",
            isUnlocked = totalSessions >= 25,
        ),
    )

    fun showEditNameDialog() {
        _uiState.update { it.copy(showEditNameDialog = true, editNameText = it.userName) }
    }

    fun dismissEditNameDialog() {
        _uiState.update { it.copy(showEditNameDialog = false) }
    }

    fun onEditNameChange(text: String) {
        _uiState.update { it.copy(editNameText = text) }
    }

    fun saveName() {
        val newName = _uiState.value.editNameText.trim()
        if (newName.isBlank()) return
        viewModelScope.launch {
            prefs.setUserName(newName)
            _uiState.update { it.copy(showEditNameDialog = false, userName = newName) }
        }
    }

    fun saveProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            prefs.setProfilePhotoPath(uri.toString())
            _uiState.update { it.copy(profilePhotoPath = uri.toString()) }
        }
    }
}
