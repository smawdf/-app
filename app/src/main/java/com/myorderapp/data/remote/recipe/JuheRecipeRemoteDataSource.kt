package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

interface JuheRecipeRemoteDataSource {
    suspend fun searchRecipes(
        query: String,
        num: Int = 10,
        page: Int = 1
    ): JuheResult
}

class RetrofitJuheRecipeRemoteDataSource(
    private val api: JuheRecipeApi,
    private val apiKey: String
) : JuheRecipeRemoteDataSource {

    override suspend fun searchRecipes(
        query: String,
        num: Int,
        page: Int
    ): JuheResult {
        if (apiKey.isBlank() || apiKey.startsWith("your_")) {
            return JuheResult.NoKey
        }

        return try {
            val response = api.searchRecipes(
                apiKey = apiKey,
                word = query,
                num = num,
                page = page
            )
            if (response.isSuccess) {
                val result = response.result ?: return JuheResult.ApiError(
                    errorCode = response.errorCode,
                    message = response.errorMessage
                )
                JuheResult.Success(
                    dishes = result.list.map(JuheRecipeMapper::toBuiltinDish),
                    total = result.allnum,
                    page = result.curpage
                )
            } else {
                JuheResult.ApiError(
                    errorCode = response.errorCode,
                    message = response.errorMessage
                )
            }
        } catch (e: Exception) {
            JuheResult.NetworkError("${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

sealed class JuheResult {
    data class Success(
        val dishes: List<Dish>,
        val total: Int,
        val page: Int
    ) : JuheResult()

    data class ApiError(
        val errorCode: Int,
        val message: String
    ) : JuheResult()

    data class NetworkError(
        val message: String
    ) : JuheResult()

    data object NoKey : JuheResult()
}
