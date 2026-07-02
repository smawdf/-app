package com.myorderapp.ui.discover

import java.nio.file.Files
import java.nio.file.Paths
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
            "DishRepository",
            "MenuRepository",
            "ExternalDishImageSource",
            "DiscoverDishSearchItem",
            "onQueryChanged"
        ).forEach { expected ->
            assertTrue("发现搜索缺少数据链路：$expected", viewModel.contains(expected))
        }

        assertTrue("DiscoverViewModel 未注册到 Koin", appModule.contains("viewModel { DiscoverViewModel("))
        assertTrue("Koin 缺少天行菜谱注入", appModule.contains("tianRecipeRemoteDataSource = get()"))
        assertTrue("发现页不应再依赖 Jisu 旧接口", !viewModel.contains("JisuRecipeRemoteDataSource"))
        assertTrue("发现页不应再依赖 Juhe 旧接口", !viewModel.contains("JuheRecipeRemoteDataSource"))
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
            assertTrue("发现页空状态不应再展示：$removedText", !screen.contains(removedText))
        }

        assertTrue(
            "发现页搜索结果应优先展示有图结果",
            viewModel.contains(".sortedByDescending { !it.imageUrl.isNullOrBlank() }")
        )
    }

    @Test
    fun `discover search results can be added to the managed menu`() {
        val screen = readMainSource("ui/discover/DiscoverScreen.kt")
        val viewModel = readMainSource("ui/discover/DiscoverViewModel.kt")
        val appModule = readMainSource("di/AppModule.kt")

        assertTrue("搜索结果卡片应暴露加入小店回调", screen.contains("onAddToMenu"))
        assertTrue("搜索结果卡片应显示加入我的小店按钮", screen.contains("加入我的小店"))
        assertTrue("搜索结果卡片应显示已在我的小店状态", screen.contains("已在我的小店"))
        assertTrue("ViewModel 应提供 addToMenu 行为", viewModel.contains("fun addToMenu"))
        assertTrue("ViewModel 应跟踪已加入小店的结果", viewModel.contains("addedMenuItemIds"))
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
