package com.myorderapp.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.data.remote.supabase.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: List<OrderRecord> = emptyList(),
    val isCaretaker: Boolean = false,
    val activePairId: String = "",
    val updatingOrderId: String? = null,
    val message: String? = null
)

class OrdersViewModel(
    private val orderRepository: OrderRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                runCatching { orderRepository.refreshOrders() }
                delay(ORDER_REFRESH_INTERVAL_MS)
            }
        }
        viewModelScope.launch {
            orderRepository.observeOrders().collect { orders ->
                _uiState.value = _uiState.value.copy(orders = orders)
            }
        }
        viewModelScope.launch {
            profileRepository.getProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    isCaretaker = profile?.selectedRole == ROLE_CARETAKER
                )
            }
        }
        viewModelScope.launch {
            sessionManager.pairId.collect { pairId ->
                _uiState.value = _uiState.value.copy(activePairId = pairId)
            }
        }
    }

    fun advanceOrder(order: OrderRecord) {
        val state = _uiState.value
        if (!state.isCaretaker || order.pairId != state.activePairId || state.updatingOrderId != null) return
        val nextStatus = when (order.status) {
            "submitted", "confirmed" -> "preparing"
            "preparing", "delivering" -> "completed"
            else -> return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingOrderId = order.id, message = null)
            val resultMessage = runCatching {
                orderRepository.updateOrderStatus(order.id, nextStatus)
                orderRepository.refreshOrders()
            }.fold(
                onSuccess = { if (nextStatus == "preparing") "已确认接单，开始准备" else "这顿饭已完成" },
                onFailure = { "订单状态更新失败，请稍后重试" }
            )
            _uiState.value = _uiState.value.copy(updatingOrderId = null)
            showMessage(resultMessage)
        }
    }

    private suspend fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
        delay(MESSAGE_DURATION_MS)
        if (_uiState.value.message == message) {
            _uiState.value = _uiState.value.copy(message = null)
        }
    }

    private companion object {
        const val ORDER_REFRESH_INTERVAL_MS = 5_000L
        const val MESSAGE_DURATION_MS = 1_800L
    }
}
