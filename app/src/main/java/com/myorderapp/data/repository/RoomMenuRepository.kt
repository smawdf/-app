package com.myorderapp.data.repository

import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.ui.search.SearchableMenuItem
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    private val menuDishDao: MenuDishDao,
    private val session: SessionManager? = null,
    private val cloudErrorLogger: CloudErrorLogger? = null
) : MenuRepository {
    private val client by lazy { SupabaseClientProvider.client }

    override fun getMenuCategories(shopId: String): Flow<List<MenuCategory>> {
        return observeLocalDishes().map { dishes ->
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
        return observeLocalDishes().map { dishes -> dishes.map { it.toMenuItem() } }
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

    fun observeMenuDishes(): Flow<List<MenuDishEntity>> = observeLocalDishes()

    private fun observeLocalDishes(): Flow<List<MenuDishEntity>> = menuDishDao.observeByPair(localPairId())

    suspend fun getDish(id: String): MenuDishEntity? = menuDishDao.getById(id)

    suspend fun loadFromCloud() {
        val pairId = activePairId() ?: return
        try {
            client.from("menu_dishes").select {
                filter { eq("pair_id", pairId) }
            }.decodeList<RemoteMenuDish>().forEach { remote ->
                menuDishDao.upsert(remote.toEntity())
            }
            menuDishDao.getAllByPair(pairId).forEach { local ->
                client.from("menu_dishes").upsert(local.toRemote(pairId)) { select() }
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("menu", "load_dishes", e, "pairId=$pairId")
        }
    }

    suspend fun saveDish(draft: MenuDishDraft): String {
        val id = draft.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val existing = menuDishDao.getById(id)
        val entity = MenuDishEntity(
            id = id,
            pairId = localPairId(),
            name = draft.name.trim(),
            price = draft.price,
            originPrice = draft.originPrice,
            imageUrl = draft.imageUrl.takeIfCloudImageUrl().orEmpty(),
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
        syncMenuDishToCloud(entity)
        return id
    }

    suspend fun deleteDish(id: String) {
        menuDishDao.deleteById(id)
        deleteMenuDishFromCloud(id)
    }

    suspend fun setAvailability(id: String, isAvailable: Boolean) {
        menuDishDao.setAvailability(id, isAvailable, Instant.now().toString())
        menuDishDao.getById(id)?.let { syncMenuDishToCloud(it) }
    }

    suspend fun setAvailability(ids: List<String>, isAvailable: Boolean) {
        if (ids.isEmpty()) return
        menuDishDao.setAvailability(ids, isAvailable, Instant.now().toString())
        ids.forEach { id -> menuDishDao.getById(id)?.let { syncMenuDishToCloud(it) } }
    }

    suspend fun moveToCategory(ids: List<String>, category: String) {
        val normalizedCategory = category.trim()
        if (ids.isEmpty() || normalizedCategory.isBlank()) return
        menuDishDao.moveToCategory(ids, normalizedCategory, Instant.now().toString())
        ids.forEach { id -> menuDishDao.getById(id)?.let { syncMenuDishToCloud(it) } }
    }

    suspend fun deleteDishes(ids: List<String>) {
        if (ids.isEmpty()) return
        menuDishDao.deleteByIds(ids)
        ids.forEach { deleteMenuDishFromCloud(it) }
    }

    suspend fun saveSortOrder(ids: List<String>) {
        val now = Instant.now().toString()
        ids.forEachIndexed { index, id ->
            menuDishDao.updateSortOrder(id, index, now)
            menuDishDao.getById(id)?.let { syncMenuDishToCloud(it) }
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

    private fun activePairId(): String? {
        val currentSession = session ?: return null
        return currentSession.currentPairId
            .takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            ?: currentSession.currentUserId.takeIf { it.isNotBlank() }?.let { "user:$it" }
    }

    private fun localPairId(): String = activePairId().orEmpty()

    private suspend fun syncMenuDishToCloud(entity: MenuDishEntity) {
        val pairId = activePairId() ?: return
        try {
            client.from("menu_dishes").upsert(entity.toRemote(pairId)) { select() }
        } catch (e: Exception) {
            cloudErrorLogger?.log("menu", "sync_dish", e, "pairId=$pairId dishId=${entity.id}")
        }
    }

    private suspend fun deleteMenuDishFromCloud(id: String) {
        val pairId = activePairId() ?: return
        try {
            client.from("menu_dishes").delete {
                filter {
                    eq("id", id)
                    eq("pair_id", pairId)
                }
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("menu", "delete_dish", e, "pairId=$pairId dishId=$id")
        }
    }

    private fun MenuDishEntity.toMenuItem(): MenuItem = MenuItem(
        id = id,
        shopId = SINGLE_SHOP_ID,
        categoryId = category.ifBlank { "其他" },
        name = name,
        subtitle = category.ifBlank { "其他" },
        description = description.ifBlank { "本店自制菜品" },
        imageUrl = imageUrl.takeIfCloudImageUrl().orEmpty(),
        price = price,
        originPrice = originPrice,
        monthlySales = monthlySales,
        tags = listOfNotNull(
            "在售".takeIf { isAvailable },
            "招牌".takeIf { isSignature }
        )
    )
}

private const val DEFAULT_PAIR_ID = "00000000-0000-0000-0000-000000000000"

@Serializable
private data class RemoteMenuDish(
    val id: String,
    @SerialName("pair_id") val pairId: String,
    val name: String,
    val price: Double,
    @SerialName("origin_price") val originPrice: Double? = null,
    @SerialName("image_url") val imageUrl: String,
    val category: String,
    val description: String,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("monthly_sales") val monthlySales: Int = 0,
    val stock: Int = 0,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("is_signature") val isSignature: Boolean = false,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toEntity() = MenuDishEntity(
        id = id,
        pairId = pairId,
        name = name,
        price = price,
        originPrice = originPrice,
        imageUrl = imageUrl,
        category = category,
        description = description,
        sortOrder = sortOrder,
        monthlySales = monthlySales,
        stock = stock,
        isAvailable = isAvailable,
        isSignature = isSignature,
        updatedAt = updatedAt
    )
}

private fun MenuDishEntity.toRemote(pairId: String) = RemoteMenuDish(
    id = id,
    pairId = pairId,
    name = name,
    price = price,
    originPrice = originPrice,
    imageUrl = imageUrl.takeIfCloudImageUrl().orEmpty(),
    category = category,
    description = description,
    sortOrder = sortOrder,
    monthlySales = monthlySales,
    stock = stock,
    isAvailable = isAvailable,
    isSignature = isSignature,
    updatedAt = updatedAt
)

private fun String.takeIfCloudImageUrl(): String? {
    return trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
}
