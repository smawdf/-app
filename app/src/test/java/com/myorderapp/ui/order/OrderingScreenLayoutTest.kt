package com.myorderapp.ui.order

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderingScreenLayoutTest {

    @Test
    fun `ordering screen uses native single shop layout without video shortcuts`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")

        listOf(
            "CozyPage",
            "ShopCard(",
            "CategoryRail(",
            "DishList(",
            "OrderingDishDetailSheet(",
            "CartFloatingBar",
            "AddDishButton",
            "announcement = uiState.shopAnnouncement",
            "R.drawable.shop_banner_stitch"
        ).forEach { expected ->
            assertTrue("Ordering screen missing marker: $expected", source.contains(expected))
        }

        assertFalse(source.contains("RecipeVideoLinkIcons("))
        assertFalse(source.contains("LazyVerticalGrid"))
        assertFalse(source.contains("WebView"))
        assertFalse(source.contains("images.unsplash.com"))
        assertFalse("点餐页外层容器不应通过底部 padding 缩短内容区", source.contains(".padding(bottom = if (uiState.cartState.isEmpty) FloatingBottomNavClearance else FloatingCartListClearance)"))
        assertFalse("左侧分类应统一为无图标文字", source.contains("\"🔥 热销\""))
        assertTrue("购物车应按系统导航栏计算并贴在底部导航上方", source.contains("WindowInsets.navigationBars") && source.contains("bottomOffset = cartBottomOffset"))
        assertTrue("点菜页购物车按钮文案应明确为去结算", source.contains("SquishyCheckoutButton(text = \"去结算\""))
        assertTrue("菜品列表应通过 contentPadding 预留浮动导航空间", source.contains("bottom = bottomClearance + 14.dp"))
        assertTrue(
            "店铺卡容器必须由内容撑开，不能被 matchParentSize 压成 0 高度",
            source.contains("modifier = modifier\n            .scale(if (pressed && onClick != null) CozyMotion.SoftPressedScale else 1f)")
        )
    }

    @Test
    fun `caretaker ordering view hides menu descriptions`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")

        assertTrue(source.contains("showDescription = uiState.isEater"))
        assertTrue(source.contains("if (showDescription)"))
        assertTrue(source.contains("showDescription: Boolean"))
        assertTrue(source.contains("canManageShop = !uiState.isEater"))
        assertTrue(source.contains("text = displayAnnouncement"))
        assertFalse(source.contains("if (showDescription) {\n                        Row(verticalAlignment = Alignment.Top"))
        assertFalse(source.contains("CaretakerBrowseNotice()"))
    }

    @Test
    fun `shop information stays compact and constrained on large screens`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")

        assertTrue(source.contains("BoxWithConstraints("))
        assertTrue(source.contains("widthIn(max = 840.dp)"))
        assertTrue(source.contains("widthIn(max = 440.dp)"))
        assertTrue(source.contains("modifier = Modifier.size(width = coverWidth, height = coverHeight)"))
        assertTrue(source.contains("contentDescription = \"管理店铺\""))
        assertFalse(source.contains("displayShopName.length >"))
    }

    @Test
    fun `cart sheet uses only modal drag handle and clear checkout copy`() {
        val source = readMainSource("ui/shop/components/CartSheet.kt")

        assertFalse("购物车弹层不应再额外绘制第二条拖拽条", source.contains("size(width = 46.dp, height = 5.dp)"))
        assertFalse("购物车说明不应再使用含混的去结算提示", source.contains("点击下方去结算会直接进入确认点菜"))
        assertTrue(source.contains("确认购物篮后进入确认点菜"))
        assertTrue(source.contains("text = \"去结算\""))
    }

    @Test
    fun `ordering screen observes managed shop data`() {
        val repository = readMainSource("data/repository/SingleShopRepository.kt")
        val viewModel = readMainSource("ui/order/OrderingViewModel.kt")

        assertTrue(repository.contains("categoryState.value = getCategoryNames()"))
        assertTrue(repository.contains("combine(categoryState.asStateFlow(), menuDishDao.observeByPair(localPairId()))"))
        assertTrue(repository.contains("\"user:\$it\""))
        assertTrue(viewModel.contains("menuRepository.getMenuCategories(SINGLE_SHOP_ID)"))
        assertTrue(viewModel.contains("menuRepository.getMenuItems(SINGLE_SHOP_ID)"))
        assertTrue(viewModel.contains("shopAnnouncement = shop?.announcement ?: singleShopRepository.getShopAnnouncement()"))
        assertTrue(viewModel.contains("categories.any { it.isOrderingHotCategory() }"))
        assertTrue(viewModel.contains("singleShopRepository.removeBundledDemoMenu()"))
        assertFalse(viewModel.contains("ensureSeedMenu"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath from ${Paths.get("").toAbsolutePath()}")
        return Files.readString(sourcePath)
    }
}
