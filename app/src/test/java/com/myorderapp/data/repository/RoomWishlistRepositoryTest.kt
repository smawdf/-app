package com.myorderapp.data.repository

import com.myorderapp.data.local.dao.WishlistDao
import com.myorderapp.data.local.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomWishlistRepositoryTest {

    @Test
    fun `addToWishlist stores pending item in dao`() = runBlocking {
        val dao = FakeWishlistDao()
        val repository = RoomWishlistRepository(dao)

        val id = repository.addToWishlist(
            dishId = "dish-1",
            dishName = "Mapo tofu",
            category = "Sichuan",
            addedBy = "user-1",
            addedByName = "Ada",
            imageUrl = "https://example.com/mapo.jpg"
        )

        val items = repository.getWishlistItems("pending").first()
        assertEquals(id, items.single().id)
        assertEquals("dish-1", items.single().dishId)
        assertEquals("Mapo tofu", items.single().dishName)
        assertEquals("Sichuan", items.single().dishCategory)
        assertEquals("Ada", items.single().addedByName)
        assertEquals("pending", items.single().status)
        assertEquals("https://example.com/mapo.jpg", items.single().dishImageUrl)
    }

    @Test
    fun `status filter updates when item status changes`() = runBlocking {
        val dao = FakeWishlistDao()
        val repository = RoomWishlistRepository(dao)
        val id = repository.addToWishlist(
            dishId = "dish-2",
            dishName = "Tomato egg",
            category = "Home",
            addedBy = "user-1",
            addedByName = "Ada"
        )

        repository.updateStatus(id, "tried")

        assertTrue(repository.getWishlistItems("pending").first().isEmpty())
        assertEquals("tried", repository.getWishlistItems("tried").first().single().status)
    }

    private class FakeWishlistDao : WishlistDao {
        private val items = MutableStateFlow<List<WishlistEntity>>(emptyList())

        override fun getWishlistItems(status: String?): Flow<List<WishlistEntity>> =
            items.map { current ->
                if (status == null || status == "all") {
                    current
                } else {
                    current.filter { it.status == status }
                }
            }

        override suspend fun insert(item: WishlistEntity) {
            items.value = items.value.filterNot { it.id == item.id } + item
        }

        override suspend fun updateStatus(id: String, status: String) {
            items.value = items.value.map { item ->
                if (item.id == id) item.copy(status = status) else item
            }
        }

        override suspend fun deleteById(id: String) {
            items.value = items.value.filterNot { it.id == id }
        }
    }
}
