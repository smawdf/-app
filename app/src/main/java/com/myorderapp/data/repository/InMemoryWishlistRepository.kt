package com.myorderapp.data.repository

import com.myorderapp.domain.model.WishlistItem
import com.myorderapp.domain.repository.WishlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryWishlistRepository : WishlistRepository {

    private val _items = MutableStateFlow(sampleWishlist)

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

    companion object {
        val sampleWishlist = listOf(
            WishlistItem(
                id = "w1", dishId = "s1", dishName = "红烧肉",
                dishCategory = "中餐", externalSource = "Spoonacular",
                addedBy = "u1", addedByName = "你", status = "pending"
            ),
            WishlistItem(
                id = "w2", dishId = "s2", dishName = "凯撒沙拉",
                dishCategory = "西餐", externalSource = "Spoonacular",
                addedBy = "u2", addedByName = "她", status = "pending"
            ),
            WishlistItem(
                id = "w3", dishId = "3", dishName = "芒果糯米饭",
                dishCategory = "甜品", externalSource = "Spoonacular",
                addedBy = "u1", addedByName = "你", status = "tried"
            ),
            WishlistItem(
                id = "w4", dishId = "4", dishName = "焦糖布丁",
                dishCategory = "甜品",
                addedBy = "u2", addedByName = "她", status = "pending"
            ),
            WishlistItem(
                id = "w5", dishId = "5", dishName = "法式焗蜗牛",
                dishCategory = "西餐",
                addedBy = "u1", addedByName = "你", status = "rejected"
            )
        )
    }
}
