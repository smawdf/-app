package com.myorderapp.data.repository

import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseApi
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

class SupabaseDishRepository(
    private val api: SupabaseApi,
    private val session: SessionManager,
    private val filesDir: File
) : DishRepository {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Dish::class.java)
    private val jsonAdapter = moshi.adapter<List<Dish>>(listType)
    private val storageFile = File(filesDir, "dishes_cloud.json")

    private val _dishes = MutableStateFlow<List<Dish>>(loadFromFile())

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
        saveToFile()
    }

    override suspend fun addDish(dish: Dish): String {
        return try {
            val result = api.createDish(dish, session.accessToken)
            val created = result.firstOrNull() ?: dish
            _dishes.value = _dishes.value + created
            saveToFile()
            created.id
        } catch (_: Exception) {
            val id = "local_${System.currentTimeMillis()}"
            _dishes.value = _dishes.value + dish.copy(id = id)
            saveToFile()
            id
        }
    }

    override suspend fun updateDish(dish: Dish) {
        try {
            api.updateDish(dish.id, dish, session.accessToken)
        } catch (_: Exception) { }
        _dishes.value = _dishes.value.map { if (it.id == dish.id) dish else it }
        saveToFile()
    }

    override suspend fun deleteDish(id: String) {
        try { api.deleteDish(id, session.accessToken) } catch (_: Exception) { }
        _dishes.value = _dishes.value.filter { it.id != id }
        saveToFile()
    }

    override fun getRecentDishes(limit: Int): Flow<List<Dish>> =
        _dishes.map { list -> list.sortedByDescending { it.createdAt }.take(limit) }

    suspend fun syncFromCloud() {
        if (!session.isLoggedIn.value) return
        try {
            // 查所有菜品，不按 pair_id 过滤
            val dishes = api.getAllDishes(session.accessToken)
            _dishes.value = dishes
            saveToFile()
        } catch (_: Exception) { }
    }

    private fun saveToFile() {
        try {
            val json = jsonAdapter.toJson(_dishes.value)
            storageFile.writeText(json)
        } catch (_: Exception) { }
    }

    private fun loadFromFile(): List<Dish> {
        return try {
            if (!storageFile.exists()) return emptyList()
            val json = storageFile.readText()
            jsonAdapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
