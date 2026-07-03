package com.myorderapp.e2e

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainFlowSourceGuardTest {

    @Test
    fun `main couple ordering flow remains wired end to end`() {
        val nav = readMainSource("ui/navigation/NavGraph.kt")
        val home = readMainSource("ui/couple/CoupleMenuScreen.kt")
        val ordering = readMainSource("ui/order/OrderingScreen.kt")
        val discover = readMainSource("ui/discover/DiscoverScreen.kt")
        val checkout = readMainSource("ui/checkout/CheckoutViewModel.kt")
        val orders = readMainSource("ui/orders/OrdersScreen.kt")
        val detail = readMainSource("ui/orders/OrderDetailScreen.kt")
        val profile = readMainSource("ui/profile/ProfileScreen.kt")
        val appModule = readMainSource("di/AppModule.kt")

        listOf(
            "ONBOARDING",
            "AUTH",
            "CoupleMenuScreen",
            "OrderingScreen",
            "DiscoverScreen",
            "CheckoutScreen",
            "OrdersScreen",
            "OrderDetailScreen",
            "ProfileScreen",
            "MenuManagementScreen"
        ).forEach { expected ->
            assertTrue("导航缺少主流程页面：$expected", nav.contains(expected))
        }

        listOf(
            "selectRole(CoupleRole.Caretaker)",
            "selectRole(CoupleRole.Eater)",
            "putString(KEY_SELECTED_ROLE, role.storageKey)",
            "LatestOrderNudge",
            "touchPresence()",
            "notifyActiveOrderIfAllowed"
        ).forEach { expected ->
            assertTrue("首页主流程缺失：$expected", home.contains(expected))
        }

        assertTrue("点菜页应保留购物车入口", ordering.contains("CartSheet") && ordering.contains("Checkout"))
        assertTrue("发现页应保留加入我的店铺", discover.contains("加入我的小店") && discover.contains("confirmAddToMenu"))
        assertTrue("结算应防重复提交", checkout.contains("isSubmitting"))
        assertTrue("美食日记应接入已完成订单", orders.contains("status == \"completed\""))
        assertTrue("订单详情应限制饲养员推进状态", detail.contains("canAdvanceOrder"))
        assertTrue("我的页应管理伴侣绑定", profile.contains("PairManagementDialog"))
        assertTrue("运行时应注入订单仓储", appModule.contains("single<OrderRepository>"))

        listOf("RealtimeService", "SupabaseMealRepository", "RoomWishlistRepository").forEach { legacy ->
            assertFalse("主运行时不应恢复旧模块：$legacy", appModule.contains(legacy))
        }
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath)
        )
        val path = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(path)
    }
}
