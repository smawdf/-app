package com.myorderapp.ui.discover

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XiachufangDiscoverPrioritySourceTest {

    @Test
    fun `discover search prefers exact matches before fuzzy fallback`() {
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        val appModule = readMainSource("di/AppModule.kt")
        val networkModule = readMainSource("di/NetworkModule.kt")

        assertTrue(viewModel.contains("XiachufangRecipeSearchSource"))
        assertTrue(viewModel.contains("xiachufangRecipeSearchSource.search(query, limit = 20)"))
        assertTrue(viewModel.contains("publishExactOrRememberFuzzy"))
        assertTrue(viewModel.contains("exactNameMatches(query)"))
        assertTrue(viewModel.contains("fuzzyNameMatches(query)"))
        assertTrue(viewModel.contains("if (publishExactOrRememberFuzzy(xiachufangResults)) return"))
        assertTrue(viewModel.contains("if (publishExactOrRememberFuzzy(localAndMenuResults, menuItems)) return"))
        assertTrue(viewModel.contains("if (publishExactOrRememberFuzzy(tianResults, menuItems)) return"))
        assertFalse(viewModel.contains("if (publishExactOrRememberFuzzy(externalResults"))
        assertFalse(viewModel.contains("logSourceResults(query, \"external_images\""))
        assertFalse(viewModel.contains("juheRecipeRemoteDataSource"))
        assertFalse(viewModel.contains("jisuRecipeRemoteDataSource"))
        assertTrue(viewModel.contains("candidateResults = fuzzyFallbackResults"))
        assertTrue(viewModel.contains("allowEmpty = true"))
        assertTrue(viewModel.contains("withBackfilledImages(fallbackImageQuery = fallbackImageQuery)"))
        assertTrue(viewModel.contains("shouldPreferEarlierImageResult(exactResults, fuzzyFallbackResults)"))
        assertTrue(viewModel.contains("findImageForDishName(item.name, excludedImageUrl = item.imageUrl)"))
        assertTrue(viewModel.contains("preferredQuery = item.name"))
        assertTrue(viewModel.contains("fun String.fuzzyDishMatchScore"))
        assertTrue(viewModel.contains("fun String.dishMatchTokenGroups"))
        assertTrue(viewModel.contains("DishSynonymTokenGroups"))
        assertTrue(viewModel.contains("fun ensureImageFor"))
        assertTrue(viewModel.contains("val imageResults = externalDishImageSource.search(query)"))
        assertTrue(viewModel.contains("xiachufangRecipeSearchSource.search(query, limit = 12)"))
        assertTrue(viewModel.contains("imageSearchQueriesForDishName(name, preferredQuery)"))
        assertTrue(viewModel.contains("bestImageUrlForName("))
        assertTrue(viewModel.contains("excludedImageUrl = item.imageUrl"))
        assertTrue(viewModel.contains("sourceLabel = result.source.ifBlank { \"external\" }"))
        assertFalse(viewModel.contains("fallbackImageUrlForName"))
        assertFalse(viewModel.contains("DEFAULT_DISH_IMAGE_URLS"))
        assertFalse(viewModel.contains("KNOWN_DISH_IMAGE_URLS"))
        assertTrue(viewModel.contains("val resolvedImageUrl = item.imageUrl?.takeIf"))
        assertTrue(viewModel.contains("imageUrl = resolvedImageUrl"))
        assertTrue(viewModel.contains("allowEmpty -> emptyList()"))
        assertFalse(viewModel.contains("createManualSearchResult"))
        assertFalse(viewModel.contains("sourceLabel = \"manual\""))
        assertTrue(appModule.contains("xiachufangRecipeSearchSource = get()"))
        assertFalse(appModule.contains("juheRecipeRemoteDataSource = get()"))
        assertFalse(appModule.contains("jisuRecipeRemoteDataSource = get()"))
        assertTrue(networkModule.contains("XiachufangRecipeSearchSource"))
        assertFalse(networkModule.contains("JUHE_BASE_URL"))
        assertFalse(networkModule.contains("JISU_BASE_URL"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }
}
