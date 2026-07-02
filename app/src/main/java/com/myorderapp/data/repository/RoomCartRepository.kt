package com.myorderapp.data.repository

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.CartDao
import com.myorderapp.domain.model.CartItem
import com.myorderapp.domain.model.CartMutationResult
import com.myorderapp.domain.model.CartState
import com.myorderapp.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

class RoomCartRepository(
    private val cartDao: CartDao
) : CartRepository {

    override fun observeCart(): Flow<CartState> {
        return cartDao.observeCartItems().map { entities ->
            val items = entities.map { it.toDomain() }
            val first = items.firstOrNull()
            CartState(
                shopId = first?.shopId,
                shopName = first?.shopName.orEmpty(),
                shopCoverUrl = first?.shopCoverUrl.orEmpty(),
                minOrderPrice = first?.minOrderPrice ?: 0.0,
                deliveryFee = first?.deliveryFee ?: 0.0,
                items = items
            )
        }
    }

    override suspend fun addItem(item: CartItem): CartMutationResult {
        val current = observeCart().first()
        if (!current.isEmpty && current.shopId != item.shopId) {
            return CartMutationResult.ShopConflict(
                currentShopId = current.shopId.orEmpty(),
                currentShopName = current.shopName,
                incomingShopId = item.shopId,
                incomingShopName = item.shopName
            )
        }

        val existing = current.items.firstOrNull { it.menuItemId == item.menuItemId }
        val merged = if (existing != null) {
            existing.copy(quantity = existing.quantity + 1)
        } else {
            item.copy(
                id = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id,
                addedAt = item.addedAt.ifBlank { Instant.now().toString() }
            )
        }
        cartDao.upsert(merged.toEntity())
        return CartMutationResult.Success
    }

    override suspend fun decrementItem(menuItemId: String) {
        val current = observeCart().first()
        val existing = current.items.firstOrNull { it.menuItemId == menuItemId } ?: return
        if (existing.quantity <= 1) {
            cartDao.deleteByMenuItemId(menuItemId)
        } else {
            cartDao.upsert(existing.copy(quantity = existing.quantity - 1).toEntity())
        }
    }

    override suspend fun clearCart() {
        cartDao.clearAll()
    }

    override suspend fun replaceCartWith(item: CartItem): CartMutationResult {
        clearCart()
        return addItem(item)
    }
}
