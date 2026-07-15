package com.myorderapp.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Address
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.model.ROLE_EATER
import com.myorderapp.domain.repository.CartRepository
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class CheckoutUiState(
    val cartState: CartState = CartState(),
    val contactName: String = "到店取餐",
    val contactPhone: String = "",
    val addressLine1: String = "本店",
    val addressLine2: String = "",
    val buyerNote: String = "",
    val candyCoins: Int = 66,
    val isEater: Boolean = false,
    val orderSubmittedId: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class CheckoutViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.observeCart().collect { cart ->
                _uiState.value = _uiState.value.copy(cartState = cart)
            }
        }
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(isEater = profile?.selectedRole == ROLE_EATER)
            }
        }
        viewModelScope.launch {
            profileRepository.observeCandyWalletBalance().collect { balance ->
                _uiState.value = _uiState.value.copy(candyCoins = balance)
            }
        }
        viewModelScope.launch {
            while (true) {
                profileRepository.refreshCandyWalletBalance()
                delay(10_000)
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
        if (!state.isEater) {
            _uiState.value = state.copy(errorMessage = "只有吃货可以提交点菜，饲养员负责管理小店和接单")
            return
        }
        if (state.cartState.isEmpty) {
            _uiState.value = state.copy(errorMessage = "购物篮还是空的，先去点菜吧")
            return
        }
        val candyCost = candyCoinsCost(state.cartState.totalPrice)
        if (state.candyCoins < candyCost) {
            _uiState.value = state.copy(errorMessage = "糖糖币不够啦，找饲养员撒点糖再点菜")
            return
        }

        val contactName = state.contactName.trim().ifBlank { "到店取餐" }
        val addressLine1 = state.addressLine1.trim().ifBlank { "本店" }

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
                        tag = "小饭桌",
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
                val message = when (it.message) {
                    "NOT_ENOUGH_CANDY_COINS" -> "糖糖币不够啦，找饲养员撒点糖再点菜"
                    "EATER_ROLE_REQUIRED" -> "只有吃货可以提交点菜，饲养员负责管理小店和接单"
                    else -> "提交失败，请稍后再试"
                }
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = message
                )
            }
        }
    }
}

fun candyCoinsCost(totalPrice: Double): Int = ceil(totalPrice).toInt().coerceAtLeast(1)
