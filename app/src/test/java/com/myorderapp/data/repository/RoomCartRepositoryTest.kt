package com.myorderapp.data.repository

import com.myorderapp.data.local.dao.CartDao
import com.myorderapp.data.local.entity.CartItemEntity
import com.myorderapp.domain.model.CartItem
import com.myorderapp.domain.model.CartMutationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCartRepositoryTest {

    @Test
    fun `add item persists and aggregates quantity`() = runBlocking {
        val dao = FakeCartDao()
        val repository = RoomCartRepository(dao)
        val item = sampleItem()

        repository.addItem(item)
        repository.addItem(item)

        val state = repository.observeCart().first()
        assertEquals("shop-1", state.shopId)
        assertEquals(2, state.itemCount)
        assertEquals(1, state.items.size)
    }

    @Test
    fun `cross shop add returns conflict until cart is replaced`() = runBlocking {
        val dao = FakeCartDao()
        val repository = RoomCartRepository(dao)
        repository.addItem(sampleItem(shopId = "shop-1", shopName = "Shop 1"))

        val conflict = repository.addItem(sampleItem(shopId = "shop-2", shopName = "Shop 2"))
        assertTrue(conflict is CartMutationResult.ShopConflict)

        repository.replaceCartWith(sampleItem(shopId = "shop-2", shopName = "Shop 2"))
        val state = repository.observeCart().first()
        assertEquals("shop-2", state.shopId)
        assertEquals(1, state.itemCount)
    }

    private fun sampleItem(
        shopId: String = "shop-1",
        shopName: String = "Shop 1"
    ) = CartItem(
        id = "${shopId}_menu-1",
        shopId = shopId,
        shopName = shopName,
        shopCoverUrl = "",
        minOrderPrice = 10.0,
        deliveryFee = 2.0,
        menuItemId = "menu-1",
        menuItemName = "Menu 1",
        menuItemImageUrl = "",
        unitPrice = 12.0,
        quantity = 1
    )

    private class FakeCartDao : CartDao {
        private val items = MutableStateFlow<List<CartItemEntity>>(emptyList())

        override fun observeCartItems(): Flow<List<CartItemEntity>> = items

        override suspend fun upsert(item: CartItemEntity) {
            items.value = items.value.filterNot { it.id == item.id } + item
        }

        override suspend fun deleteByMenuItemId(menuItemId: String) {
            items.value = items.value.filterNot { it.menuItemId == menuItemId }
        }

        override suspend fun clearAll() {
            items.value = emptyList()
        }
    }
}
