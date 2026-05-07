package com.myorderapp.ui.adddish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddDishUiState(
    val name: String = "",
    val category: String = "中餐",
    val difficulty: Int = 1,
    val cookTimeMin: String = "",
    val servings: String = "2",
    val imageUrl: String = "",
    val ingredients: List<String> = emptyList(),
    val ingredientInput: String = "",
    val cookSteps: List<CookStep> = emptyList(),
    val notes: String = "",
    val whoLikesYou: Boolean = true,
    val whoLikesPartner: Boolean = true,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false
)

class AddDishViewModel(
    private val dishRepository: DishRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDishUiState())
    val uiState: StateFlow<AddDishUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun onCategoryChanged(category: String) { _uiState.value = _uiState.value.copy(category = category) }
    fun onDifficultyChanged(difficulty: Int) { _uiState.value = _uiState.value.copy(difficulty = difficulty) }
    fun onCookTimeChanged(time: String) { _uiState.value = _uiState.value.copy(cookTimeMin = time) }

    fun onIngredientInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(ingredientInput = input)
    }

    fun addIngredient() {
        val input = _uiState.value.ingredientInput.trim()
        if (input.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                ingredients = _uiState.value.ingredients + input,
                ingredientInput = ""
            )
        }
    }

    fun removeIngredient(index: Int) {
        _uiState.value = _uiState.value.copy(
            ingredients = _uiState.value.ingredients.toMutableList().also { it.removeAt(index) }
        )
    }

    fun addStep() {
        _uiState.value = _uiState.value.copy(
            cookSteps = _uiState.value.cookSteps + CookStep(
                step = _uiState.value.cookSteps.size + 1,
                description = ""
            )
        )
    }

    fun updateStep(index: Int, description: String) {
        val steps = _uiState.value.cookSteps.toMutableList()
        if (index < steps.size) {
            steps[index] = steps[index].copy(description = description)
            _uiState.value = _uiState.value.copy(cookSteps = steps)
        }
    }

    fun removeStep(index: Int) {
        val steps = _uiState.value.cookSteps.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(cookSteps = steps.mapIndexed { i, s -> s.copy(step = i + 1) })
    }

    fun onImageUrlChanged(url: String) { _uiState.value = _uiState.value.copy(imageUrl = url) }
    fun onNotesChanged(notes: String) { _uiState.value = _uiState.value.copy(notes = notes) }
    fun onServingsChanged(servings: String) { _uiState.value = _uiState.value.copy(servings = servings) }
    fun toggleWhoLikesYou() { _uiState.value = _uiState.value.copy(whoLikesYou = !_uiState.value.whoLikesYou) }
    fun toggleWhoLikesPartner() { _uiState.value = _uiState.value.copy(whoLikesPartner = !_uiState.value.whoLikesPartner) }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val state = _uiState.value
            val whoLikes = buildList {
                if (state.whoLikesYou) add("你")
                if (state.whoLikesPartner) add("她")
            }
            dishRepository.addDish(
                Dish(
                    name = state.name,
                    category = state.category,
                    difficulty = state.difficulty,
                    cookTimeMin = state.cookTimeMin.toIntOrNull() ?: 0,
                    imageUrl = state.imageUrl,
                    ingredients = state.ingredients,
                    cookSteps = state.cookSteps,
                    notes = state.notes,
                    whoLikes = whoLikes,
                    source = "custom",
                    createdBy = "你创建"
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccess = true)
        }
    }
}
