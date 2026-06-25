package com.myorderapp.data.repository

import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class SupabaseDishRepository(
    private val session: SessionManager,
    private val filesDir: File
) : DishRepository {

    private val client = SupabaseClientProvider.client
    private val _dishes = MutableStateFlow<List<Dish>>(emptyList())
    private val loadState = CloudDishLoadState()

    override fun getAllDishes(): Flow<List<Dish>> = _dishes

    override fun getDishesByCategory(category: String): Flow<List<Dish>> =
        _dishes.map { list -> list.filter { it.category == category } }

    override fun searchDishes(query: String): Flow<List<Dish>> =
        _dishes.map { list ->
            if (query.isBlank()) list
            else list.filter { it.name.contains(query, ignoreCase = true) }
        }

    override suspend fun getDishById(id: String): Dish? =
        _dishes.value.find { it.id == id }

    override suspend fun cacheSearchResult(dish: Dish) {
        val existing = _dishes.value.toMutableList()
        val idx = existing.indexOfFirst { it.id == dish.id }
        if (idx >= 0) existing[idx] = dish else existing.add(dish)
        _dishes.value = existing
    }

    override suspend fun addDish(dish: Dish): String {
        val pairId = requireActivePairId()
        val created = client.from("dishes").insert(dish.copy(pairId = pairId)) {
            select()
        }.decodeSingle<Dish>()
        _dishes.value = _dishes.value + created
        return created.id
    }

    override suspend fun updateDish(dish: Dish) {
        val pairId = dish.pairId.ifBlank { requireActivePairId() }
        val updated = client.from("dishes").update(dish.copy(pairId = pairId)) {
            select()
            filter { eq("id", dish.id) }
        }.decodeSingle<Dish>()
        _dishes.value = _dishes.value.map { if (it.id == dish.id) updated else it }
    }

    override suspend fun deleteDish(id: String) {
        client.from("dishes").delete {
            filter { eq("id", id) }
        }
        _dishes.value = _dishes.value.filter { it.id != id }
    }

    override fun getRecentDishes(limit: Int): Flow<List<Dish>> =
        _dishes.map { list -> list.sortedByDescending { it.createdAt }.take(limit) }

    suspend fun loadFromCloud() {
        val userId = session.currentUserId
        val pairId = session.currentPairId
        if (!loadState.shouldLoad(session.isLoggedIn.value, userId, pairId)) return

        _dishes.value = emptyList()
        try {
            withContext(Dispatchers.IO) {
                val dishes = client.from("dishes").select {
                    filter { eq("pair_id", pairId) }
                }.decodeList<Dish>()
                _dishes.value = dishes
                loadState.markLoaded(userId, pairId)
            }
        } catch (_: Exception) {
            loadState.reset()
        }
    }

    fun clearCloudCache() {
        loadState.reset()
        _dishes.value = emptyList()
    }

    private fun requireActivePairId(): String {
        val pairId = session.currentPairId
        require(session.isLoggedIn.value && pairId.isNotBlank()) {
            "登录后才能保存云端菜品"
        }
        return pairId
    }
}
