package com.myorderapp.data.repository

import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseApi
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class SupabaseDishRepository(
    private val api: SupabaseApi,
    private val session: SessionManager,
    private val filesDir: File
) : DishRepository {

    private val _dishes = MutableStateFlow<List<Dish>>(emptyList())
    private var loaded = false

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
        val result = api.createDish(dish, session.accessToken)
        val created = result.firstOrNull() ?: dish
        _dishes.value = _dishes.value + created
        return created.id
    }

    override suspend fun updateDish(dish: Dish) {
        try {
            api.updateDish(dish.id, dish, session.accessToken)
        } catch (_: Exception) { }
        _dishes.value = _dishes.value.map { if (it.id == dish.id) dish else it }
    }

    override suspend fun deleteDish(id: String) {
        api.deleteDish(id, session.accessToken)
        _dishes.value = _dishes.value.filter { it.id != id }
    }

    override fun getRecentDishes(limit: Int): Flow<List<Dish>> =
        _dishes.map { list -> list.sortedByDescending { it.createdAt }.take(limit) }

    suspend fun loadFromCloud() {
        if (!session.isLoggedIn.value) return
        if (loaded) return
        try {
            withContext(Dispatchers.IO) {
                val dishes = api.getAllDishes(session.accessToken)
                _dishes.value = dishes
                loaded = true
            }
        } catch (_: Exception) { }
    }
}
