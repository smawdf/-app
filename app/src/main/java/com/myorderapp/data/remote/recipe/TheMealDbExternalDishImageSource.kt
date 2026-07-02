package com.myorderapp.data.remote.recipe

import com.myorderapp.ui.search.ExternalDishImageSearchResult
import com.myorderapp.ui.search.ExternalDishImageSource

class TheMealDbExternalDishImageSource(
    private val api: TheMealDbApi
) : ExternalDishImageSource {

    override suspend fun search(query: String): ExternalDishImageSearchResult {
        return try {
            val response = api.searchMeals(query)
            ExternalDishImageSearchResult(
                fallback = response.meals
                    .orEmpty()
                    .map { TheMealDbMapper.toExternalResult(it, query) }
                    .filter { !it.imageUrl.isNullOrBlank() }
            )
        } catch (_: Exception) {
            ExternalDishImageSearchResult()
        }
    }
}
