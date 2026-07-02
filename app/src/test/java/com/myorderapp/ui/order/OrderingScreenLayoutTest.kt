package com.myorderapp.ui.order

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderingScreenLayoutTest {

    @Test
    fun `ordering screen uses current single shop ordering layout`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")

        assertTrue(source.contains("ShopBanner("))
        assertTrue(source.contains("OrderingSearchBar("))
        assertTrue(source.contains("NoticePromotionBar("))
        assertTrue(source.contains("CategoryRail("))
        assertTrue(source.contains("DishList("))
        assertTrue(source.contains("OrderingDishDetailSheet("))
        assertTrue(source.contains("ModalBottomSheet"))
        assertTrue(source.contains("LazyColumn"))
        assertFalse(source.contains("LazyVerticalGrid"))
    }

    @Test
    fun `ordering banner uses shop cover without marketplace metrics`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")
        val viewModel = readMainSource("ui/order/OrderingViewModel.kt")

        assertTrue(source.contains(".height(176.dp)"))
        assertTrue(source.contains("bannerImageUrl = uiState.shopCoverUrl"))
        assertTrue(viewModel.contains("singleShopRepository.getShopById(SINGLE_SHOP_ID)"))
        assertTrue(viewModel.contains("shopCoverUrl = shop?.coverUrl.orEmpty()"))
        assertTrue(viewModel.contains("shopCoverUrl = singleShopRepository.getShopImageUrl()"))
        assertFalse(source.contains("4.5"))
        assertFalse(source.contains("monthlySales = uiState.monthlySales"))
        assertFalse(source.contains("距离"))
        assertFalse(source.contains("45 分钟"))
    }

    @Test
    fun `ordering screen keeps category rail search and notice`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")

        assertTrue(source.contains(".width(86.dp)"))
        assertTrue(source.contains(".height(48.dp)"))
        assertTrue(source.contains(".width(3.dp)"))
        assertTrue(source.contains(".size(78.dp)"))
        assertTrue(source.contains(".size(30.dp)"))
        assertTrue(source.contains("OrderSearchField("))
        assertTrue(source.contains("Icons.Outlined.Notifications"))
        assertTrue(source.contains("月售 \${item.monthlySales}"))
        assertTrue(source.contains("搜索本店菜品"))
        assertTrue(source.contains("小狗厨师提醒"))
        assertTrue(source.contains("Color(0xFF247A84)"))
    }

    @Test
    fun `floating cart bar stays hidden until cart has items`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")

        assertTrue(source.contains("if (!uiState.cartState.isEmpty)"))
        assertTrue(source.contains("\"¥%.2f\""))
        assertTrue(source.contains("去结算"))
        assertTrue(source.contains("Color(0xFF247A84)"))
        assertTrue(source.contains("shadowElevation = 10.dp"))
        assertTrue(source.contains("ShoppingCart"))
        assertFalse(source.contains("购物车为空"))
    }

    @Test
    fun `ordering empty state sends owners to menu settings`() {
        val source = readMainSource("ui/order/OrderingScreen.kt")

        assertTrue(source.contains("去设置菜品"))
        assertTrue(source.contains("onManageMenuClick"))
        assertFalse(source.contains("Text(\"添加菜品\")"))
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
