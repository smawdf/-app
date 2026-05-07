package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

class InMemoryDishRepository(
    private val context: Context,
    initialDishes: List<Dish> = emptyList()
) : DishRepository {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, Dish::class.java)
    private val jsonAdapter = moshi.adapter<List<Dish>>(listType)
    private val storageFile: File
        get() = File(context.filesDir, "dishes.json")

    private val builtinDishes: List<Dish> by lazy { sampleDishes }

    private val _dishes = MutableStateFlow(
        loadFromFile() ?: (if (initialDishes.isNotEmpty()) initialDishes else builtinDishes)
    )

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
        val id = "dish_${System.currentTimeMillis()}"
        val newDish = dish.copy(id = id)
        _dishes.value = _dishes.value + newDish
        saveToFile()
        return id
    }

    override suspend fun updateDish(dish: Dish) {
        _dishes.value = _dishes.value.map { if (it.id == dish.id) dish else it }
        saveToFile()
    }

    override suspend fun deleteDish(id: String) {
        _dishes.value = _dishes.value.filter { it.id != id }
        saveToFile()
    }

    override fun getRecentDishes(limit: Int): Flow<List<Dish>> =
        _dishes.map { list -> list.sortedByDescending { it.createdAt }.take(limit) }

    // ── 本地 JSON 文件持久化 ──

    private fun saveToFile() {
        try {
            val json = jsonAdapter.toJson(_dishes.value)
            storageFile.writeText(json)
        } catch (_: Exception) { }
    }

    private fun loadFromFile(): List<Dish>? {
        return try {
            if (!storageFile.exists()) return null
            val json = storageFile.readText()
            jsonAdapter.fromJson(json)?.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        val sampleDishes = listOf(
            Dish(
                id = "1", name = "宫保鸡丁", source = "custom", category = "中餐",
                cookTimeMin = 30, difficulty = 2, ingredients = listOf("鸡胸肉 300g", "花生米 50g", "干辣椒 8个"),
                cookSteps = listOf(
                    com.myorderapp.domain.model.CookStep(1, "鸡胸肉切丁，加料酒、生抽腌制15分钟", tip = "加少许淀粉让肉更嫩"),
                    com.myorderapp.domain.model.CookStep(2, "调碗汁：醋、生抽、白糖、淀粉、水拌匀", tip = "糖醋比例 1:1"),
                    com.myorderapp.domain.model.CookStep(3, "热油炒鸡丁至变色盛出，爆香辣椒花椒"),
                    com.myorderapp.domain.model.CookStep(4, "回锅鸡丁，倒入碗汁翻炒，加花生米")
                ),
                notes = "她不太能吃辣，少放干辣椒",
                createdBy = "你创建", createdAt = "2026-04-28",
                whoLikes = listOf("你", "她")
            ),
            Dish(
                id = "2", name = "番茄炒蛋", source = "custom", category = "中餐",
                cookTimeMin = 15, difficulty = 1, ingredients = listOf("番茄 2个", "鸡蛋 3个", "葱花 适量"),
                cookSteps = listOf(
                    com.myorderapp.domain.model.CookStep(1, "番茄切块，鸡蛋打散加盐"),
                    com.myorderapp.domain.model.CookStep(2, "热油炒鸡蛋至凝固盛出"),
                    com.myorderapp.domain.model.CookStep(3, "炒番茄出汁，加糖调味"),
                    com.myorderapp.domain.model.CookStep(4, "倒回鸡蛋翻炒均匀")
                ),
                createdBy = "你创建", createdAt = "2026-04-25",
                whoLikes = listOf("你", "她")
            ),
            Dish(
                id = "3", name = "红烧排骨", source = "custom", category = "中餐",
                cookTimeMin = 60, difficulty = 3, ingredients = listOf("排骨 500g", "生抽", "老抽", "冰糖", "八角"),
                cookSteps = listOf(
                    com.myorderapp.domain.model.CookStep(1, "排骨焯水去血沫"),
                    com.myorderapp.domain.model.CookStep(2, "炒糖色，下排骨翻炒"),
                    com.myorderapp.domain.model.CookStep(3, "加开水没过排骨，小火炖40分钟"),
                    com.myorderapp.domain.model.CookStep(4, "大火收汁")
                ),
                createdBy = "她创建", createdAt = "2026-04-20",
                whoLikes = listOf("你")
            ),
        )
    }
}
