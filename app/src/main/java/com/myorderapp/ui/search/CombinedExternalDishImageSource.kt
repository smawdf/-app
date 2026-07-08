package com.myorderapp.ui.search

class CombinedExternalDishImageSource(
    private val sources: List<ExternalDishImageSource>
) : ExternalDishImageSource {

    constructor(
        primarySource: ExternalDishImageSource,
        fallbackSource: ExternalDishImageSource
    ) : this(listOf(primarySource, fallbackSource))

    override suspend fun search(query: String): ExternalDishImageSearchResult {
        val fallbackResults = mutableListOf<ExternalDishImageResult>()

        for (source in sources) {
            val result = source.search(query)
            if (result.primary.isNotEmpty()) {
                return ExternalDishImageSearchResult(
                    primary = result.primary,
                    fallback = fallbackResults + result.fallback
                )
            }
            fallbackResults += result.fallback
        }

        return ExternalDishImageSearchResult(
            fallback = fallbackResults
        )
    }
}
