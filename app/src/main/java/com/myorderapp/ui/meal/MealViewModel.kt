package com.myorderapp.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.model.MealItem
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.MealRepository
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MealUiState(
    val step: Int = 0,
    val mealType: String = "",
    val allDishes: List<Dish> = emptyList(),
    val searchQuery: String = "",
    val mySelections: List<MealItem> = emptyList(),
    val partnerSelections: List<MealItem> = emptyList(),
    val mealId: String = "",
    val partnerName: String = "对方",
    val partnerConnected: Boolean = false,
    val partnerSubmitted: Boolean = false,
    val mySubmitted: Boolean = false,
    val bothSubmitted: Boolean = false
)

class MealViewModel(
    private val mealRepository: MealRepository,
    private val dishRepository: DishRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dishRepository.getAllDishes().collect { dishes ->
                _uiState.value = _uiState.value.copy(allDishes = dishes)
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                partnerName = profileRepository.getPairInfo().partnerName.ifBlank { "对方" }
            )
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredDishes(): List<Dish> {
        val state = _uiState.value
        val q = state.searchQuery.trim().lowercase()
        return if (q.isBlank()) state.allDishes
        else state.allDishes.filter {
            it.name.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.ingredients.any { ing -> ing.lowercase().contains(q) }
        }
    }

    fun selectMealType(type: String) {
        _uiState.value = _uiState.value.copy(mealType = type, step = 1)
        viewModelScope.launch {
            val id = mealRepository.createMeal(type, "你")
            _uiState.value = _uiState.value.copy(mealId = id)
        }
    }

    fun addDish(dish: Dish) {
        val item = MealItem(
            id = "mi_${System.currentTimeMillis()}",
            dishId = dish.id, dishName = dish.name,
            dishCategory = dish.category, dishImageUrl = dish.imageUrl,
            cookTimeMin = dish.cookTimeMin, difficulty = dish.difficulty,
            chosenBy = "u1", chosenByName = "你"
        )
        _uiState.value = _uiState.value.copy(
            mySelections = _uiState.value.mySelections + item
        )
    }

    fun removeMyDish(itemId: String) {
        _uiState.value = _uiState.value.copy(
            mySelections = _uiState.value.mySelections.filter { it.id != itemId },
            mySubmitted = false
        )
    }

    fun submitMySelection() {
        viewModelScope.launch {
            mealRepository.submitSelection(_uiState.value.mealId, "u1")
            _uiState.value = _uiState.value.copy(mySubmitted = true)
            // 对方未提交时仅标记自己已提交，不自动跳转
            if (_uiState.value.partnerSubmitted) {
                _uiState.value = _uiState.value.copy(bothSubmitted = true, step = 2)
            }
        }
    }

    fun onPartnerSubmitted() {
        _uiState.value = _uiState.value.copy(partnerSubmitted = true)
        if (_uiState.value.mySubmitted) {
            _uiState.value = _uiState.value.copy(bothSubmitted = true, step = 2)
        }
    }

    fun confirmMeal() {
        viewModelScope.launch {
            mealRepository.confirmMeal(_uiState.value.mealId)
        }
    }
}
