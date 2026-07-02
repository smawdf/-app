package com.myorderapp.ui.search

class CombinedExternalDishImageSource(
    private val primarySource: ExternalDishImageSource,
    private val fallbackSource: ExternalDishImageSource
) : ExternalDishImageSource {

    override suspend fun search(query: String): ExternalDishImageSearchResult {
        val primary = primarySource.search(query)
        if (primary.primary.isNotEmpty()) {
            return primary
        }

        val fallback = fallbackSource.search(query)
        return ExternalDishImageSearchResult(
            primary = primary.primary,
            fallback = fallback.fallback.ifEmpty { primary.fallback }
        )
    }
}
