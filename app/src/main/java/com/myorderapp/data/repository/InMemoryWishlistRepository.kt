package com.myorderapp.data.repository

import com.myorderapp.domain.model.WishlistItem
import com.myorderapp.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryWishlistRepository : WishlistRepository {

    private val _items = MutableStateFlow<List<WishlistItem>>(emptyList())

    override fun getWishlistItems(status: String?): Flow<List<WishlistItem>> =
        _items.map { list ->
            if (status == null || status == "all") list
            else list.filter { it.status == status }
        }

    override suspend fun addToWishlist(
        dishId: String, dishName: String, category: String,
        addedBy: String, addedByName: String, imageUrl: String?
    ): String {
        val id = "wish_${System.currentTimeMillis()}"
        val item = WishlistItem(
            id = id,
            dishId = dishId,
            dishName = dishName,
            dishCategory = category,
            addedBy = addedBy,
            addedByName = addedByName,
            dishImageUrl = imageUrl,
            status = "pending"
        )
        _items.value = _items.value + item
        return id
    }

    override suspend fun updateStatus(id: String, status: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    override suspend fun removeFromWishlist(id: String) {
        _items.value = _items.value.filter { it.id != id }
    }

}
