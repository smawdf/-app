package com.myorderapp.data.local

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAssetPackSourceTest {

    @Test
    fun `small offline recipe pack is bundled and wired into discover search`() {
        val recipeAsset = resolveProjectFile("src/main/assets/recipes.json", "app/src/main/assets/recipes.json")
        val repository = readMainSource("data/repository/HybridDishRepository.kt")
        val appModule = readMainSource("di/AppModule.kt")

        assertTrue("内置菜谱小包应随 App assets 打包", Files.exists(recipeAsset))

        val recipesJson = Files.readString(recipeAsset)
        assertTrue("内置菜谱小包应包含基础菜谱数据", recipesJson.contains("\"recipes\""))
        assertTrue("内置菜谱小包应包含可搜索的中文菜名", recipesJson.contains("宫保鸡丁"))
        assertTrue("内置菜谱小包应保持轻量，避免显著增大安装包", Files.size(recipeAsset) < 200_000)

        listOf(
            "RecipeAssetLoader",
            "recipeAssetLoader.loadRecipes()",
            "searchBuiltInRecipes",
            "dish.ingredients.any",
            "dish.cookSteps.any"
        ).forEach { expected ->
            assertTrue("发现页搜索缺少内置菜谱接入：$expected", repository.contains(expected))
        }

        assertTrue(
            "Koin 应把 RecipeAssetLoader 注入 HybridDishRepository",
            appModule.contains("HybridDishRepository(get(), get(), get(), get())")
        )
    }

    private fun readMainSource(relativePath: String): String {
        val sourcePath = resolveProjectFile(
            "src/main/java/com/myorderapp/$relativePath",
            "app/src/main/java/com/myorderapp/$relativePath"
        )
        return Files.readString(sourcePath)
    }

    private fun resolveProjectFile(vararg relativePaths: String) =
        relativePaths
            .map { Paths.get(it) }
            .firstOrNull { Files.exists(it) }
            ?: error("Source file not found: ${relativePaths.joinToString()}")
}
