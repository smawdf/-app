package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

class TianRecipeRemoteDataSource(
    private val api: TianRecipeApi,
    private val apiKey: String
) {

    suspend fun searchRecipes(
        query: String,
        num: Int = 20,
        page: Int = 1
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
                val dishes = response.result!!.list.map {
                    TianRecipeMapper.toBuiltinDish(it)
                }
                TianResult.Success(
                    dishes = dishes,
                    total = response.result.allnum,
                    page = response.result.curpage
                )
            } else {
                TianResult.ApiError(
                    errorCode = response.code,
                    message = response.errorMessage
                )
            }
        } catch (e: Exception) {
            val detail = "${e.javaClass.simpleName}: ${e.message}"
            TianResult.NetworkError(detail)
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
