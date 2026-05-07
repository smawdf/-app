package com.myorderapp.ui.dishdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DishDetailUiState(
    val dish: Dish? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val wishlistAdded: Boolean = false
)

class DishDetailViewModel(
    private val dishRepository: DishRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DishDetailUiState())
    val uiState: StateFlow<DishDetailUiState> = _uiState.asStateFlow()

    fun loadDish(dishId: String) {
        viewModelScope.launch {
            _uiState.value = DishDetailUiState(isLoading = true)
            val dish = dishRepository.getDishById(dishId)
            _uiState.value = DishDetailUiState(dish = dish, isLoading = false)
        }
    }

    fun addToWishlist() {
        val dish = _uiState.value.dish ?: return
        if (_uiState.value.wishlistAdded) return
        viewModelScope.launch {
            wishlistRepository.addToWishlist(
                dishId = dish.id,
                dishName = dish.name,
                category = dish.category,
                addedBy = "u1",
                addedByName = "你"
            )
            _uiState.value = _uiState.value.copy(wishlistAdded = true)
        }
    }

    fun deleteDish() {
        val dish = _uiState.value.dish ?: return
        viewModelScope.launch {
            dishRepository.deleteDish(dish.id)
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }
    }
}
