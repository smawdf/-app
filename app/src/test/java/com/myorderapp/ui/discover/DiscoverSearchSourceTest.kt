package com.myorderapp.ui.discover

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverSearchSourceTest {

    @Test
    fun `discover page keeps search results and offers external video shortcuts`() {
        val screen = readMainSource("ui/discover/DiscoverScreen.kt")
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        val appModule = readMainSource("di/AppModule.kt")

        listOf(
            "DiscoverResultCard",
            "StitchDiscoverTopBar()",
            "DiscoverSurface.copy(alpha = 0.84f)",
            "border = BorderStroke(2.dp, DiscoverCardBorder)",
            "onAddToMenu"
        ).forEach { expected ->
            assertTrue("Discover page missing marker: $expected", screen.contains(expected))
        }

        assertTrue("Discover page should show Douyin/Bilibili shortcuts", screen.contains("RecipeVideoLinkIcons("))
        assertTrue(screen.contains("去抖音看看"))
        assertTrue(screen.contains("去哔站看看"))
        assertTrue(viewModel.contains("fun addToMenu"))
        assertTrue(viewModel.contains("DiscoverSearchMemoryCache"))
        assertTrue(viewModel.contains("restoreCachedSearch(query)"))
        assertTrue(viewModel.contains("imageRequests[requestKey]"))
        assertTrue(viewModel.contains("currentShopDishNames()"))
        assertTrue(viewModel.contains("normalizedMenuName()"))
        assertTrue(viewModel.contains("markResultAsAdded(item"))
        assertTrue(viewModel.contains("RoomMenuRepository"))
        assertTrue(appModule.contains("roomMenuRepository = get()"))
    }

    @Test
    fun `recommended dishes can be added to the single shop`() {
        val screen = readMainSource("ui/discover/DiscoverScreen.kt")
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")

        assertTrue(screen.contains("onAddToMenu = viewModel::addToMenu"))
        assertTrue(screen.contains("text = if (item.isAdded) \"已在店铺\" else \"加入店铺\""))
        assertTrue(screen.contains("onClick = { onAddToMenu(item) }"))
        assertTrue(viewModel.contains("markRecommendationAdded(item, resolvedImageUrl)"))
        assertTrue(viewModel.contains("isAdded = addedNames.contains"))
        assertTrue(viewModel.contains("it.equals(item.category.trim(), ignoreCase = true)"))
        assertTrue(viewModel.contains("?: currentCategories.firstOrNull() ?: \"未分类\""))
    }

    @Test
    fun `discover results prefer image-backed recipes`() {
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        assertTrue(viewModel.contains(".sortedByDescending { !it.imageUrl.isNullOrBlank() && !it.imageUrl.isLegacyRecipeImageUrl() }"))
        assertFalse(viewModel.contains("DEFAULT_DISH_IMAGE_URLS"))
    }

    @Test
    fun `discover hides recommendations while searching and keeps results scrollable`() {
        val screen = readMainSource("ui/discover/DiscoverScreen.kt")

        assertTrue(screen.contains("uiState.query.isBlank()"))
        assertTrue(screen.contains("uiState.recommendations.isNotEmpty()"))
        assertFalse(screen.contains("userScrollEnabled = uiState.results.size > 2"))
    }

    @Test
    fun `discover ignores legacy hoto recipe images`() {
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        val bimissingSource = readMainSource("data/local/BimissingRecipeAssetSource.kt")

        assertTrue(viewModel.contains("!it.isLegacyRecipeImageUrl()"))
        assertTrue(viewModel.contains("findImageForDishName(item.name, excludedImageUrl = item.imageUrl)"))
        assertTrue(viewModel.contains("preferredQuery = item.name"))
        assertFalse(viewModel.contains("?: item.imageUrl?.takeIf { it.isNotBlank() }"))
        assertTrue(bimissingSource.contains("!it.isLegacyRecipeImageUrl()"))
        assertTrue(bimissingSource.contains("res.hoto.cn"))
        assertTrue(bimissingSource.contains("fuzzyMatchScore(normalizedQuery)"))
        assertTrue(bimissingSource.contains("DishSynonymTokenGroups"))
    }

    @Test
    fun `daily recommendations survive release minification and asset parse failures`() {
        val source = readMainSource("data/local/BimissingRecipeAssetSource.kt")
        val appGradlePath = listOf(
            Paths.get("build.gradle.kts"),
            Paths.get("app/build.gradle.kts")
        ).firstOrNull { Files.exists(it) } ?: error("App Gradle file not found")
        val appGradle = Files.readString(appGradlePath)

        assertTrue(source.contains("@JsonClass(generateAdapter = true)"))
        assertTrue(source.contains("recipes.ifEmpty { FallbackRecipes }"))
        assertTrue(appGradle.contains("ksp(libs.moshi.kotlin.codegen)"))
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
