package com.myorderapp.data.repository

import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.data.local.entity.MenuDishDeletionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomMenuRepositoryTest {

    @Test
    fun `menu dishes are local single shop items with price and category`() = runBlocking {
        val dao = FakeMenuDishDao()
        val repository = RoomMenuRepository(dao)

        val dishId = repository.saveDish(
            MenuDishDraft(
                name = "招牌牛肉饭",
                price = 26.8,
                imageUrl = "content://dish/beef",
                category = "主食"
            )
        )

        val items = repository.getMenuItems("ignored-shop-id").first()

        assertEquals(1, items.size)
        assertEquals("single_shop", items.first().shopId)
        assertEquals(dishId, items.first().id)
        assertEquals("招牌牛肉饭", items.first().name)
        assertEquals(26.8, items.first().price, 0.001)
        assertEquals("主食", items.first().categoryId)
    }

    @Test
    fun `save existing dish updates it and delete removes it`() = runBlocking {
        val dao = FakeMenuDishDao()
        val repository = RoomMenuRepository(dao)

        val dishId = repository.saveDish(MenuDishDraft(name = "可乐", price = 6.0, category = "饮品"))
        repository.saveDish(MenuDishDraft(id = dishId, name = "冰可乐", price = 7.0, category = "饮品"))
        repository.deleteDish(dishId)

        assertNull(repository.getDish(dishId))
        assertEquals(emptyList<MenuDishEntity>(), dao.observeAll().first())
    }

    @Test
    fun `rename category updates existing dishes`() = runBlocking {
        val dao = FakeMenuDishDao()
        val repository = RoomMenuRepository(dao)

        val dishId = repository.saveDish(MenuDishDraft(name = "牛肉饭", price = 26.8, category = "主食"))
        repository.renameCategory(oldName = "主食", newName = "招牌")

        assertEquals("招牌", repository.getDish(dishId)?.category)
    }

    @Test
    fun `record sales increments monthly sales from submitted orders`() = runBlocking {
        val dao = FakeMenuDishDao()
        val repository = RoomMenuRepository(dao)

        val dishId = repository.saveDish(MenuDishDraft(name = "牛肉饭", price = 26.8, category = "主食"))
        repository.recordSales(mapOf(dishId to 3))

        assertEquals(3, repository.getDish(dishId)?.monthlySales)
    }

    @Test
    fun `batch delete records tombstones for a later cloud sync`() = runBlocking {
        val dao = FakeMenuDishDao()
        val repository = RoomMenuRepository(dao)
        val firstId = repository.saveDish(MenuDishDraft(name = "菜品一", price = 18.0, category = "主食"))
        val secondId = repository.saveDish(MenuDishDraft(name = "菜品二", price = 22.0, category = "主食"))

        repository.deleteDishes(listOf(firstId, secondId, firstId, ""))

        assertEquals(emptyList<MenuDishEntity>(), dao.observeAll().first())
        assertEquals(setOf(firstId, secondId), dao.getDeletions("").map { it.id }.toSet())
    }

    private class FakeMenuDishDao : MenuDishDao {
        private val items = MutableStateFlow<List<MenuDishEntity>>(emptyList())
        private val deletions = mutableListOf<MenuDishDeletionEntity>()

        override fun observeAll(): Flow<List<MenuDishEntity>> = items

        override fun observeByPair(pairId: String): Flow<List<MenuDishEntity>> =
            items.map { dishes -> dishes.filter { it.pairId == pairId } }

        override suspend fun getAllByPair(pairId: String): List<MenuDishEntity> =
            items.value.filter { it.pairId == pairId }

        override suspend fun getById(id: String, pairId: String): MenuDishEntity? =
            items.value.firstOrNull { it.id == id && it.pairId == pairId }

        override suspend fun upsert(dish: MenuDishEntity) {
            items.value = items.value.filterNot { it.id == dish.id } + dish
        }

        override suspend fun renameCategory(oldName: String, newName: String, updatedAt: String, pairId: String) {
            items.value = items.value.map { dish ->
                if (dish.category == oldName && dish.pairId == pairId) dish.copy(category = newName, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun setAvailability(id: String, isAvailable: Boolean, updatedAt: String, pairId: String) {
            items.value = items.value.map { dish ->
                if (dish.id == id && dish.pairId == pairId) dish.copy(isAvailable = isAvailable, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun setAvailability(ids: List<String>, isAvailable: Boolean, updatedAt: String, pairId: String) {
            items.value = items.value.map { dish ->
                if (dish.id in ids && dish.pairId == pairId) dish.copy(isAvailable = isAvailable, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun moveToCategory(ids: List<String>, category: String, updatedAt: String, pairId: String) {
            items.value = items.value.map { dish ->
                if (dish.id in ids && dish.pairId == pairId) dish.copy(category = category, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: String, pairId: String) {
            items.value = items.value.map { dish ->
                if (dish.id == id && dish.pairId == pairId) dish.copy(sortOrder = sortOrder, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun incrementMonthlySales(id: String, quantity: Int, updatedAt: String, pairId: String) {
            items.value = items.value.map { dish ->
                if (dish.id == id && dish.pairId == pairId) dish.copy(monthlySales = dish.monthlySales + quantity, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun updateImage(id: String, imageUrl: String, updatedAt: String, pairId: String) {
            items.value = items.value.map { dish ->
                if (dish.id == id && dish.pairId == pairId) dish.copy(imageUrl = imageUrl, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun markDeleted(deletion: MenuDishDeletionEntity) {
            deletions.removeAll { it.id == deletion.id }
            deletions += deletion
        }

        override suspend fun getDeletions(pairId: String): List<MenuDishDeletionEntity> {
            return deletions.filter { it.pairId == pairId }
        }

        override suspend fun clearDeletion(id: String, pairId: String) {
            deletions.removeAll { it.id == id && it.pairId == pairId }
        }

        override suspend fun deleteById(id: String, pairId: String) {
            items.value = items.value.filterNot { it.id == id && it.pairId == pairId }
        }

        override suspend fun deleteByIds(ids: List<String>, pairId: String) {
            items.value = items.value.filterNot { it.id in ids && it.pairId == pairId }
        }
    }
}
