package com.myorderapp.ui.navigation

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantFlowNavigationTest {

    @Test
    fun `bottom navigation exposes single shop tabs`() {
        val source = readMainSource("ui/navigation/BottomNavItem.kt")

        assertTrue(source.contains("title = \"首页\""))
        assertTrue(source.contains("data object Ordering"))
        assertTrue(source.contains("route = \"ordering\""))
        assertTrue(source.contains("title = \"点菜\""))
        assertTrue(source.contains("data object Discover"))
        assertTrue(source.contains("data object Orders"))
        assertTrue(source.contains("val items = listOf(Home, Ordering, Discover, Orders, Profile)"))
        assertFalse(source.contains("route = \"menu_management\""))
        assertFalse(source.contains("title = \"菜单\""))
        assertFalse(source.contains("data object Merchant"))
        assertFalse(source.contains("title = \"店铺\""))
    }

    @Test
    fun `main screen keeps the bottom bar on home and switches tabs in place`() {
        val source = readMainSource("MainActivity.kt")

        assertTrue(source.contains("showBottomBar = currentDestination?.route in bottomNavRoutes"))
        assertTrue(source.contains("WarmBottomBar"))
        assertTrue(source.contains("LiquidBottomNavItem"))
        assertTrue(source.contains("LiquidGlassTop"))
        assertTrue(source.contains("LiquidGlassBottom"))
        assertTrue(source.contains("RoundedCornerShape(34.dp)"))
        assertTrue(source.contains("shadow(18.dp"))
        assertTrue(source.contains("Brush.verticalGradient"))
        assertTrue(source.contains("modifier = Modifier.weight(1f)"))
        assertFalse("底部页签不应再使用固定宽度，避免右侧缺块", source.contains(".width(58.dp)"))
        assertTrue(source.contains("navController.navigateAsTab(route)"))
        assertTrue(source.contains("popUpTo(graph.findStartDestination().id)"))
        assertTrue(source.contains("restoreState = true"))
        assertTrue(source.contains("launchSingleTop = true"))
        assertFalse(source.contains("NavigationBar("))
        assertFalse(source.contains("NavigationBarItem("))
    }

    @Test
    fun `nav graph wires single shop flow screens`() {
        val source = readMainSource("ui/navigation/NavGraph.kt")

        assertTrue(source.contains("const val MENU_MANAGEMENT = \"menu_management\""))
        assertTrue(source.contains("const val SHOP_SETTINGS = \"shop_settings\""))
        assertTrue(source.contains("const val ORDERING = \"ordering\""))
        assertTrue(source.contains("const val DISCOVER = \"discover\""))
        assertTrue(source.contains("const val ORDERS = \"orders\""))
        assertTrue(source.contains("com.myorderapp.ui.couple.CoupleMenuScreen"))
        assertTrue(source.contains("com.myorderapp.ui.order.OrderingScreen"))
        assertTrue(source.contains("com.myorderapp.ui.discover.DiscoverScreen"))
        assertTrue(source.contains("com.myorderapp.ui.menu.MenuManagementScreen"))
        assertFalse(source.contains("com.myorderapp.ui.settings.ShopSettingsScreen"))
        assertTrue(source.contains("com.myorderapp.ui.orders.OrdersScreen"))
        assertFalse(source.contains("onCaretakerClick = { navController.navigate(Routes.SHOP_SETTINGS) }"))
        assertFalse(source.contains("onEaterClick = { navController.navigateAsTab(Routes.ORDERING) }"))
        assertTrue(source.contains("onGoOrderingClick = { navController.navigateAsTab(Routes.ORDERING) }"))
        assertTrue(source.contains("onManageMenuClick = { navController.navigate(Routes.SHOP_SETTINGS) }"))
        assertTrue(source.contains("onDishManageClick = { navController.navigate(Routes.SHOP_SETTINGS) }"))
        assertTrue(source.contains("onLoginClick = {"))
        assertTrue(source.contains("navController.navigate(Routes.AUTH)"))
        assertTrue(source.contains("popUpTo(0) { inclusive = true }"))
        assertTrue(source.contains("composable(Routes.SHOP_SETTINGS)"))
        assertTrue(source.contains("MenuManagementScreen(onBack = { navController.popBackStack() })"))
        assertFalse(source.contains("const val MERCHANT"))
        assertFalse(source.contains("const val SHOP_DETAIL"))
        assertFalse(source.contains("ui.merchant"))
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
