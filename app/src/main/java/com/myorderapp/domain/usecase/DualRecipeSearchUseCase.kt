package com.myorderapp.domain.usecase

import com.myorderapp.data.remote.recipe.JisuRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JisuResult
import com.myorderapp.data.remote.recipe.JuheRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JuheResult
import com.myorderapp.data.remote.recipe.TianRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.TianResult
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class DualRecipeSearchUseCase(
    private val juheDataSource: JuheRecipeRemoteDataSource,
    private val tianDataSource: TianRecipeRemoteDataSource,
    private val jisuDataSource: JisuRecipeRemoteDataSource,
    private val dishRepository: DishRepository
) {

    suspend fun search(query: String, numPerApi: Int = 20): DualSearchResult {
        return coroutineScope {
            // 1. 先查本地/Supabase
            val allLocal = dishRepository.getAllDishes().first()
            val localDishes = allLocal.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }

            if (localDishes.size >= 5) {
                return@coroutineScope DualSearchResult(
                    dishes = localDishes,
                    sources = listOf("本地数据库(${localDishes.size}条)"),
                    errors = emptyList(),
                    juheTotal = localDishes.size,
                    spoonacularTotal = 0
                )
            }

            // 2. 并行查询三个 API
            val juheDeferred = async { juheDataSource.searchRecipes(query, numPerApi) }
            val tianDeferred = async { tianDataSource.searchRecipes(query, numPerApi) }
            val jisuDeferred = async { jisuDataSource.searchRecipes(query, numPerApi) }

            val juheResult = juheDeferred.await()
            val tianResult = tianDeferred.await()
            val jisuResult = jisuDeferred.await()

            val allDishes = mutableListOf<Dish>()
            val sources = mutableListOf<String>()
            val errors = mutableListOf<String>()
            var juheTotal = 0

            // 本地结果
            allDishes.addAll(localDishes)
            if (localDishes.isNotEmpty()) sources.add("本地(${localDishes.size}条)")

            // Juhe
            when (juheResult) {
                is JuheResult.Success -> {
                    allDishes.addAll(juheResult.dishes)
                    sources.add("聚合数据(${juheResult.total}条)")
                    juheTotal = juheResult.total
                    juheResult.dishes.forEach { dishRepository.cacheSearchResult(it) }
                }
                is JuheResult.ApiError -> errors.add("聚合数据: ${juheResult.message}")
                is JuheResult.NetworkError -> errors.add("聚合数据: ${juheResult.message}")
                is JuheResult.NoKey -> errors.add("聚合数据: 未配置API Key")
            }

            // TianAPI
            when (tianResult) {
                is TianResult.Success -> {
                    allDishes.addAll(tianResult.dishes)
                    sources.add("天行数据(${tianResult.total}条)")
                    tianResult.dishes.forEach { dishRepository.cacheSearchResult(it) }
                }
                is TianResult.ApiError -> errors.add("天行数据: ${tianResult.message}")
                is TianResult.NetworkError -> errors.add("天行数据: ${tianResult.message}")
                is TianResult.NoKey -> errors.add("天行数据: 未配置API Key")
            }

            // JisuAPI
            when (jisuResult) {
                is JisuResult.Success -> {
                    allDishes.addAll(jisuResult.dishes)
                    sources.add("极速数据(${jisuResult.total}条)")
                    jisuResult.dishes.forEach { dishRepository.cacheSearchResult(it) }
                }
                is JisuResult.ApiError -> errors.add("极速数据: ${jisuResult.message}")
                is JisuResult.NetworkError -> errors.add("极速数据: ${jisuResult.message}")
                is JisuResult.NoKey -> errors.add("极速数据: 未配置API Key")
            }

            DualSearchResult(
                dishes = allDishes,
                sources = sources,
                errors = errors,
                juheTotal = juheTotal,
                spoonacularTotal = 0
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
