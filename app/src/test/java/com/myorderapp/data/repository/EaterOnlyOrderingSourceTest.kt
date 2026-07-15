package com.myorderapp.data.repository

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EaterOnlyOrderingSourceTest {

    @Test
    fun `ordering UI disables caretaker cart actions`() {
        val viewModel = readMain("ui/order/OrderingViewModel.kt")
        val screen = readMain("ui/order/OrderingScreen.kt")

        assertTrue(viewModel.contains("profile?.selectedRole == ROLE_EATER"))
        assertTrue(viewModel.contains("if (!_uiState.value.isEater) return false"))
        assertTrue(screen.contains("canOrder = uiState.isEater"))
        assertTrue(screen.contains("canManageShop = !uiState.isEater"))
        assertFalse(screen.contains("点菜和结算由吃货完成"))
    }

    @Test
    fun `checkout and repository reject caretaker submissions`() {
        val checkout = readMain("ui/checkout/CheckoutViewModel.kt")
        val orders = readMain("data/repository/SupabaseOrderRepository.kt")

        assertTrue(checkout.contains("if (!state.isEater)"))
        assertTrue(orders.contains("profile?.selectedRole != ROLE_EATER"))
        assertTrue(orders.contains("EATER_ROLE_REQUIRED"))
    }

    @Test
    fun `database spend function requires eater role`() {
        val sql = readProject("table/34_eater_only_ordering.sql")

        assertTrue(sql.contains("if caller_role <> 'eater'"))
        assertTrue(sql.contains("where user_id = caller_id"))
        assertTrue(sql.contains("'eater', 'eater', 'order spend'"))
    }

    private fun readMain(relativePath: String): String = Files.readString(
        existingPath(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
    )

    private fun readProject(relativePath: String): String = Files.readString(
        existingPath(Paths.get(relativePath), Paths.get("..").resolve(relativePath))
    )

    private fun existingPath(vararg candidates: Path): Path = candidates.first(Files::exists)
}
