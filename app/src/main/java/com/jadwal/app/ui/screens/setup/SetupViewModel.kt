package com.jadwal.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.local.dao.SubjectDao
import com.jadwal.data.local.dao.ExamDao
import com.jadwal.data.local.entity.SubjectEntity
import com.jadwal.data.local.entity.ExamEntity
import com.jadwal.data.preferences.UserPreferencesDataStore
import com.jadwal.domain.algorithm.ScheduleAlgorithm
import com.jadwal.domain.model.Difficulty
import com.jadwal.domain.model.StudyTime
import com.jadwal.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ===== نموذج مادة جديدة أثناء الإعداد =====
data class SubjectDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val icon: String = "📚",
    val colorHex: String = "#5C6BC0",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val totalChapters: Int = 1,
    val examDateMillis: Long? = null,
)

data class SetupUiState(
    // ===== القائمة تبدأ فارغة — المستخدم يضيف موادّه بنفسه =====
    val subjects: List<SubjectDraft> = emptyList(),
    val dailyHours: Int = 2,
    val preferredTime: StudyTime = StudyTime.MORNING,
    val isGenerating: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val subjectDao: SubjectDao,
    private val examDao: ExamDao,
    private val prefs: UserPreferencesDataStore,
    private val scheduleAlgorithm: ScheduleAlgorithm,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    // ===== إضافة مادة جديدة (فارغة) =====
    fun addSubject() {
        val newSubject = SubjectDraft(
            id = UUID.randomUUID().toString(),
            name = "",
            icon = availableIcons.random(),
            colorHex = availableColors.random(),
            difficulty = Difficulty.MEDIUM,
        )
        _uiState.update { it.copy(subjects = it.subjects + newSubject) }
    }

    // ===== تعديل اسم المادة =====
    fun updateSubjectName(id: String, name: String) {
        _uiState.update { state ->
            state.copy(
                subjects = state.subjects.map { s ->
                    if (s.id == id) s.copy(name = name) else s
                }
            )
        }
    }

    // ===== تعديل أيقونة المادة =====
    fun updateSubjectIcon(id: String, icon: String) {
        _uiState.update { state ->
            state.copy(
                subjects = state.subjects.map { s ->
                    if (s.id == id) s.copy(icon = icon) else s
                }
            )
        }
    }

    // ===== تعديل صعوبة المادة =====
    fun setSubjectDifficulty(id: String, difficulty: Difficulty) {
        _uiState.update { state ->
            state.copy(
                subjects = state.subjects.map { s ->
                    if (s.id == id) s.copy(difficulty = difficulty) else s
                }
            )
        }
    }

    // ===== تعيين تاريخ الامتحان =====
    fun setExamDate(id: String, dateMillis: Long) {
        _uiState.update { state ->
            state.copy(
                subjects = state.subjects.map { s ->
                    if (s.id == id) s.copy(examDateMillis = dateMillis) else s
                }
            )
        }
    }

    // ===== حذف مادة =====
    fun removeSubject(id: String) {
        _uiState.update { state ->
            state.copy(subjects = state.subjects.filter { it.id != id })
        }
    }

    // ===== عدد الساعات =====
    fun setDailyHours(hours: Int) {
        _uiState.update { it.copy(dailyHours = hours) }
    }

    // ===== وقت الدراسة المفضل =====
    fun setPreferredTime(time: StudyTime) {
        _uiState.update { it.copy(preferredTime = time) }
    }

    // ===== توليد الجدول وحفظ البيانات =====
    fun generateSchedule() {
        val state = _uiState.value

        // تحقق أن هناك مادة واحدة على الأقل بها اسم
        val validSubjects = state.subjects.filter { it.name.isNotBlank() }
        if (validSubjects.isEmpty()) {
            _uiState.update { it.copy(error = "يرجى إضافة مادة واحدة على الأقل") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            try {
                // حفظ المواد في قاعدة البيانات
                val subjectEntities = validSubjects.map { draft ->
                    SubjectEntity(
                        id = draft.id,
                        name = draft.name,
                        nameEn = draft.name, // يمكن إضافة ترجمة لاحقاً
                        difficulty = draft.difficulty.name,
                        colorHex = draft.colorHex,
                        icon = draft.icon,
                        totalChapters = draft.totalChapters,
                    )
                }
                subjectDao.insertAll(subjectEntities)

                // حفظ مواعيد الامتحانات
                val examEntities = validSubjects
                    .filter { it.examDateMillis != null }
                    .map { draft ->
                        ExamEntity(
                            id = UUID.randomUUID().toString(),
                            subjectId = draft.id,
                            examDate = draft.examDateMillis!!,
                        )
                    }
                if (examEntities.isNotEmpty()) {
                    examDao.insertAll(examEntities)
                }

                // حفظ تفضيلات المستخدم
                prefs.setDailyHours(state.dailyHours)
                prefs.setPreferredTime(state.preferredTime.name)
                prefs.setSetupDone(true)

                _uiState.update { it.copy(isGenerating = false, isComplete = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = "حدث خطأ أثناء حفظ البيانات: ${e.message}"
                    )
                }
            }
        }
    }

    // ===== قائمة الأيقونات المتاحة للمواد =====
    val availableIcons = listOf(
        "📐", "🔬", "📖", "💬", "🗺️", "⚗️", "🧬", "💰",
        "🖥️", "🎨", "🎵", "📊", "🏛️", "⚽", "🌍", "📝",
    )

    // ===== قائمة الألوان المتاحة =====
    val availableColors = listOf(
        "#5C6BC0", "#26A69A", "#EF5350", "#42A5F5",
        "#7E57C2", "#66BB6A", "#26C6DA", "#FFB74D",
        "#AB47BC", "#FF7043",
    )
}
