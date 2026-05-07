package com.myorderapp.domain.repository

import com.myorderapp.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun getWishlistItems(status: String? = null): Flow<List<WishlistItem>>
    suspend fun addToWishlist(dishId: String, dishName: String, category: String, addedBy: String, addedByName: String, imageUrl: String? = null): String
    suspend fun updateStatus(id: String, status: String)
    suspend fun removeFromWishlist(id: String)
}
