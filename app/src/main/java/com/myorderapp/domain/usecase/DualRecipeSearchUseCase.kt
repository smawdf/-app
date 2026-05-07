package com.myorderapp.domain.usecase

import com.myorderapp.data.remote.recipe.JuheRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JuheResult
import com.myorderapp.data.remote.recipe.TheMealDBRemoteDataSource
import com.myorderapp.data.remote.recipe.TheMealDBResult
import com.myorderapp.domain.model.Dish
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DualRecipeSearchUseCase(
    private val juheDataSource: JuheRecipeRemoteDataSource,
    private val theMealDBDataSource: TheMealDBRemoteDataSource
) {

    suspend fun search(query: String, numPerApi: Int = 20): DualSearchResult {
        return coroutineScope {
            val juheDeferred = async { juheDataSource.searchRecipes(query, numPerApi) }
            val themealdbDeferred = async { theMealDBDataSource.searchRecipes(query, numPerApi) }

            val juheResult = juheDeferred.await()
            val themealdbResult = themealdbDeferred.await()

            val allDishes = mutableListOf<Dish>()
            val sources = mutableListOf<String>()
            var juheTotal = 0
            var themealdbTotal = 0
            val errors = mutableListOf<String>()

            when (juheResult) {
                is JuheResult.Success -> {
                    allDishes.addAll(juheResult.dishes)
                    sources.add("聚合数据(${juheResult.total}条)")
                    juheTotal = juheResult.total
                }
                is JuheResult.ApiError -> errors.add("聚合数据: ${juheResult.message}")
                is JuheResult.NetworkError -> errors.add("聚合数据: ${juheResult.message}")
                is JuheResult.NoKey -> errors.add("聚合数据: 未配置API Key")
            }

            when (themealdbResult) {
                is TheMealDBResult.Success -> {
                    allDishes.addAll(themealdbResult.dishes)
                    sources.add("TheMealDB(${themealdbResult.total}条)")
                    themealdbTotal = themealdbResult.total
                }
                is TheMealDBResult.NetworkError -> errors.add("TheMealDB: ${themealdbResult.message}")
            }

            DualSearchResult(
                dishes = allDishes,
                sources = sources,
                errors = errors,
                juheTotal = juheTotal,
                spoonacularTotal = themealdbTotal
            )
        }
    }
}

data class DualSearchResult(
    val dishes: List<Dish>,
    val sources: List<String>,
    val errors: List<String>,
    val juheTotal: Int,
    val spoonacularTotal: Int
) {
    val totalResults: Int get() = juheTotal + spoonacularTotal
    val isPartialSuccess: Boolean get() = dishes.isNotEmpty()
    val allFailed: Boolean get() = dishes.isEmpty() && errors.isNotEmpty()
}
