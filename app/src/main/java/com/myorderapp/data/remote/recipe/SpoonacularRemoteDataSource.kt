package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

class SpoonacularRemoteDataSource(
    private val api: SpoonacularApi,
    private val apiKey: String
) {

    suspend fun searchRecipes(
        query: String,
        num: Int = 20
    ): SpoonacularResult {
        if (apiKey.isBlank()) return SpoonacularResult.NoKey

        return try {
            val response = api.searchRecipes(
                apiKey = apiKey,
                query = query,
                number = num
            )
            val dishes = response.results.map { SpoonacularMapper.fromSearchResult(it) }
            SpoonacularResult.Success(
                dishes = dishes,
                total = response.totalResults
            )
        } catch (e: Exception) {
            SpoonacularResult.NetworkError(e.message ?: "Spoonacular 连接失败")
        }
    }

    suspend fun getRecipeDetail(recipeId: Int): SpoonacularDetailResult {
        return try {
            val detail = api.getRecipeDetail(recipeId = recipeId, apiKey = apiKey)
            val dish = SpoonacularMapper.fromDetail(detail)
            SpoonacularDetailResult.Success(dish)
        } catch (e: Exception) {
            SpoonacularDetailResult.NetworkError(e.message ?: "获取详情失败")
        }
    }
}

sealed class SpoonacularResult {
    data class Success(val dishes: List<Dish>, val total: Int) : SpoonacularResult()
    data class NetworkError(val message: String) : SpoonacularResult()
    data object NoKey : SpoonacularResult()
}

sealed class SpoonacularDetailResult {
    data class Success(val dish: Dish) : SpoonacularDetailResult()
    data class NetworkError(val message: String) : SpoonacularDetailResult()
}
