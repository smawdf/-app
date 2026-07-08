package com.myorderapp.ui.candy

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CandyCoinIconSourceTest {

    @Test
    fun `candy coin icon asset is used in candy related screens`() {
        assertTrue(
            Files.exists(Paths.get("app/src/main/res/drawable-nodpi/candy_coin.png")) ||
                Files.exists(Paths.get("src/main/res/drawable-nodpi/candy_coin.png"))
        )
        val icon = readMainSource("ui/components/CandyCoinIcon.kt")
        val candy = readMainSource("ui/candy/CandyCoinsScreen.kt")
        val checkout = readMainSource("ui/checkout/CheckoutScreen.kt")
        val orders = readMainSource("ui/orders/OrdersScreen.kt")
        val detail = readMainSource("ui/orders/OrderDetailScreen.kt")

        assertTrue(icon.contains("R.drawable.candy_coin"))
        assertTrue(candy.contains("CandyCoinIcon"))
        assertTrue(checkout.contains("CandyCoinIcon"))
        assertTrue(orders.contains("CandyCoinIcon"))
        assertTrue(detail.contains("CandyCoinIcon"))
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
