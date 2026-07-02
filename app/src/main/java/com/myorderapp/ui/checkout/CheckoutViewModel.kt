package com.myorderapp.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Address
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.repository.CartRepository
import com.myorderapp.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val cartState: CartState = CartState(),
    val contactName: String = "到店取餐",
    val contactPhone: String = "",
    val addressLine1: String = "本店",
    val addressLine2: String = "",
    val buyerNote: String = "",
    val orderSubmittedId: String? = null
)

class CheckoutViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.observeCart().collect { cart ->
                _uiState.value = _uiState.value.copy(cartState = cart)
            }
        }
    }

    fun onBuyerNoteChange(value: String) {
        _uiState.value = _uiState.value.copy(buyerNote = value)
    }

    fun onContactNameChange(value: String) {
        _uiState.value = _uiState.value.copy(contactName = value)
    }

    fun onContactPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(contactPhone = value)
    }

    fun onAddressLine1Change(value: String) {
        _uiState.value = _uiState.value.copy(addressLine1 = value)
    }

    fun onAddressLine2Change(value: String) {
        _uiState.value = _uiState.value.copy(addressLine2 = value)
    }

    fun submitOrder() {
        val state = _uiState.value
        if (state.cartState.isEmpty) return

        viewModelScope.launch {
            val orderId = orderRepository.submitOrder(
                cart = state.cartState,
                address = Address(
                    id = "pickup",
                    userId = "guest",
                    contactName = state.contactName.trim().ifBlank { "到店取餐" },
                    contactPhone = state.contactPhone.trim(),
                    addressLine1 = state.addressLine1.trim().ifBlank { "本店" },
                    addressLine2 = state.addressLine2.trim(),
                    tag = "收餐",
                    isDefault = true
                ),
                note = state.buyerNote
            )
            cartRepository.clearCart()
            _uiState.value = _uiState.value.copy(orderSubmittedId = orderId)
        }
    }
}
