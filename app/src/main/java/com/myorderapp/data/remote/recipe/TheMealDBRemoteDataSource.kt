package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

class TheMealDBRemoteDataSource(private val api: TheMealDBApi) {

    suspend fun searchRecipes(query: String, num: Int = 20): TheMealDBResult {
        return try {
            val response = api.searchMeals(query)
            val meals = response.meals ?: emptyList()
            val dishes = meals.take(num).map { TheMealDBMapper.fromMeal(it) }
            TheMealDBResult.Success(dishes, meals.size)
        } catch (e: Exception) {
            TheMealDBResult.NetworkError(e.message ?: "TheMealDB 连接失败")
        }
    }

    suspend fun randomRecipe(): TheMealDBResult {
        return try {
            val response = api.randomMeal()
            val meals = response.meals ?: emptyList()
            val dishes = meals.map { TheMealDBMapper.fromMeal(it) }
            TheMealDBResult.Success(dishes, dishes.size)
        } catch (e: Exception) {
            TheMealDBResult.NetworkError(e.message ?: "TheMealDB 连接失败")
        }
    }
}

sealed class TheMealDBResult {
    data class Success(val dishes: List<Dish>, val total: Int) : TheMealDBResult()
    data class NetworkError(val message: String) : TheMealDBResult()
}
