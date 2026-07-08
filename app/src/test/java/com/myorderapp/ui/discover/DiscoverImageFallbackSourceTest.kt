package com.myorderapp.ui.discover

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverImageFallbackSourceTest {

    @Test
    fun `discover result card asks viewmodel to backfill missing images`() {
        val source = readMainSource("ui/discover/DiscoverScreen.kt")

        assertTrue(source.contains("onEnsureImage = viewModel::ensureImageFor"))
        assertTrue(source.contains("onRecoverImage = viewModel::recoverImageFor"))
        assertTrue(source.contains("LaunchedEffect(result.id, result.imageUrl)"))
        assertTrue(source.contains("onEnsureImage(result)"))
        assertTrue(source.contains("onLoadError = { onRecoverImage(result) }"))
        assertTrue(source.contains("var loadFailed by remember(imageUrl)"))
        assertTrue(source.contains("loadFailed = true"))
        assertTrue(source.contains("displaySourceName()"))
        assertTrue(source.contains("\"xiachufang\" -> \"下厨房\""))
        assertTrue(source.contains("DishImageOrPlaceholder"))
        assertTrue(source.contains("\"暂无图片\""))
        assertFalse(source.contains("\"manual\" -> \"手动添加\""))
        assertFalse(source.contains("DEFAULT_DISCOVER_DISH_IMAGE_URL"))
        assertFalse(source.contains("Icons.Outlined.Restaurant"))
    }

    @Test
    fun `discover viewmodel can recover broken image urls`() {
        val source = readMainSource("ui/discover/DiscoverViewModel.kt")

        assertTrue(source.contains("fun recoverImageFor"))
        assertTrue(source.contains("replaceResultImage(item, imageUrl)"))
        assertTrue(source.contains("imageUrl == item.imageUrl"))
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
