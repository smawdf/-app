package com.myorderapp.data.repository

import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.data.local.entity.MenuDishDeletionEntity
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

    suspend fun getDish(id: String): MenuDishEntity? = menuDishDao.getById(id, localPairId())

    suspend fun loadFromCloud() {
        val pairId = activePairId() ?: return
        try {
            val deletions = menuDishDao.getDeletions(pairId)
            deletions.forEach { deletion -> syncDeletionToCloud(deletion) }
            val remoteById = client.from("menu_dishes").select {
                filter { eq("pair_id", pairId) }
            }.decodeList<RemoteMenuDish>().associateBy { it.id }
            remoteById.values
                .filter { it.deletedAt != null }
                .forEach { remote ->
                    menuDishDao.deleteById(remote.id, pairId)
                    menuDishDao.clearDeletion(remote.id, pairId)
                }
            val activeRemoteById = remoteById.filterValues { it.deletedAt == null }
            val localById = menuDishDao.getAllByPair(pairId).associateBy { it.id }
            (activeRemoteById.keys + localById.keys).forEach { id ->
                val remote = activeRemoteById[id]
                val local = localById[id]
                when {
                    remote == null && local != null -> client.from("menu_dishes").upsert(local.toRemote(pairId)) { select() }
                    local == null && remote != null -> menuDishDao.upsert(remote.toEntity())
                    remote != null && local != null && remote.updatedAt.isCloudTimestampAfter(local.updatedAt) -> menuDishDao.upsert(remote.toEntity())
                    remote != null && local != null && local.updatedAt.isCloudTimestampAfter(remote.updatedAt) ->
                        client.from("menu_dishes").upsert(local.toRemote(pairId)) { select() }
                }
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("menu", "load_dishes", e, "pairId=$pairId")
        }
    }

    suspend fun saveDish(draft: MenuDishDraft): String {
        val id = draft.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val pairId = localPairId()
        val existing = menuDishDao.getById(id, pairId)
        val entity = MenuDishEntity(
            id = id,
            pairId = pairId,
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
        val pairId = localPairId()
        if (menuDishDao.getById(id, pairId) == null) return
        val deletedAt = Instant.now().toString()
        menuDishDao.markDeleted(MenuDishDeletionEntity(id, pairId, deletedAt))
        menuDishDao.deleteById(id, pairId)
        deleteMenuDishFromCloud(id, deletedAt)
    }

    suspend fun setAvailability(id: String, isAvailable: Boolean) {
        val pairId = localPairId()
        menuDishDao.setAvailability(id, isAvailable, Instant.now().toString(), pairId)
        menuDishDao.getById(id, pairId)?.let { syncMenuDishToCloud(it) }
    }

    suspend fun updateDishImage(id: String, imageUrl: String) {
        val pairId = localPairId()
        menuDishDao.updateImage(id, imageUrl.takeIfCloudImageUrl().orEmpty(), Instant.now().toString(), pairId)
        menuDishDao.getById(id, pairId)?.let { syncMenuDishToCloud(it) }
    }

    suspend fun setAvailability(ids: List<String>, isAvailable: Boolean) {
        if (ids.isEmpty()) return
        val pairId = localPairId()
        menuDishDao.setAvailability(ids, isAvailable, Instant.now().toString(), pairId)
        ids.forEach { id -> menuDishDao.getById(id, pairId)?.let { syncMenuDishToCloud(it) } }
    }

    suspend fun moveToCategory(ids: List<String>, category: String) {
        val normalizedCategory = category.trim()
        if (ids.isEmpty() || normalizedCategory.isBlank()) return
        val pairId = localPairId()
        menuDishDao.moveToCategory(ids, normalizedCategory, Instant.now().toString(), pairId)
        ids.forEach { id -> menuDishDao.getById(id, pairId)?.let { syncMenuDishToCloud(it) } }
    }

    suspend fun deleteDishes(ids: List<String>) {
        val uniqueIds = ids.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        if (uniqueIds.isEmpty()) return

        val pairId = localPairId()
        val scopedIds = menuDishDao.getAllByPair(pairId)
            .asSequence()
            .map { it.id }
            .filter { it in uniqueIds }
            .toList()
        if (scopedIds.isEmpty()) return
        val deletedAt = Instant.now().toString()
        scopedIds.forEach { id ->
            menuDishDao.markDeleted(MenuDishDeletionEntity(id, pairId, deletedAt))
        }
        menuDishDao.deleteByIds(scopedIds, pairId)
        scopedIds.forEach { deleteMenuDishFromCloud(it, deletedAt) }
    }

    suspend fun saveSortOrder(ids: List<String>) {
        val pairId = localPairId()
        val now = Instant.now().toString()
        ids.forEachIndexed { index, id ->
            menuDishDao.updateSortOrder(id, index, now, pairId)
            menuDishDao.getById(id, pairId)?.let { syncMenuDishToCloud(it) }
        }
    }

    suspend fun recordSales(items: Map<String, Int>) {
        val pairId = localPairId()
        val now = Instant.now().toString()
        items.forEach { (id, quantity) ->
            if (id.isNotBlank() && quantity > 0) {
                menuDishDao.incrementMonthlySales(id, quantity, now, pairId)
                menuDishDao.getById(id, pairId)?.let { syncMenuDishToCloud(it) }
            }
        }
    }

    suspend fun renameCategory(oldName: String, newName: String) {
        val normalizedOldName = oldName.trim()
        val normalizedNewName = newName.trim()
        if (normalizedOldName.isBlank() || normalizedNewName.isBlank() || normalizedOldName == normalizedNewName) return

        val pairId = localPairId()
        menuDishDao.renameCategory(
            oldName = normalizedOldName,
            newName = normalizedNewName,
            updatedAt = Instant.now().toString(),
            pairId = pairId
        )
        menuDishDao.getAllByPair(pairId)
            .filter { it.category == normalizedNewName }
            .forEach { syncMenuDishToCloud(it) }
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
            menuDishDao.clearDeletion(entity.id, pairId)
        } catch (e: Exception) {
            cloudErrorLogger?.log("menu", "sync_dish", e, "pairId=$pairId dishId=${entity.id}")
        }
    }

    private suspend fun deleteMenuDishFromCloud(id: String, deletedAt: String) {
        val pairId = activePairId() ?: return
        try {
            syncDeletionToCloud(MenuDishDeletionEntity(id, pairId, deletedAt))
        } catch (e: Exception) {
            cloudErrorLogger?.log("menu", "delete_dish", e, "pairId=$pairId dishId=$id")
        }
    }

    private suspend fun syncDeletionToCloud(deletion: MenuDishDeletionEntity) {
        client.from("menu_dishes").update(
            MenuDishDeletionUpdate(
                deletedAt = deletion.deletedAt,
                updatedAt = deletion.deletedAt
            )
        ) {
            filter {
                eq("id", deletion.id)
                eq("pair_id", deletion.pairId)
            }
        }
        menuDishDao.clearDeletion(deletion.id, deletion.pairId)
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
        tags = listOfNotNull("在售".takeIf { isAvailable })
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
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
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
    updatedAt = updatedAt,
    deletedAt = null
)

@Serializable
private data class MenuDishDeletionUpdate(
    @SerialName("deleted_at") val deletedAt: String,
    @SerialName("updated_at") val updatedAt: String
)

private fun String.takeIfCloudImageUrl(): String? {
    return trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
}
