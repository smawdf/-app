package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

interface JisuRecipeRemoteDataSource {
    suspend fun searchRecipes(
        query: String,
        num: Int = 10,
        start: Int = 0
    ): JisuResult
}

class RetrofitJisuRecipeRemoteDataSource(
    private val api: JisuRecipeApi,
    private val apiKey: String
) : JisuRecipeRemoteDataSource {

    override suspend fun searchRecipes(
        query: String,
        num: Int,
        start: Int
    ): JisuResult {
        if (apiKey.isBlank() || apiKey.startsWith("your_")) {
            return JisuResult.NoKey
        }

        return try {
            val response = api.searchRecipes(
                keyword = query,
                num = num,
                start = start,
                appkey = apiKey
            )
            if (response.isSuccess) {
                val result = response.result ?: return JisuResult.ApiError(
                    errorCode = response.status,
                    message = response.errorMessage
                )
                JisuResult.Success(
                    dishes = result.list.map(JisuRecipeMapper::toBuiltinDish),
                    total = result.total,
                    count = result.num
                )
            } else {
                JisuResult.ApiError(
                    errorCode = response.status,
                    message = response.errorMessage
                )
            }
        } catch (e: Exception) {
            JisuResult.NetworkError("${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

sealed class JisuResult {
    data class Success(
        val dishes: List<Dish>,
        val total: Int,
        val count: Int
    ) : JisuResult()

    data class ApiError(
        val errorCode: Int,
        val message: String
    ) : JisuResult()

    data class NetworkError(
        val message: String
    ) : JisuResult()

    data object NoKey : JisuResult()
}
