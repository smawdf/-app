package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.Dish

class JuheRecipeRemoteDataSource(
    private val api: JuheRecipeApi,
    private val apiKey: String
) {

    suspend fun searchRecipes(
        query: String,
        num: Int = 20,
        page: Int = 1
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
                val dishes = response.result!!.list.map {
                    JuheRecipeMapper.toBuiltinDish(it)
                }
                JuheResult.Success(
                    dishes = dishes,
                    total = response.result.allnum,
                    page = response.result.curpage
                )
            } else {
                JuheResult.ApiError(
                    errorCode = response.errorCode,
                    message = response.errorMessage
                )
            }
        } catch (e: Exception) {
            val detail = "${e.javaClass.simpleName}: ${e.message}"
            JuheResult.NetworkError(detail)
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
