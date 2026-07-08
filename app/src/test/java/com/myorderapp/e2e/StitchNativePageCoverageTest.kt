package com.myorderapp.e2e

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StitchNativePageCoverageTest {

    @Test
    fun `all exported Stitch pages have native Compose landing points`() {
        (1..13).forEach { pageNumber ->
            val page = "page_$pageNumber"
            val pageDir = resolvePath(
                "app/src/main/assets/stitch/$page",
                "src/main/assets/stitch/$page"
            )
            assertTrue("Missing Stitch export directory: $page", Files.exists(pageDir))
            assertTrue("Missing Stitch HTML export: $page", Files.exists(pageDir.resolve("code.html")))
            assertTrue("Missing Stitch screenshot: $page", Files.exists(pageDir.resolve("screen.png")))
        }

        val mappings = listOf(
            NativePageMapping("page_1", "ui/couple/AnniversaryScreen.kt", listOf("AnniversaryScreen")),
            NativePageMapping("page_2", "ui/discover/DiscoverScreen.kt", listOf("DiscoverScreen")),
            NativePageMapping("page_3", "ui/couple/CoupleMenuScreen.kt", listOf("CoupleMenuScreen")),
            NativePageMapping("page_4", "ui/orders/OrdersScreen.kt", listOf("OrdersScreen")),
            NativePageMapping("page_5", "ui/profile/ProfileScreen.kt", listOf("ProfileScreen")),
            NativePageMapping("page_6", "ui/order/OrderingScreen.kt", listOf("OrderingScreen")),
            NativePageMapping("page_7", "ui/auth/AuthScreen.kt", listOf("AuthScreen")),
            NativePageMapping("page_8", "ui/onboarding/OnboardingScreen.kt", listOf("OnboardingScreen")),
            NativePageMapping("page_9", "ui/onboarding/OnboardingScreen.kt", listOf("ActivityResultContracts.GetContent()")),
            NativePageMapping("page_10", "ui/auth/ResetPasswordScreen.kt", listOf("ResetPasswordScreen")),
            NativePageMapping("page_11", "ui/checkout/CheckoutScreen.kt", listOf("CheckoutScreen")),
            NativePageMapping("page_12", "ui/menu/MenuManagementScreen.kt", listOf("MenuManagementScreen")),
            NativePageMapping("page_13", "ui/menu/MenuManagementScreen.kt", listOf("DishEditorDialog"))
        )

        mappings.forEach { mapping ->
            val source = readMainSource(mapping.sourcePath)
            mapping.markers.forEach { marker ->
                assertTrue("${mapping.page} native mapping missing marker: $marker", source.contains(marker))
            }
            assertFalse("${mapping.page} must not be implemented as a WebView", source.contains("WebView"))
            assertFalse("${mapping.page} must not load Stitch HTML at runtime", source.contains("file:///android_asset/stitch"))
            assertFalse("${mapping.page} must not embed raw HTML", source.contains("<html"))
        }
    }

    @Test
    fun `main navigation keeps Stitch pages native and task flow pages chrome free`() {
        val main = readMainSource("MainActivity.kt")
        val nav = readMainSource("ui/navigation/NavGraph.kt")

        listOf("HOME", "ORDERING", "DISCOVER", "ORDERS", "PROFILE").forEach { tab ->
            assertTrue("Bottom nav item missing: $tab", readMainSource("ui/navigation/BottomNavItem.kt").contains(tab))
        }
        assertTrue("Main shell should keep floating liquid nav", main.contains("FloatingLiquidBottomBar"))
        assertFalse("Main shell should not leave a traditional Scaffold bottom bar slot", main.contains("bottomBar ="))

        listOf("CHECKOUT", "ORDER_DETAIL", "CART", "AUTH", "ONBOARDING", "ANNIVERSARY").forEach { route ->
            assertTrue("Task flow route missing: $route", nav.contains(route))
        }
        assertFalse(nav.contains("StitchScreen"))
        assertFalse(nav.contains("WebView"))
    }

    private data class NativePageMapping(
        val page: String,
        val sourcePath: String,
        val markers: List<String>
    )

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }

    private fun resolvePath(vararg candidates: String) =
        candidates
            .map { Paths.get(it) }
            .firstOrNull { Files.exists(it) }
            ?: error("Path not found. Tried: ${candidates.joinToString()}")
}
