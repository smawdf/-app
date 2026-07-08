package com.myorderapp.ui.navigation

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantFlowNavigationTest {

    @Test
    fun `bottom navigation model exposes native single shop tabs`() {
        val source = readMainSource("ui/navigation/BottomNavItem.kt")

        assertTrue(source.contains("data object Home"))
        assertTrue(source.contains("data object Ordering"))
        assertTrue(source.contains("route = Routes.ORDERING"))
        assertTrue(source.contains("data object Discover"))
        assertTrue(source.contains("data object Orders"))
        assertTrue(source.contains("data object Profile"))
        assertTrue(source.contains("val items = listOf(Home, Ordering, Discover, Orders, Profile)"))
        assertTrue(source.contains("title = \"首页\""))
        assertTrue(source.contains("title = \"点餐\""))
        assertTrue(source.contains("title = \"发现\""))
        assertTrue(source.contains("title = \"订单\""))
        assertTrue(source.contains("title = \"我的\""))
        assertTrue(source.contains("Icons.Outlined.Restaurant"))
        assertTrue(source.contains("Icons.AutoMirrored.Outlined.ReceiptLong"))
        assertFalse(source.contains("RamenDining"))
        assertFalse(source.contains("AutoMirrored.Outlined.List"))
        assertFalse(source.contains("data object Merchant"))
    }

    @Test
    fun `main screen owns native floating liquid tab bar`() {
        val source = readMainSource("MainActivity.kt")

        assertTrue(source.contains("FloatingLiquidBottomBar"))
        assertTrue(source.contains("BottomNavItem.items"))
        assertTrue(source.contains("currentRoute in tabRoutes"))
        assertTrue(source.contains("navController.navigateAsTab(route)"))
        assertTrue(source.contains("RoundedCornerShape(36.dp)"))
        assertTrue(source.contains("Column("))
        assertTrue(source.contains("Color(0xFFFFD1DC).copy(alpha = 0.82f)"))
        assertTrue(source.contains("item.unselectedIcon"))

        assertFalse(source.contains("blur("))
        assertFalse(source.contains("shadow("))
        assertFalse(source.contains("WarmBottomBar"))
        assertFalse(source.contains("LiquidBottomNavItem"))
        assertFalse(source.contains("Scaffold("))
        assertFalse(source.contains("WebView"))
        assertFalse(source.contains("file:///android_asset"))
    }

    @Test
    fun `nav graph wires single shop flow through native compose screens`() {
        val source = readMainSource("ui/navigation/NavGraph.kt")

        listOf(
            "const val MENU_MANAGEMENT = \"menu_management\"",
            "const val SHOP_SETTINGS = \"shop_settings\"",
            "const val ORDERING = \"ordering\"",
            "const val DISCOVER = \"discover\"",
            "const val ORDERS = \"orders\""
        ).forEach { expected ->
            assertTrue(source.contains(expected))
        }

        listOf(
            "CoupleMenuScreen",
            "OrderingScreen",
            "DiscoverScreen",
            "OrdersScreen",
            "ProfileScreen",
            "MenuManagementScreen",
            "CheckoutScreen",
            "AuthScreen",
            "OnboardingScreen",
            "onOrdersClick = { navController.navigateAsTab(Routes.ORDERS) }"
        ).forEach { expected ->
            assertTrue("Missing native route mapping: $expected", source.contains(expected))
        }

        assertFalse(source.contains("StitchScreen"))
        assertFalse(source.contains("StitchPage"))
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
