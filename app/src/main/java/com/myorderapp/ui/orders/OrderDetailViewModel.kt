package com.myorderapp.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.data.remote.supabase.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val order: OrderRecord? = null,
    val isCaretaker: Boolean = false,
    val activePairId: String = "",
    val message: String? = null
)

class OrderDetailViewModel(
    private val orderRepository: OrderRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState = _uiState.asStateFlow()
    private var orderRefreshJob: Job? = null

    init {
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

    fun load(orderId: String) {
        orderRefreshJob?.cancel()
        orderRefreshJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(order = orderRepository.getOrderById(orderId))
            while (isActive) {
                runCatching {
                    orderRepository.refreshOrders()
                    orderRepository.getOrderById(orderId)
                }.onSuccess { updatedOrder ->
                    if (updatedOrder != null) {
                        _uiState.value = _uiState.value.copy(order = updatedOrder)
                    }
                }
                delay(ORDER_REFRESH_INTERVAL_MS)
            }
        }
    }

    fun advanceStatus() {
        if (!_uiState.value.isCaretaker || _uiState.value.order?.pairId != _uiState.value.activePairId) {
            _uiState.value = _uiState.value.copy(message = "只有饲养员可以更新订单进度")
            return
        }
        val order = _uiState.value.order ?: return
        val nextStatus = order.status.nextOrderStatus() ?: return
        viewModelScope.launch {
            runCatching {
                orderRepository.updateOrderStatus(order.id, nextStatus)
                orderRepository.getOrderById(order.id)
            }.onSuccess { updatedOrder ->
                _uiState.value = _uiState.value.copy(
                    order = updatedOrder,
                    message = nextStatus.toStatusChangeMessage()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(message = "订单进度更新失败，请稍后重试")
            }
        }
    }

    fun cancelOrder() {
        val order = _uiState.value.order ?: return
        if (order.pairId != _uiState.value.activePairId) return
        if (order.status in setOf("completed", "cancelled")) return
        viewModelScope.launch {
            runCatching {
                orderRepository.updateOrderStatus(order.id, "cancelled")
                orderRepository.getOrderById(order.id)
            }.onSuccess { updatedOrder ->
                _uiState.value = _uiState.value.copy(
                    order = updatedOrder,
                    message = "订单已取消"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(message = "取消订单失败，请稍后重试")
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun String.nextOrderStatus(): String? = when (this) {
        "submitted", "confirmed" -> "preparing"
        "preparing", "delivering" -> "completed"
        else -> null
    }

    private fun String.toStatusChangeMessage(): String = when (this) {
        "preparing" -> "饲养员已接单，开始准备今天的饭"
        "completed" -> "这顿饭已完成"
        else -> "订单状态已更新"
    }

    private companion object {
        const val ORDER_REFRESH_INTERVAL_MS = 5_000L
    }
}
