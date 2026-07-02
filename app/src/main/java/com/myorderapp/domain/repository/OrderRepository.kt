package com.myorderapp.domain.repository

import com.myorderapp.domain.model.Address
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.model.OrderRecord
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(): Flow<List<OrderRecord>>
    suspend fun getOrderById(orderId: String): OrderRecord?
    suspend fun submitOrder(cart: CartState, address: Address, note: String): String
}
