package com.myorderapp.data.local

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAssetPackSourceTest {

    @Test
    fun `small offline recipe pack is bundled with searchable images`() {
        val recipeAsset = resolveProjectFile("src/main/assets/recipes.json", "app/src/main/assets/recipes.json")
        val repository = readMainSource("data/repository/HybridDishRepository.kt")
        val appModule = readMainSource("di/AppModule.kt")
        val loader = readMainSource("data/local/RecipeAssetLoader.kt")

        assertTrue("内置菜谱小包应随 App assets 打包", Files.exists(recipeAsset))

        val recipesJson = Files.readString(recipeAsset)
        assertTrue("内置菜谱小包应包含基础菜谱数据", recipesJson.contains("\"recipes\""))
        assertTrue("内置菜谱小包应包含可搜索的中文菜名", recipesJson.contains("\"name\": \"宫保鸡丁\""))
        assertTrue("内置番茄炒蛋应带图片，避免发现页展示占位图", recipesJson.contains("\"name\": \"番茄炒蛋\""))
        assertTrue("内置番茄炒蛋应带图片，避免发现页展示占位图", recipesJson.contains("07968b333d974e6997242d28214f1eed"))
        assertTrue("内置鱼香肉丝应带图片，避免发现页展示占位图", recipesJson.contains("\"name\": \"鱼香肉丝\""))
        assertTrue("内置鱼香肉丝应带图片，避免发现页展示占位图", recipesJson.contains("f3067a6e886111e6b87c0242ac110003"))
        assertTrue("内置红烧肉应带图片，避免发现页展示占位图", recipesJson.contains("\"name\": \"红烧肉\""))
        assertTrue("内置红烧肉应带图片，避免发现页展示占位图", recipesJson.contains("b44e5c2a8a6811e6a9a10242ac110002"))
        assertTrue("内置菜谱小包应包含豉汁蒸排骨，避免下厨房临时不可用时直接搜不到", recipesJson.contains("\"name\": \"豉汁蒸排骨\""))
        assertTrue("内置豉汁蒸排骨应带下厨房图片，避免发现页展示占位图", recipesJson.contains("cd2022e5260f43fbba37aa82ce03e610"))
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
        assertTrue("RecipeAssetLoader 应读取 assets 中的 imageUrl", loader.contains("val imageUrl: String? = null"))
        assertTrue("RecipeAssetLoader 应把 imageUrl 映射到 Dish", loader.contains("imageUrl = imageUrl?.trim()?.takeIf"))
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
