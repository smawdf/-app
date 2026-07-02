package com.myorderapp.ui.menu

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuReplicaSourceTest {

    @Test
    fun `my shop keeps manual add as the only add sheet action`() {
        val source = readMainSource("ui/menu/MenuManagementScreen.kt")
        val viewModel = readMainSource("ui/menu/MenuManagementViewModel.kt")
        val repository = readMainSource("data/repository/SingleShopRepository.kt")
        val combinedSource = source + viewModel + repository

        listOf(
            "MenuTopBar",
            "ShopSettingsStrip",
            "updateShopImage",
            "getShopImageUrl",
            "updateShopImageUrl",
            "AddRecipeSheet",
            "Icons.Outlined.Add",
            "onAddDish = { showAddSheet = true }"
        ).forEach { expected ->
            assertTrue("我的店铺缺少入口或能力：$expected", combinedSource.contains(expected))
        }

        assertEquals("添加弹层只应保留一个手动添加功能项", 1, Regex("Text\\(\"手动添加\"").findAll(source).count())
        assertFalse("我的店铺添加弹层不应包含广场偷菜", source.contains("广场偷菜"))
        assertFalse("我的店铺添加弹层不应包含克隆厨房", source.contains("克隆厨房"))
        assertTrue("排序应使用独立下拉菜单", source.contains("sortMenuExpanded"))
        assertTrue("排序按钮应只保留图标", source.contains("IconOnlyButton("))
        assertFalse("工具栏不应再保留额外分类管理入口", source.contains("contentDescription = \"分类管理\"") || source.contains("contentDescription = \"鍒嗙被绠＄悊\""))
        assertFalse("工具栏不应显示分类管理文字按钮", source.contains("SecondaryButton(text = \"分类管理\"") || source.contains("SecondaryButton(text = \"鍒嗙被绠＄悊\""))
    }

    @Test
    fun `menu category rail follows ordering categories and uses long press actions`() {
        val source = readMainSource("ui/menu/MenuManagementScreen.kt")
        val viewModel = readMainSource("ui/menu/MenuManagementViewModel.kt")

        listOf(
            "singleShopRepository.getCategoryNames()",
            "dishes.map { it.category }",
            "normalizedMenuCategories"
        ).forEach { expected ->
            assertTrue("分类栏应和点菜页使用同一套店铺分类来源：$expected", viewModel.contains(expected))
        }

        listOf(
            "CategoryRenameDialog",
            "renameCategory",
            "deleteCategory",
            "combinedClickable",
            "activeCategoryAction",
            "onCategoryRenameClick",
            "onCategoryDeleteClick",
            "onCategoryLongClick",
            "if (actionsVisible)"
        ).forEach { expected ->
            assertTrue("分类标签应长按后才能修改删除：$expected", (source + viewModel).contains(expected))
        }

        assertTrue("分类区默认标题应展示分类", source.contains("CategoryRailTitle(\"分类\")") || source.contains("CategoryRailTitle(\"鍒嗙被\")"))
        assertTrue("分类新增入口应只显示一个加号图标", source.contains("Icon(Icons.Outlined.Add"))
        assertTrue("分类新增入口应显示新建分类文字", source.contains("Text(\"新建分类\"") || source.contains("Text(\"鏂板缓鍒嗙被\""))
        assertFalse("分类新增入口不应同时显示文本加号", source.contains("\"+ 新建分类\"") || source.contains("\"+ 鏂板缓鍒嗙被\""))
        assertTrue("分类标签普通点击只应选择分类，长按才进入操作态", source.contains("onClick = onClick") && source.contains("onLongClick = onCategoryLongClick"))
        assertFalse("我的店铺左栏不应额外插入全部分类", source.contains("listOf(MenuManagementViewModel.ALL_CATEGORIES)"))
    }

    @Test
    fun `my shop implements requested merchant operations without cart controls`() {
        val screen = readMainSource("ui/menu/MenuManagementScreen.kt")
        val viewModel = readMainSource("ui/menu/MenuManagementViewModel.kt")
        val combinedSource = screen + viewModel

        listOf(
            "batchSetAvailability",
            "batchMoveToCategory",
            "batchDeleteSelected",
            "toggleDishAvailability",
            "MenuFilter",
            "MenuSortMode",
            "SalesDesc",
            "PriceAsc",
            "Newest",
            "DeleteDishDialog",
            "AvailabilitySwitch"
        ).forEach { expected ->
            assertTrue("新版我的店铺缺少商家操作：$expected", combinedSource.contains(expected))
        }

        listOf(
            "自定义拖拽",
            "DragIndicator",
            "MenuCartBar",
            "QuantityStepper",
            "AddShoppingCart",
            "去结算",
            "cartQuantities",
            "addDishToCart",
            "decreaseDishInCart"
        ).forEach { forbidden ->
            assertFalse("我的店铺不应包含客户购物车或拖拽排序：$forbidden", combinedSource.contains(forbidden))
        }
    }

    @Test
    fun `menu page removes mini program capsule chrome`() {
        val source = readMainSource("ui/menu/MenuManagementScreen.kt")

        listOf("MiniProgramCapsule", "•••").forEach { forbidden ->
            assertFalse("我的店铺页不应包含小程序胶囊：$forbidden", source.contains(forbidden))
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
