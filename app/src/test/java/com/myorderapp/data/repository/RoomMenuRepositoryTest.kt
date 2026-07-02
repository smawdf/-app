package com.myorderapp.data.repository

import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.entity.MenuDishEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

    private class FakeMenuDishDao : MenuDishDao {
        private val items = MutableStateFlow<List<MenuDishEntity>>(emptyList())

        override fun observeAll(): Flow<List<MenuDishEntity>> = items

        override suspend fun getById(id: String): MenuDishEntity? =
            items.value.firstOrNull { it.id == id }

        override suspend fun upsert(dish: MenuDishEntity) {
            items.value = items.value.filterNot { it.id == dish.id } + dish
        }

        override suspend fun renameCategory(oldName: String, newName: String, updatedAt: String) {
            items.value = items.value.map { dish ->
                if (dish.category == oldName) dish.copy(category = newName, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun setAvailability(id: String, isAvailable: Boolean, updatedAt: String) {
            items.value = items.value.map { dish ->
                if (dish.id == id) dish.copy(isAvailable = isAvailable, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun setAvailability(ids: List<String>, isAvailable: Boolean, updatedAt: String) {
            items.value = items.value.map { dish ->
                if (dish.id in ids) dish.copy(isAvailable = isAvailable, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun moveToCategory(ids: List<String>, category: String, updatedAt: String) {
            items.value = items.value.map { dish ->
                if (dish.id in ids) dish.copy(category = category, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: String) {
            items.value = items.value.map { dish ->
                if (dish.id == id) dish.copy(sortOrder = sortOrder, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun incrementMonthlySales(id: String, quantity: Int, updatedAt: String) {
            items.value = items.value.map { dish ->
                if (dish.id == id) dish.copy(monthlySales = dish.monthlySales + quantity, updatedAt = updatedAt) else dish
            }
        }

        override suspend fun deleteById(id: String) {
            items.value = items.value.filterNot { it.id == id }
        }

        override suspend fun deleteByIds(ids: List<String>) {
            items.value = items.value.filterNot { it.id in ids }
        }
    }
}
