package com.myorderapp.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val order: OrderRecord? = null,
    val message: String? = null
)

class OrderDetailViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(orderId: String) {
        viewModelScope.launch {
            _uiState.value = OrderDetailUiState(order = orderRepository.getOrderById(orderId))
        }
    }

    fun advanceStatus(canAdvance: Boolean = true) {
        if (!canAdvance) {
            _uiState.value = _uiState.value.copy(message = "只有饲养员可以更新订单进度")
            return
        }
        val order = _uiState.value.order ?: return
        val nextStatus = order.status.nextOrderStatus() ?: return
        viewModelScope.launch {
            orderRepository.updateOrderStatus(order.id, nextStatus)
            _uiState.value = OrderDetailUiState(
                order = orderRepository.getOrderById(order.id),
                message = nextStatus.toStatusChangeMessage()
            )
        }
    }

    fun cancelOrder() {
        val order = _uiState.value.order ?: return
        if (order.status in setOf("completed", "cancelled")) return
        viewModelScope.launch {
            orderRepository.updateOrderStatus(order.id, "cancelled")
            _uiState.value = OrderDetailUiState(
                order = orderRepository.getOrderById(order.id),
                message = "订单已取消"
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun String.nextOrderStatus(): String? = when (this) {
        "submitted" -> "confirmed"
        "confirmed" -> "delivering"
        "delivering" -> "completed"
        else -> null
    }

    private fun String.toStatusChangeMessage(): String = when (this) {
        "confirmed" -> "饲养员已接单"
        "delivering" -> "开始准备今天的饭"
        "completed" -> "这顿饭已完成"
        else -> "订单状态已更新"
    }
}
