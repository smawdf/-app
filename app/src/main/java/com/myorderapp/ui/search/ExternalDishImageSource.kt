package com.myorderapp.ui.search

interface ExternalDishImageSource {
    suspend fun search(query: String): ExternalDishImageSearchResult
}

data class ExternalDishImageSearchResult(
    val primary: List<ExternalDishImageResult> = emptyList(),
    val fallback: List<ExternalDishImageResult> = emptyList()
)

class FakeExternalDishImageSource(
    private val primaryResults: List<ExternalDishImageResult> = emptyList(),
    private val fallbackResults: List<ExternalDishImageResult> = emptyList()
) : ExternalDishImageSource {

    val queries = mutableListOf<String>()

    override suspend fun search(query: String): ExternalDishImageSearchResult {
        queries += query
        return ExternalDishImageSearchResult(
            primary = primaryResults,
            fallback = fallbackResults
        )
    }
}
