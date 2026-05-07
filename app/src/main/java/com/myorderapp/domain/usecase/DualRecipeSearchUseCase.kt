package com.myorderapp.domain.usecase

import com.myorderapp.data.remote.recipe.FoodTranslator
import com.myorderapp.data.remote.recipe.JuheRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JuheResult
import com.myorderapp.data.remote.recipe.SpoonacularRemoteDataSource
import com.myorderapp.data.remote.recipe.SpoonacularResult
import com.myorderapp.domain.model.Dish
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DualRecipeSearchUseCase(
    private val juheDataSource: JuheRecipeRemoteDataSource,
    private val spoonacularDataSource: SpoonacularRemoteDataSource
) {

    suspend fun search(query: String, numPerApi: Int = 20): DualSearchResult {
        return coroutineScope {
            val enQuery = FoodTranslator.translate(query)
            val juheDeferred = async { juheDataSource.searchRecipes(query, numPerApi) }
            val spoonDeferred = async {
                if (enQuery != query) {
                    spoonacularDataSource.searchRecipes(enQuery, numPerApi)
                } else {
                    spoonacularDataSource.searchRecipes(query, numPerApi)
                }
            }

            val juheResult = juheDeferred.await()
            val spoonResult = spoonDeferred.await()

            val allDishes = mutableListOf<Dish>()
            val sources = mutableListOf<String>()
            var juheTotal = 0
            var spoonTotal = 0
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

            when (spoonResult) {
                is SpoonacularResult.Success -> {
                    allDishes.addAll(spoonResult.dishes)
                    sources.add("Spoonacular(${spoonResult.total}条)")
                    spoonTotal = spoonResult.total
                }
                is SpoonacularResult.NetworkError -> errors.add("Spoonacular: ${spoonResult.message}")
                is SpoonacularResult.NoKey -> errors.add("Spoonacular: 未配置API Key")
            }

            // 交替排列两个源的结果，避免一个 API 的结果堆在一起
            val juheDishes = allDishes.filter { it.externalSource == "juhe" }
            val spoonDishes = allDishes.filter { it.externalSource == "spoonacular" }
            val merged = mergeAlternating(juheDishes, spoonDishes)

            DualSearchResult(
                dishes = merged,
                sources = sources,
                errors = errors,
                juheTotal = juheTotal,
                spoonacularTotal = spoonTotal
            )
        }
    }

    private fun mergeAlternating(list1: List<Dish>, list2: List<Dish>): List<Dish> {
        val result = mutableListOf<Dish>()
        val maxLen = maxOf(list1.size, list2.size)
        for (i in 0 until maxLen) {
            if (i < list1.size) result.add(list1[i])
            if (i < list2.size) result.add(list2[i])
        }
        return result
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
