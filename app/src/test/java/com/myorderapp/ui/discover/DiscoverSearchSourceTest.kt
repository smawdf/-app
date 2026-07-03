package com.myorderapp.ui.discover

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverSearchSourceTest {

    @Test
    fun `discover page has searchable recipe results backed by active recipe sources`() {
        val screen = readMainSource("ui/discover/DiscoverScreen.kt")
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        val appModule = readMainSource("di/AppModule.kt")

        listOf(
            "搜索菜品、做法、食材",
            "正在搜索菜品...",
            "没有找到相关菜品",
            "部分网络结果暂不可用",
            "DiscoverResultCard"
        ).forEach { expected ->
            assertTrue("发现页缺少搜索 UI：$expected", screen.contains(expected))
        }

        listOf(
            "TianRecipeRemoteDataSource",
            "TianResult",
            "JuheRecipeRemoteDataSource",
            "JuheResult",
            "JisuRecipeRemoteDataSource",
            "JisuResult",
            "DishRepository",
            "MenuRepository",
            "ExternalDishImageSource",
            "DiscoverDishSearchItem",
            "onQueryChanged",
            "toRecipeSourceResult",
            "聚合菜谱",
            "极速菜谱"
        ).forEach { expected ->
            assertTrue("发现搜索缺少数据链路：$expected", viewModel.contains(expected))
        }

        val networkModule = readMainSource("di/NetworkModule.kt")
        val apiConfig = readMainSource("ApiConfig.kt")
        assertTrue("DiscoverViewModel 未注册到 Koin", appModule.contains("viewModel { DiscoverViewModel("))
        assertTrue("Koin 缺少天行菜谱注入", appModule.contains("tianRecipeRemoteDataSource = get()"))
        assertTrue("Koin 缺少聚合菜谱注入", appModule.contains("juheRecipeRemoteDataSource = get()"))
        assertTrue("Koin 缺少极速菜谱注入", appModule.contains("jisuRecipeRemoteDataSource = get()"))
        assertTrue("网络模块缺少聚合 Retrofit", networkModule.contains("JUHE_BASE_URL"))
        assertTrue("网络模块缺少极速 Retrofit", networkModule.contains("JISU_BASE_URL"))
        assertTrue("配置缺少聚合 API Key", apiConfig.contains("JUHE_API_KEY"))
        assertTrue("配置缺少极速 API Key", apiConfig.contains("JISU_API_KEY"))
    }

    @Test
    fun `discover page hides placeholder cards and prioritizes image results`() {
        val screen = readMainSource("ui/discover/DiscoverScreen.kt")
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")

        listOf(
            "纪念日点菜",
            "好友点菜",
            "点菜清单"
        ).forEach { removedText ->
            assertFalse("发现页空状态不应再展示：$removedText", screen.contains(removedText))
        }

        assertTrue(
            "发现页搜索结果应优先展示有图结果",
            viewModel.contains(".sortedByDescending { !it.imageUrl.isNullOrBlank() }")
        )
    }

    @Test
    fun `discover search results can be added to the managed menu with duplicate feedback`() {
        val screen = readMainSource("ui/discover/DiscoverScreen.kt")
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        val appModule = readMainSource("di/AppModule.kt")

        assertTrue("搜索结果卡片应暴露加入小店回调", screen.contains("onAddToMenu"))
        assertTrue("搜索结果卡片应显示加入我的小店按钮", screen.contains("加入我的小店"))
        assertTrue("搜索结果卡片应显示已在我的小店状态", screen.contains("已在我的小店"))
        assertTrue("加入弹窗应允许调整售价", screen.contains("售价"))
        assertTrue("加入弹窗应允许选择或填写分类", screen.contains("自定义分类"))
        assertTrue("ViewModel 应提供 addToMenu 行为", viewModel.contains("fun addToMenu"))
        assertTrue("ViewModel 应跟踪已加入小店的结果", viewModel.contains("addedMenuItemIds"))
        assertTrue("ViewModel 应按名称去重", viewModel.contains("addedMenuItemNames.contains(normalizedName)"))
        assertTrue("重复加入应给出已在我的小店提示", viewModel.contains("message = \"已在我的小店：${'$'}{item.name}\""))
        assertTrue("ViewModel 应写入 RoomMenuRepository", viewModel.contains("RoomMenuRepository"))
        assertTrue("Koin 应给 DiscoverViewModel 注入 RoomMenuRepository", appModule.contains("roomMenuRepository = get()"))
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
