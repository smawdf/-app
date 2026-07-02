package com.myorderapp.data.remote.recipe

import com.myorderapp.ui.search.ExternalDishImageResult
import com.myorderapp.ui.search.ExternalDishImageSearchResult
import com.myorderapp.ui.search.ExternalDishImageSource

class SpoonacularExternalDishImageSource(
    private val api: SpoonacularApi,
    private val apiKey: String
) : ExternalDishImageSource {

    override suspend fun search(query: String): ExternalDishImageSearchResult {
        if (apiKey.isBlank()) return ExternalDishImageSearchResult()

        return try {
            val response = api.searchRecipes(
                apiKey = apiKey,
                query = query
            )
            ExternalDishImageSearchResult(
                primary = response.results
                    .map { SpoonacularMapper.toExternalResult(it, query) }
                    .filter { !it.imageUrl.isNullOrBlank() }
            )
        } catch (_: Exception) {
            ExternalDishImageSearchResult()
        }
    }
}
