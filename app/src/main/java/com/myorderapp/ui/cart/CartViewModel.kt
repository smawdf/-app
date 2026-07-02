package com.myorderapp.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.repository.CartRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartUiState(
    val cartState: CartState = CartState()
)

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.observeCart().collect { cart ->
                _uiState.value = CartUiState(cartState = cart)
            }
        }
    }

    fun increase(menuItemId: String) {
        val item = _uiState.value.cartState.items.firstOrNull { it.menuItemId == menuItemId } ?: return
        viewModelScope.launch {
            cartRepository.addItem(item.copy(quantity = 1))
        }
    }

    fun decrease(menuItemId: String) {
        viewModelScope.launch {
            cartRepository.decrementItem(menuItemId)
        }
    }

    fun clear() {
        viewModelScope.launch {
            cartRepository.clearCart()
        }
    }
}
