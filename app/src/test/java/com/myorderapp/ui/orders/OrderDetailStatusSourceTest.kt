package com.myorderapp.ui.orders

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDetailStatusSourceTest {

    @Test
    fun `only caretaker can accept an order and begin preparation`() {
        val screen = readMainSource("ui/orders/OrderDetailScreen.kt")
        val viewModel = readMainSource("ui/orders/OrderDetailViewModel.kt")
        val ordersScreen = readMainSource("ui/orders/OrdersScreen.kt")
        val ordersViewModel = readMainSource("ui/orders/OrdersViewModel.kt")
        val repository = readMainSource("data/repository/SupabaseOrderRepository.kt")
        val sql = readProjectSource("table/35_caretaker_order_acceptance.sql")

        assertTrue(screen.contains("val canAdvanceOrder = uiState.isCaretaker"))
        assertTrue(screen.contains("确认接单"))
        assertTrue(screen.contains("待饲养员确认"))
        assertTrue(screen.contains("nextActionText = order?.status?.nextActionText()?.takeIf { canAdvanceOrder }"))
        assertFalse(screen.contains("enabled = canAdvanceOrder"))
        assertTrue(screen.contains("preparing"))
        assertFalse(screen.contains("COUPLE_HOME_PREFS"))

        assertTrue(viewModel.contains("profile?.selectedRole == ROLE_CARETAKER"))
        assertTrue(viewModel.contains("if (!_uiState.value.isCaretaker)"))
        assertTrue(viewModel.contains("nextOrderStatus"))
        assertTrue(viewModel.contains("preparing"))
        assertTrue(viewModel.contains("completed"))
        assertTrue(viewModel.contains("runCatching"))
        assertTrue(viewModel.contains("orderRepository.refreshOrders()"))

        assertTrue(ordersScreen.contains("onClick = { onOrderClick(order.id) }"))
        assertTrue(ordersScreen.contains("onAdvance = { viewModel.advanceOrder(order) }"))
        assertTrue(ordersScreen.contains("onClick = onAdvance"))
        assertTrue(ordersViewModel.contains("fun advanceOrder(order: OrderRecord)"))
        assertTrue(ordersViewModel.contains("orderRepository.updateOrderStatus(order.id, nextStatus)"))
        assertTrue(ordersViewModel.contains("updatingOrderId"))

        assertTrue(repository.contains("CARETAKER_ROLE_REQUIRED"))
        assertTrue(repository.contains("preparing"))
        assertTrue(sql.contains("if caller_role <> 'caretaker'"))
        assertTrue(sql.contains("normalized_status = 'preparing'"))
        assertTrue(sql.contains("status in ('submitted', 'confirmed', 'preparing', 'delivering', 'completed', 'cancelled')"))
    }

    private fun readMainSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )

    private fun readProjectSource(relativePath: String): String = Files.readString(
        listOf(Paths.get(relativePath), Paths.get("..").resolve(relativePath)).first(Files::exists)
    )
}
