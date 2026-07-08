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
        assertTrue(viewModel.contains("currentShopDishNames()"))
        assertTrue(viewModel.contains("normalizedMenuName()"))
        assertTrue(viewModel.contains("markResultAsAdded(item"))
        assertTrue(viewModel.contains("RoomMenuRepository"))
        assertTrue(appModule.contains("roomMenuRepository = get()"))
    }

    @Test
    fun `discover results prefer image-backed recipes`() {
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        assertTrue(viewModel.contains(".sortedByDescending { !it.imageUrl.isNullOrBlank() && !it.imageUrl.isLegacyRecipeImageUrl() }"))
        assertFalse(viewModel.contains("DEFAULT_DISH_IMAGE_URLS"))
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
