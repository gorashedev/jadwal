package com.jadwal.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.R
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.data.repository.SessionRepository
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.model.Session
import com.jadwal.domain.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AchievementBadge(
    val emoji: String,
    val titleRes: Int,
    val descriptionRes: Int,
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
    @ApplicationContext private val context: Context,
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
                        profilePhotoPath = if (photoPath.isNotBlank() && File(photoPath).exists())
                            photoPath else "",
                    )
                }
            }
        }

        viewModelScope.launch {
            try {
                sessionRepository.getAllSessions().collect { sessions ->
                    val totalMinutes = sessions.sumOf { it.durationMinutes }
                    val totalHours = totalMinutes / 60f

                    val subjectMinutes = sessions.groupBy { it.subjectId }
                        .mapValues { (_, s) -> s.sumOf { it.durationMinutes } }
                    val topSubjectId = subjectMinutes.maxByOrNull { it.value }?.key ?: ""

                    val subjects = subjectRepository.getAllSubjects().first()
                    val topSubject = subjects.find { it.id == topSubjectId }

                    val badges = calculateBadges(sessions, subjects)

                    _uiState.update {
                        it.copy(
                            totalStudyMinutes = totalMinutes,
                            totalStudyHours = totalHours,
                            totalSessions = sessions.size,
                            topSubjectName = topSubject?.name ?: "",
                            topSubjectIcon = topSubject?.icon ?: "📚",
                            topSubjectMinutes = subjectMinutes[topSubjectId] ?: 0,
                            badges = badges,
                            isLoading = false,
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onPhotoSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val profileDir = File(context.filesDir, "profile").also { it.mkdirs() }
                val destFile = File(profileDir, "avatar.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val permanentPath = destFile.absolutePath
                prefs.setProfilePhotoPath(permanentPath)
                _uiState.update { it.copy(profilePhotoPath = permanentPath) }

            } catch (e: Exception) { }
        }
    }

    fun showEditNameDialog() {
        _uiState.update {
            it.copy(showEditNameDialog = true, editNameText = it.userName)
        }
    }

    fun onEditNameChange(name: String) {
        _uiState.update { it.copy(editNameText = name) }
    }

    fun saveNewName() {
        val newName = _uiState.value.editNameText.trim()
        if (newName.isBlank()) return
        viewModelScope.launch {
            prefs.setUserName(newName)
            _uiState.update { it.copy(userName = newName, showEditNameDialog = false) }
        }
    }

    fun dismissEditNameDialog() {
        _uiState.update { it.copy(showEditNameDialog = false) }
    }

    private fun calculateBadges(sessions: List<Session>, subjects: List<Subject>): List<AchievementBadge> {
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val totalHours = totalMinutes / 60
        val totalSessions = sessions.size
        val streak = _uiState.value.streakDays

        return listOf(
            AchievementBadge("🎯", R.string.badge_first_step_title, R.string.badge_first_step_desc, totalSessions >= 1),
            AchievementBadge("📚", R.string.badge_active_title, R.string.badge_active_desc, totalSessions >= 10),
            AchievementBadge("🏆", R.string.badge_pro_title, R.string.badge_pro_desc, totalSessions >= 50),
            AchievementBadge("⏰", R.string.badge_10h_title, R.string.badge_10h_desc, totalHours >= 10),
            AchievementBadge("🌟", R.string.badge_50h_title, R.string.badge_50h_desc, totalHours >= 50),
            AchievementBadge("💎", R.string.badge_100h_title, R.string.badge_100h_desc, totalHours >= 100),
            AchievementBadge("🔥", R.string.badge_3d_title, R.string.badge_3d_desc, streak >= 3),
            AchievementBadge("⚡", R.string.badge_1w_title, R.string.badge_1w_desc, streak >= 7),
            AchievementBadge("👑", R.string.badge_1m_title, R.string.badge_1m_desc, streak >= 30),
            AchievementBadge("🎓", R.string.badge_multi_title, R.string.badge_multi_desc, subjects.size >= 5),
        )
    }
}