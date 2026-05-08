package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

class JisuRecipeRemoteDataSource(
    private val api: JisuRecipeApi,
    private val apiKey: String
) {

    suspend fun searchRecipes(
        query: String,
        num: Int = 20,
        start: Int = 0
    ): JisuResult {
        if (apiKey.isBlank()) {
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
                val dishes = response.result!!.list.map {
                    JisuRecipeMapper.toBuiltinDish(it)
                }
                JisuResult.Success(
                    dishes = dishes,
                    total = response.result.total,
                    count = response.result.num
                )
            } else {
                JisuResult.ApiError(
                    errorCode = response.status,
                    message = response.errorMessage
                )
            }
        } catch (e: Exception) {
            val detail = "${e.javaClass.simpleName}: ${e.message}"
            JisuResult.NetworkError(detail)
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
