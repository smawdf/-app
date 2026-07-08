package com.myorderapp.data.remote.recipe

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingRecipeNetworkSourceTest {

    @Test
    fun `blocking recipe scrapers run okhttp execute on io dispatcher`() {
        val xiachufang = readMainSource("data/remote/recipe/XiachufangRecipeSearchSource.kt")
        val bing = readMainSource("data/remote/recipe/BingDishImageSource.kt")

        listOf(xiachufang, bing).forEach { source ->
            assertTrue(source.contains("withContext(Dispatchers.IO)"))
            assertTrue(source.contains(".execute().use"))
        }
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
