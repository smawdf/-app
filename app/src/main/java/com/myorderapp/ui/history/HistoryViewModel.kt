package com.myorderapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.repository.MealRepository
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val meals: List<Meal> = emptyList(),
    val viewMode: String = "list", // "list" or "calendar"
    val isLoading: Boolean = true,
    val totalMeals: Int = 0,
    val streakDays: Int = 0,
    val partnerName: String = ""
)

class HistoryViewModel(
    private val mealRepository: MealRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mealRepository.getMealHistory().collect { meals ->
                _uiState.value = _uiState.value.copy(
                    meals = meals,
                    totalMeals = meals.size,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                partnerName = profileRepository.getPairInfo().partnerName
            )
        }
    }

    fun toggleViewMode() {
        val current = _uiState.value.viewMode
        _uiState.value = _uiState.value.copy(
            viewMode = if (current == "list") "calendar" else "list"
        )
    }
}
