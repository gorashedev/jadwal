package com.jadwal.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.repository.ExamRepository
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.model.Difficulty
import com.jadwal.domain.model.Exam
import com.jadwal.domain.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SubjectFormState(
    val id: String = "",
    val name: String = "",
    val nameEn: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val colorHex: String = "#5C6BC0",
    val icon: String = "📚",
    val totalChapters: String = "10",
    val isEditing: Boolean = false,
    val examDate: Long? = null,
)

data class SubjectsUiState(
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val formState: SubjectFormState = SubjectFormState(),
    val errorMessage: String? = null,
)

val SUBJECT_COLOR_PALETTE = listOf(
    "#5C6BC0",
    "#7E57C2",
    "#26A69A",
    "#EF5350",
    "#FF7043",
    "#66BB6A",
    "#FFA726",
    "#42A5F5",
    "#AB47BC",
    "#EC407A",
)

val SUBJECT_ICONS = listOf(
    "📚", "📐", "🔬", "🧮", "🌍", "📖", "💻", "🎨", "🎵", "⚗️",
    "🧬", "📝", "🏛️", "🔭", "🌿", "📊", "🧠", "✏️", "🔢", "🗺️",
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val examRepository: ExamRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            subjectRepository.getAllSubjects()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { subjects ->
                    _uiState.update { it.copy(subjects = subjects, isLoading = false) }
                }
        }
    }

    // ===== إدارة الحوار =====

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                formState = SubjectFormState(),
            )
        }
    }

    fun showEditDialog(subject: Subject) {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                formState = SubjectFormState(
                    id = subject.id,
                    name = subject.name,
                    nameEn = subject.nameEn,
                    difficulty = subject.difficulty,
                    colorHex = subject.colorHex,
                    icon = subject.icon,
                    totalChapters = subject.totalChapters.toString(),
                    isEditing = true,
                    examDate = null,
                ),
            )
        }
        // تحميل تاريخ الامتحان للمادة المُعدَّلة
        viewModelScope.launch {
            try {
                val exam = examRepository.getExamBySubject(subject.id)
                if (exam != null) {
                    _uiState.update { state ->
                        state.copy(formState = state.formState.copy(examDate = exam.examDate))
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    // ===== تحديث حقول النموذج =====

    fun updateName(value: String) {
        _uiState.update { it.copy(formState = it.formState.copy(name = value)) }
    }

    fun updateNameEn(value: String) {
        _uiState.update { it.copy(formState = it.formState.copy(nameEn = value)) }
    }

    fun updateDifficulty(value: Difficulty) {
        _uiState.update { it.copy(formState = it.formState.copy(difficulty = value)) }
    }

    fun updateColor(value: String) {
        _uiState.update { it.copy(formState = it.formState.copy(colorHex = value)) }
    }

    fun updateIcon(value: String) {
        _uiState.update { it.copy(formState = it.formState.copy(icon = value)) }
    }

    fun updateTotalChapters(value: String) {
        if (value.isEmpty() || value.toIntOrNull() != null) {
            _uiState.update { it.copy(formState = it.formState.copy(totalChapters = value)) }
        }
    }

    fun updateExamDate(dateMs: Long?) {
        _uiState.update { it.copy(formState = it.formState.copy(examDate = dateMs)) }
    }

    // ===== حفظ وحذف المادة =====

    fun saveSubject() {
        val form = _uiState.value.formState
        if (form.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم المادة") }
            return
        }

        val chapters = form.totalChapters.toIntOrNull()?.coerceAtLeast(1) ?: 1

        viewModelScope.launch {
            try {
                val subjectId: String
                if (form.isEditing) {
                    subjectId = form.id
                    subjectRepository.updateSubject(
                        Subject(
                            id = subjectId,
                            name = form.name.trim(),
                            nameEn = form.nameEn.trim(),
                            difficulty = form.difficulty,
                            colorHex = form.colorHex,
                            icon = form.icon,
                            totalChapters = chapters,
                        )
                    )
                } else {
                    subjectId = UUID.randomUUID().toString()
                    subjectRepository.insertSubject(
                        Subject(
                            id = subjectId,
                            name = form.name.trim(),
                            nameEn = form.nameEn.trim(),
                            difficulty = form.difficulty,
                            colorHex = form.colorHex,
                            icon = form.icon,
                            totalChapters = chapters,
                        )
                    )
                }

                // حفظ / تحديث موعد الامتحان
                if (form.examDate != null) {
                    val existing = if (form.isEditing) examRepository.getExamBySubject(subjectId) else null
                    if (existing != null) {
                        examRepository.updateExam(existing.copy(examDate = form.examDate))
                    } else {
                        examRepository.insertExam(
                            Exam(
                                id = UUID.randomUUID().toString(),
                                subjectId = subjectId,
                                examDate = form.examDate,
                            )
                        )
                    }
                }

                _uiState.update { it.copy(showAddDialog = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "فشل الحفظ: ${e.message}") }
            }
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            try {
                subjectRepository.deleteSubject(subject)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "فشل الحذف: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
