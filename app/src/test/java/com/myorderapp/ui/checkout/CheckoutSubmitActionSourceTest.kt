package com.myorderapp.ui.checkout

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutSubmitActionSourceTest {
    @Test
    fun `submit action stays clickable and reports insufficient coins`() {
        val screen = readMainSource("ui/checkout/CheckoutScreen.kt")
        val viewModel = readMainSource("ui/checkout/CheckoutViewModel.kt")

        assertTrue(screen.contains("Button("))
        assertTrue(screen.contains("val enabled = !cart.isEmpty && !isSubmitting"))
        assertTrue(screen.contains("errorMessage = uiState.errorMessage"))
        assertTrue(screen.contains("errorMessage?.let { message ->"))
        assertTrue(screen.contains("糖糖币不足 · 需要"))
        assertTrue(viewModel.contains("if (state.candyCoins < candyCost)"))
        assertTrue(viewModel.contains("糖糖币不够啦，找饲养员撒点糖再点菜"))
    }

    private fun readMainSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )
}
