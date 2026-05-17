package com.jadwal.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    // ─── إصلاح #5: نحتاج Context لنسخ الصورة إلى مجلد داخلي دائم ───
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        // ─── مراقبة DataStore بشكل مستمر ───
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
                        // ─── إصلاح: تحقق أن الملف لا يزال موجوداً ───
                        // المشكلة كانت: يُحفظ المسار الأصلي من gallery
                        // بعد إعادة التشغيل Android يسحب الصلاحية المؤقتة
                        // الحل: ننسخ الصورة للمجلد الداخلي filesDir
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

    // ─── إصلاح #5: نسخ الصورة إلى مجلد داخلي دائم قبل حفظ المسار ───
    /**
     * المشكلة الأصلية:
     * عند اختيار صورة من الـ Gallery، Android يعطي URI مؤقت (content://)
     * بعد قفل التطبيق تنتهي صلاحية الوصول لهذا URI
     *
     * الحل:
     * ننسخ الصورة فوراً إلى context.filesDir (مجلد داخلي دائم)
     * ونحفظ المسار الكامل الجديد في DataStore
     */
    fun onPhotoSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // إنشاء ملف دائم في المجلد الداخلي
                val profileDir = File(context.filesDir, "profile").also { it.mkdirs() }
                val destFile = File(profileDir, "avatar.jpg")

                // نسخ الصورة
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val permanentPath = destFile.absolutePath
                prefs.setProfilePhotoPath(permanentPath)
                _uiState.update { it.copy(profilePhotoPath = permanentPath) }

            } catch (e: Exception) {
                // فشل نسخ الصورة — لا نحدث UI
            }
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

    // ─── حساب الشارات ───────────────────────────────────────────
    private fun calculateBadges(
        sessions: List<Session>,
        subjects: List<Subject>,
    ): List<AchievementBadge> {
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val totalHours = totalMinutes / 60
        val totalSessions = sessions.size
        val streak = _uiState.value.streakDays

        return listOf(
            AchievementBadge("🎯", "أول خطوة",    "أكمل أول جلسة مذاكرة",            totalSessions >= 1),
            AchievementBadge("📚", "مذاكر نشيط",  "أكمل 10 جلسات مذاكرة",             totalSessions >= 10),
            AchievementBadge("🏆", "محترف",       "أكمل 50 جلسة مذاكرة",              totalSessions >= 50),
            AchievementBadge("⏰", "10 ساعات",    "ذاكر 10 ساعات إجمالاً",             totalHours >= 10),
            AchievementBadge("🌟", "50 ساعة",     "ذاكر 50 ساعة إجمالاً",             totalHours >= 50),
            AchievementBadge("💎", "100 ساعة",    "ذاكر 100 ساعة إجمالاً",            totalHours >= 100),
            AchievementBadge("🔥", "3 أيام 🔥",   "ذاكر 3 أيام متواصلة",              streak >= 3),
            AchievementBadge("⚡", "أسبوع كامل",  "ذاكر أسبوعاً بدون انقطاع",        streak >= 7),
            AchievementBadge("👑", "شهر متواصل",  "ذاكر شهراً كاملاً بدون انقطاع",   streak >= 30),
            AchievementBadge("🎓", "متعدد المواد","أضف 5 مواد أو أكثر",               subjects.size >= 5),
        )
    }
}
