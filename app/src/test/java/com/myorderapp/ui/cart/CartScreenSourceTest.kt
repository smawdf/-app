package com.myorderapp.ui.cart

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CartScreenSourceTest {

    @Test
    fun `cart screen respects system bars and keeps checkout action usable`() {
        val source = readMainSource("ui/cart/CartScreen.kt")

        listOf(
            "CartTopBar(",
            "statusBarsPadding()",
            "CartSummaryBar(",
            "navigationBarsPadding()",
            "text = \"去结算\"",
            "enabled = !cart.isEmpty"
        ).forEach { expected ->
            assertTrue("Cart screen missing expected structure: $expected", source.contains(expected))
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
