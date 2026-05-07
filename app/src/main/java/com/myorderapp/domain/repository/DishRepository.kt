package com.myorderapp.domain.repository

import com.myorderapp.domain.model.Dish
import kotlinx.coroutines.flow.Flow

interface DishRepository {
    fun getAllDishes(): Flow<List<Dish>>
    fun getDishesByCategory(category: String): Flow<List<Dish>>
    fun searchDishes(query: String): Flow<List<Dish>>
    suspend fun getDishById(id: String): Dish?
    suspend fun cacheSearchResult(dish: Dish)
    suspend fun addDish(dish: Dish): String
    suspend fun updateDish(dish: Dish)
    suspend fun deleteDish(id: String)
    fun getRecentDishes(limit: Int): Flow<List<Dish>>
}
