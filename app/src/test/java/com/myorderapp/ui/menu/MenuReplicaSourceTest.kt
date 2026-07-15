package com.myorderapp.ui.menu

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuReplicaSourceTest {

    @Test
    fun `my shop follows Stitch sections and scrolls as one page`() {
        val source = readMainSource("ui/menu/MenuManagementScreen.kt")

        listOf(
            "StoreTopBar",
            "ShopSettingsStrip",
            "CategoryManagementBento",
            "DishManagementHeader",
            "DishEditorDialog",
            "LazyColumn(",
            "contentPadding = PaddingValues(bottom = 128.dp)",
            "modifier = modifier.fillMaxWidth()"
        ).forEach { expected ->
            assertTrue("My shop page missing marker: $expected", source.contains(expected))
        }

        assertFalse(source.contains("RecipeVideoLinkIcons("))
        assertFalse(source.contains("MenuCartBar"))
        assertFalse(source.contains("QuantityStepper"))
        assertFalse(source.contains("AddShoppingCart"))
        assertFalse("新增菜品应直接进入原型表单，不应保留中间选择层", source.contains("AddRecipeSheet"))
        assertFalse("新增菜品应直接进入原型表单，不应保留手动添加入口", source.contains("手动添加"))
        assertFalse(source.contains("Row(\n                    modifier = Modifier\n                        .fillMaxSize()"))
        assertFalse(source.contains("LazyColumn(\n                modifier = Modifier.fillMaxSize()"))
    }

    @Test
    fun `caretaker can edit and sync the shop name`() {
        val source = readMainSource("ui/menu/MenuManagementScreen.kt")
        val viewModel = readMainSource("ui/menu/MenuManagementViewModel.kt")
        val repository = readMainSource("data/repository/SingleShopRepository.kt")

        assertTrue(source.contains("ShopNameDialog("))
        assertTrue(source.contains("onEditShopName = { showShopNameDialog = true }"))
        assertTrue(source.contains("contentDescription = \"编辑店铺名称\""))
        assertTrue(source.contains("viewModel.saveShopName()"))
        assertTrue(viewModel.contains("singleShopRepository.updateShopName(name)"))
        assertTrue(repository.contains("syncShopSettingsToCloud()"))
    }

    @Test
    fun `category and dish management use prototype controls`() {
        val source = readMainSource("ui/menu/MenuManagementScreen.kt")

        listOf(
            "categories.map { CategoryCardModel.Category(it) } + CategoryCardModel.Create",
            "LazyRow(",
            "modifier = Modifier.width(154.dp)",
            "CategoryBentoCard",
            "CategoryBentoCardContent",
            "onManageCategoriesClick = { showCategoryManagerDialog = true }",
            "管理全部分类",
            "新增分类",
            "菜品管理",
            "新增菜品",
            "categoryIcon(category)",
            "Icons.Outlined.LocalPizza",
            "Icons.Outlined.Cake",
            "Icons.Outlined.LocalCafe",
            "Color(0xFFFF9FB7)",
            "Icons.Outlined.AddPhotoAlternate",
            "onClick = viewModel::newDish",
            "MenuFilter.All to \"全部\"",
            "MenuFilter.Available to \"已上架\"",
            "MenuFilter.Unavailable to \"已下架\""
        ).forEach { expected ->
            assertTrue("Prototype management control missing: $expected", source.contains(expected))
        }

        assertFalse("Visible dish management header should not expose batch controls", source.contains("DishManagementHeader(\n    totalCount"))
        assertFalse("Visible dish management header should not expose sorting controls", source.contains("onSortSelected = viewModel::setSortMode"))
        assertFalse("Category bento cards should not filter or expand the dish list", source.contains("onCategoryClick = viewModel::selectCategory"))
    }

    @Test
    fun `dish editor follows page 13 add dish prototype`() {
        val source = readMainSource("ui/menu/MenuManagementScreen.kt")
        val viewModel = readMainSource("ui/menu/MenuManagementViewModel.kt")

        listOf(
            "DishEditorDialog",
            "新增菜品",
            "给你们的小饭桌添一道新菜",
            "从相册选择菜品图",
            "名称",
            "售价 (¥)",
            "原价 (可选)",
            "分类",
            "选择或新建分类",
            "主食",
            "小吃",
            "饮品",
            "库存",
            "描述",
            "是否上架",
            "立即在小店展示",
            "是否招牌",
            "带有专属标识",
            "保存菜品",
            "已新增菜品"
        ).forEach { expected ->
            assertTrue("Page 13 dish editor missing prototype text: $expected", source.contains(expected) || viewModel.contains(expected))
        }

        assertFalse("新增菜品不应回退为 page_12 的旧弹层标题", source.contains("上新啦！"))
        assertFalse("新增菜品不应回退为 page_12 的旧上传文案", source.contains("点击上传美味照片"))
        assertFalse("新增菜品不应回退为 page_12 的旧提交文案", source.contains("确认上架"))
    }

    @Test
    fun `dish management list is not scoped by category card taps`() {
        val viewModel = readMainSource("ui/menu/MenuManagementViewModel.kt")

        assertFalse(
            "Dish management should follow the Stitch all-listed-available-unavailable tabs, not category-card filtering",
            viewModel.contains(".filter { selectedCategory.isBlank() || it.category == selectedCategory }")
        )
        assertTrue("Unavailable dishes should move after available dishes in all filter", viewModel.contains("compareBy<MenuDishEntity> { !it.isAvailable }"))
    }

    @Test
    fun `old bundled demo menu is removed`() {
        val viewModel = readMainSource("ui/menu/MenuManagementViewModel.kt")
        val repository = readMainSource("data/repository/SingleShopRepository.kt")

        assertTrue(repository.contains("removeBundledDemoMenu"))
        assertTrue(viewModel.contains("singleShopRepository.removeBundledDemoMenu()"))
        assertFalse(repository.contains("ensureSeedMenu"))
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
