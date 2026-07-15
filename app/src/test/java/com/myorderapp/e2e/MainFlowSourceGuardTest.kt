package com.myorderapp.e2e

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainFlowSourceGuardTest {

    @Test
    fun `main couple ordering flow remains native compose end to end`() {
        val main = readMainSource("MainActivity.kt")
        val nav = readMainSource("ui/navigation/NavGraph.kt")
        val checkout = readMainSource("ui/checkout/CheckoutViewModel.kt")
        val orders = readMainSource("ui/orders/OrdersScreen.kt")
        val detail = readMainSource("ui/orders/OrderDetailScreen.kt")
        val appModule = readMainSource("di/AppModule.kt")

        listOf(
            "CoupleMenuScreen",
            "OrderingScreen",
            "DiscoverScreen",
            "CheckoutScreen",
            "OrdersScreen",
            "OrderDetailScreen",
            "ProfileScreen",
            "MenuManagementScreen",
            "AuthScreen",
            "OnboardingScreen",
            "ResetPasswordScreen",
            "AnniversaryScreen"
        ).forEach { expected ->
            assertTrue("Native route missing screen: $expected", nav.contains(expected))
        }

        listOf(
            "FloatingLiquidBottomBar",
            "HeartNavIndicator",
            "BottomNavItem.items",
            "navigateAsTab(route)",
            "Routes.resetPassword(initialDeepLink)",
            "sessionManager.isLoggedIn.collectAsStateWithLifecycle()"
        ).forEach { expected ->
            assertTrue("Native shell missing: $expected", main.contains(expected))
        }

        assertFalse("Runtime routes must not render Stitch WebView", nav.contains("StitchScreen"))
        assertFalse("Main shell must not host WebView", main.contains("WebView"))
        assertFalse("Main shell must not load Android assets as pages", main.contains("file:///android_asset"))

        assertTrue("Checkout should keep duplicate-submit protection", checkout.contains("isSubmitting"))
        assertTrue("Food diary should keep completed-order support", orders.contains("status == \"completed\""))
        assertTrue("Order detail should keep caretaker-only advance guard", detail.contains("canAdvanceOrder"))
        assertTrue("Runtime should keep injecting order repository", appModule.contains("single<OrderRepository>"))

        listOf("RealtimeService", "SupabaseMealRepository", "RoomWishlistRepository").forEach { legacy ->
            assertFalse("Main runtime should not restore legacy module: $legacy", appModule.contains(legacy))
        }
    }

    private fun readMainSource(relativePath: String): String {
        val path = resolvePath(
            "app/src/main/java/com/myorderapp/$relativePath",
            "src/main/java/com/myorderapp/$relativePath"
        )
        return Files.readString(path)
    }

    private fun resolvePath(vararg candidates: String) =
        candidates
            .map { Paths.get(it) }
            .firstOrNull { Files.exists(it) }
            ?: error("Path not found. Tried: ${candidates.joinToString()}")
}
