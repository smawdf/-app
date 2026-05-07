package com.myorderapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.model.Meal
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.MealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayMeal: Meal? = null,
    val recentDishes: List<Dish> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val dishRepository: DishRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mealRepository.getTodayMeal().collect { meal ->
                _uiState.value = _uiState.value.copy(todayMeal = meal)
            }
        }
        viewModelScope.launch {
            dishRepository.getRecentDishes(5).collect { dishes ->
                _uiState.value = _uiState.value.copy(recentDishes = dishes, isLoading = false)
            }
        }
    }
}
