package com.jadwal.ui.screens.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.domain.model.Difficulty
import com.jadwal.domain.model.Subject
import com.jadwal.data.repository.SubjectRepository
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
)

data class SubjectsUiState(
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val formState: SubjectFormState = SubjectFormState(),
    val errorMessage: String? = null,
)

val SUBJECT_COLOR_PALETTE = listOf(
    "#5C6BC0",  // indigo
    "#7E57C2",  // violet
    "#26A69A",  // teal
    "#EF5350",  // red
    "#FF7043",  // deep orange
    "#66BB6A",  // green
    "#FFA726",  // orange
    "#42A5F5",  // blue
    "#AB47BC",  // purple
    "#EC407A",  // pink
)

val SUBJECT_ICONS = listOf(
    "📚", "📐", "🔬", "🧮", "🌍", "📖", "💻", "🎨", "🎵", "⚗️",
    "🧬", "📝", "🏛️", "🔭", "🌿", "📊", "🧠", "✏️", "🔢", "🗺️",
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
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
                ),
            )
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
                if (form.isEditing) {
                    val updated = Subject(
                        id = form.id,
                        name = form.name.trim(),
                        nameEn = form.nameEn.trim(),
                        difficulty = form.difficulty,
                        colorHex = form.colorHex,
                        icon = form.icon,
                        totalChapters = chapters,
                    )
                    subjectRepository.updateSubject(updated)
                } else {
                    val newSubject = Subject(
                        id = UUID.randomUUID().toString(),
                        name = form.name.trim(),
                        nameEn = form.nameEn.trim(),
                        difficulty = form.difficulty,
                        colorHex = form.colorHex,
                        icon = form.icon,
                        totalChapters = chapters,
                    )
                    subjectRepository.insertSubject(newSubject)
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
