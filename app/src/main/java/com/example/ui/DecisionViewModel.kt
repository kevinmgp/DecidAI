package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DecisionEntity
import com.example.data.DecisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val decision: DecisionEntity) : UiState
    data class Error(val message: String) : UiState
}

class DecisionViewModel(private val repository: DecisionRepository) : ViewModel() {

    val allDecisions: StateFlow<List<DecisionEntity>> = repository.allDecisions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _selectedDecision = MutableStateFlow<DecisionEntity?>(null)
    val selectedDecision: StateFlow<DecisionEntity?> = _selectedDecision.asStateFlow()

    fun selectDecision(decision: DecisionEntity?) {
        _selectedDecision.value = decision
    }

    fun generateDecision(question: String, optionA: String, optionB: String) {
        if (question.isBlank()) {
            _uiState.value = UiState.Error("Please enter a decision question or topic.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = repository.analyzeNewDecision(
                    question = question,
                    explicitOptionA = optionA.trim(),
                    explicitOptionB = optionB.trim()
                )
                _uiState.value = UiState.Success(result)
                _selectedDecision.value = result
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred during analysis.")
            }
        }
    }

    fun deleteDecision(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_selectedDecision.value?.id == id) {
                _selectedDecision.value = null
            }
        }
    }

    fun clearAllDecisions() {
        viewModelScope.launch {
            repository.clearAll()
            _selectedDecision.value = null
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}

class DecisionViewModelFactory(private val repository: DecisionRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DecisionViewModel::class.java)) {
            return DecisionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
