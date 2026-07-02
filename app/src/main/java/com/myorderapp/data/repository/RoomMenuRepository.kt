package com.myorderapp.data.repository

import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.ui.search.SearchableMenuItem
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val SINGLE_SHOP_ID = "single_shop"

data class MenuDishDraft(
    val id: String? = null,
    val name: String,
    val price: Double,
    val originPrice: Double? = null,
    val imageUrl: String = "",
    val category: String,
    val description: String = "",
    val stock: Int = 32,
    val isAvailable: Boolean = true,
    val isSignature: Boolean = false
)

class RoomMenuRepository(
    private val menuDishDao: MenuDishDao
) : MenuRepository {
    override fun getMenuCategories(shopId: String): Flow<List<MenuCategory>> {
        return menuDishDao.observeAll().map { dishes ->
            dishes.map { it.category.ifBlank { "其他" } }
                .distinct()
                .mapIndexed { index, category ->
                    MenuCategory(
                        id = category,
                        shopId = SINGLE_SHOP_ID,
                        name = category,
                        sortOrder = index
                    )
                }
        }
    }

    override fun getMenuItems(shopId: String): Flow<List<MenuItem>> {
        return menuDishDao.observeAll().map { dishes -> dishes.map { it.toMenuItem() } }
    }

    override fun searchMenuItems(query: String): Flow<List<SearchableMenuItem>> {
        return getMenuItems(SINGLE_SHOP_ID).map { items ->
            items
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                .map {
                    SearchableMenuItem(
                        shopId = SINGLE_SHOP_ID,
                        shopName = "本店",
                        menuItem = it
                    )
                }
        }
    }

    fun observeMenuDishes(): Flow<List<MenuDishEntity>> = menuDishDao.observeAll()

    suspend fun getDish(id: String): MenuDishEntity? = menuDishDao.getById(id)

    suspend fun saveDish(draft: MenuDishDraft): String {
        val id = draft.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val existing = menuDishDao.getById(id)
        val entity = MenuDishEntity(
            id = id,
            name = draft.name.trim(),
            price = draft.price,
            originPrice = draft.originPrice,
            imageUrl = draft.imageUrl.trim(),
            category = draft.category.trim().ifBlank { "其他" },
            description = draft.description.trim(),
            sortOrder = existing?.sortOrder ?: 0,
            monthlySales = existing?.monthlySales ?: 0,
            stock = draft.stock,
            isAvailable = draft.isAvailable,
            isSignature = draft.isSignature,
            updatedAt = Instant.now().toString()
        )
        menuDishDao.upsert(entity)
        return id
    }

    suspend fun deleteDish(id: String) {
        menuDishDao.deleteById(id)
    }

    suspend fun setAvailability(id: String, isAvailable: Boolean) {
        menuDishDao.setAvailability(id, isAvailable, Instant.now().toString())
    }

    suspend fun setAvailability(ids: List<String>, isAvailable: Boolean) {
        if (ids.isEmpty()) return
        menuDishDao.setAvailability(ids, isAvailable, Instant.now().toString())
    }

    suspend fun moveToCategory(ids: List<String>, category: String) {
        val normalizedCategory = category.trim()
        if (ids.isEmpty() || normalizedCategory.isBlank()) return
        menuDishDao.moveToCategory(ids, normalizedCategory, Instant.now().toString())
    }

    suspend fun deleteDishes(ids: List<String>) {
        if (ids.isEmpty()) return
        menuDishDao.deleteByIds(ids)
    }

    suspend fun saveSortOrder(ids: List<String>) {
        val now = Instant.now().toString()
        ids.forEachIndexed { index, id ->
            menuDishDao.updateSortOrder(id, index, now)
        }
    }

    suspend fun recordSales(items: Map<String, Int>) {
        val now = Instant.now().toString()
        items.forEach { (id, quantity) ->
            if (id.isNotBlank() && quantity > 0) {
                menuDishDao.incrementMonthlySales(id, quantity, now)
            }
        }
    }

    suspend fun renameCategory(oldName: String, newName: String) {
        val normalizedOldName = oldName.trim()
        val normalizedNewName = newName.trim()
        if (normalizedOldName.isBlank() || normalizedNewName.isBlank() || normalizedOldName == normalizedNewName) return

        menuDishDao.renameCategory(
            oldName = normalizedOldName,
            newName = normalizedNewName,
            updatedAt = Instant.now().toString()
        )
    }

    private fun MenuDishEntity.toMenuItem(): MenuItem = MenuItem(
        id = id,
        shopId = SINGLE_SHOP_ID,
        categoryId = category.ifBlank { "其他" },
        name = name,
        subtitle = category.ifBlank { "其他" },
        description = description.ifBlank { "本店自制菜品" },
        imageUrl = imageUrl,
        price = price,
        originPrice = originPrice,
        monthlySales = monthlySales,
        tags = listOfNotNull(
            "在售".takeIf { isAvailable },
            "招牌".takeIf { isSignature }
        )
    )
}
