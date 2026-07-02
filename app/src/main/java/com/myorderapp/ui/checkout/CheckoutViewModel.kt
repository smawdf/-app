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
    val orderSubmittedId: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
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
        _uiState.value = _uiState.value.copy(buyerNote = value, errorMessage = null)
    }

    fun onContactNameChange(value: String) {
        _uiState.value = _uiState.value.copy(contactName = value, errorMessage = null)
    }

    fun onContactPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(contactPhone = value, errorMessage = null)
    }

    fun onAddressLine1Change(value: String) {
        _uiState.value = _uiState.value.copy(addressLine1 = value, errorMessage = null)
    }

    fun onAddressLine2Change(value: String) {
        _uiState.value = _uiState.value.copy(addressLine2 = value, errorMessage = null)
    }

    fun submitOrder() {
        val state = _uiState.value
        if (state.isSubmitting) return
        if (state.cartState.isEmpty) {
            _uiState.value = state.copy(errorMessage = "购物车为空，先去点菜吧")
            return
        }

        val contactName = state.contactName.trim()
        val addressLine1 = state.addressLine1.trim()
        if (contactName.isBlank() || addressLine1.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请填写联系人和收餐地址")
            return
        }

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            runCatching {
                orderRepository.submitOrder(
                    cart = state.cartState,
                    address = Address(
                        id = "pickup",
                        userId = "guest",
                        contactName = contactName,
                        contactPhone = state.contactPhone.trim(),
                        addressLine1 = addressLine1,
                        addressLine2 = state.addressLine2.trim(),
                        tag = "收餐",
                        isDefault = true
                    ),
                    note = state.buyerNote.trim()
                )
            }.onSuccess { orderId ->
                cartRepository.clearCart()
                _uiState.value = _uiState.value.copy(
                    orderSubmittedId = orderId,
                    isSubmitting = false,
                    errorMessage = null
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "提交失败，请稍后再试"
                )
            }
        }
    }
}
