package com.myorderapp.data.repository

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.WishlistDao
import com.myorderapp.domain.model.WishlistItem
import com.myorderapp.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWishlistRepository(
    private val wishlistDao: WishlistDao
) : WishlistRepository {

    override fun getWishlistItems(status: String?): Flow<List<WishlistItem>> =
        wishlistDao.getWishlistItems(status).map { items -> items.map { it.toDomain() } }

    override suspend fun addToWishlist(
        dishId: String,
        dishName: String,
        category: String,
        addedBy: String,
        addedByName: String,
        imageUrl: String?
    ): String {
        val now = System.currentTimeMillis()
        val id = "wish_$now"
        val item = WishlistItem(
            id = id,
            dishId = dishId,
            dishName = dishName,
            dishCategory = category,
            dishImageUrl = imageUrl,
            addedBy = addedBy,
            addedByName = addedByName,
            status = "pending",
            createdAt = now.toString()
        )
        wishlistDao.insert(item.toEntity())
        return id
    }

    override suspend fun updateStatus(id: String, status: String) {
        wishlistDao.updateStatus(id, status)
    }

    override suspend fun removeFromWishlist(id: String) {
        wishlistDao.deleteById(id)
    }
}
