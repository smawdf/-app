package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

interface TianRecipeRemoteDataSource {
    suspend fun searchRecipes(
        query: String,
        num: Int = 10,
        page: Int = 1
    ): TianResult
}

class RetrofitTianRecipeRemoteDataSource(
    private val api: TianRecipeApi,
    private val apiKey: String
) : TianRecipeRemoteDataSource {

    override suspend fun searchRecipes(
        query: String,
        num: Int,
        page: Int
    ): TianResult {
        if (apiKey.isBlank()) {
            return TianResult.NoKey
        }

        return try {
            val response = api.searchRecipes(
                apiKey = apiKey,
                word = query,
                num = num,
                page = page
            )
            if (response.isSuccess) {
                val result = response.result ?: return TianResult.ApiError(
                    errorCode = response.code,
                    message = response.errorMessage
                )
                TianResult.Success(
                    dishes = result.list.map(TianRecipeMapper::toBuiltinDish),
                    total = result.allnum,
                    page = result.curpage
                )
            } else {
                TianResult.ApiError(
                    errorCode = response.code,
                    message = response.errorMessage
                )
            }
        } catch (e: Exception) {
            TianResult.NetworkError("${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

sealed class TianResult {
    data class Success(
        val dishes: List<Dish>,
        val total: Int,
        val page: Int
    ) : TianResult()

    data class ApiError(
        val errorCode: Int,
        val message: String
    ) : TianResult()

    data class NetworkError(
        val message: String
    ) : TianResult()

    data object NoKey : TianResult()
}
