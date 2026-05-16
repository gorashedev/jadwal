package com.jadwal.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jadwal.data.repository.ScheduleRepository
import com.jadwal.data.repository.SubjectRepository
import com.jadwal.domain.model.AISuggestion
import com.jadwal.domain.model.ScheduleSlot
import com.jadwal.domain.usecase.GenerateAISuggestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AISuggestionState {
    data object Idle : AISuggestionState()
    data object Loading : AISuggestionState()
    data class Success(val suggestion: AISuggestion) : AISuggestionState()
    data class Error(val message: String) : AISuggestionState()
    data object Saved : AISuggestionState()
}

data class AISuggestionUiState(
    val state: AISuggestionState = AISuggestionState.Idle,
    val isSaving: Boolean = false,
    val showSaveSuccess: Boolean = false,
    val selectedSlots: Set<String> = emptySet(),   // IDs of selected schedule slots
)

@HiltViewModel
class AISuggestionViewModel @Inject constructor(
    private val generateAISuggestionUseCase: GenerateAISuggestionUseCase,
    private val scheduleRepository: ScheduleRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AISuggestionUiState())
    val uiState = _uiState.asStateFlow()

    init { generate() }

    fun generate() {
        viewModelScope.launch {
            _uiState.update { it.copy(state = AISuggestionState.Loading, selectedSlots = emptySet()) }
            try {
                val suggestion = generateAISuggestionUseCase()
                // بتحديد جميع الـ slots تلقائياً
                val allIds = suggestion.schedule.map { it.id }.toSet()
                _uiState.update {
                    it.copy(
                        state = AISuggestionState.Success(suggestion),
                        selectedSlots = allIds,
                    )
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "تعذّر الاتصال بالإنترنت. تحقق من اتصالك وحاول مجدداً."
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "تم استنفاد حصة الذكاء الاصطناعي اليوم. حاول غداً."
                    else -> "حدث خطأ غير متوقع. حاول مجدداً."
                }
                _uiState.update { it.copy(state = AISuggestionState.Error(msg)) }
            }
        }
    }

    fun toggleSlot(slotId: String) {
        val current = _uiState.value.selectedSlots
        _uiState.update {
            it.copy(
                selectedSlots = if (slotId in current) current - slotId else current + slotId
            )
        }
    }

    fun selectAll() {
        val suggestion = (_uiState.value.state as? AISuggestionState.Success)?.suggestion ?: return
        _uiState.update { it.copy(selectedSlots = suggestion.schedule.map { s -> s.id }.toSet()) }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedSlots = emptySet()) }
    }

    fun saveSelectedSlots() {
        val state = _uiState.value
        val suggestion = (state.state as? AISuggestionState.Success)?.suggestion ?: return
        val selectedItems = suggestion.schedule.filter { it.id in state.selectedSlots }
        if (selectedItems.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                scheduleRepository.saveScheduleSlots(selectedItems)
                _uiState.update { it.copy(isSaving = false, showSaveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(showSaveSuccess = false) }
    }
}
