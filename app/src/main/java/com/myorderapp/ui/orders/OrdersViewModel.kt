package com.myorderapp.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: List<OrderRecord> = emptyList()
)

class OrdersViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.refreshOrders()
        }
        viewModelScope.launch {
            orderRepository.observeOrders().collect { orders ->
                _uiState.value = OrdersUiState(orders = orders)
            }
        }
    }
}
