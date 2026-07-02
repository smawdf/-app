package com.myorderapp.domain.repository

import com.myorderapp.domain.model.CartItem
import com.myorderapp.domain.model.CartMutationResult
import com.myorderapp.domain.model.CartState
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<CartState>
    suspend fun addItem(item: CartItem): CartMutationResult
    suspend fun decrementItem(menuItemId: String)
    suspend fun clearCart()
    suspend fun replaceCartWith(item: CartItem): CartMutationResult
}
