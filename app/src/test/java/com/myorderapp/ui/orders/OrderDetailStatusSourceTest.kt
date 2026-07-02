package com.myorderapp.ui.orders

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDetailStatusSourceTest {

    @Test
    fun `order detail can advance and cancel couple order status`() {
        val screen = readMainSource("ui/orders/OrderDetailScreen.kt")
        val viewModel = readMainSource("ui/orders/OrderDetailViewModel.kt")
        val repository = readMainSource("domain/repository/OrderRepository.kt")
        val dao = readMainSource("data/local/dao/OrderDao.kt")
        val supabaseRepository = readMainSource("data/repository/SupabaseOrderRepository.kt")

        listOf(
            "饲养员接单",
            "开始准备",
            "完成这顿饭",
            "取消订单",
            "知道了",
            "viewModel::advanceStatus",
            "viewModel::cancelOrder"
        ).forEach { expected ->
            assertTrue("订单详情缺少状态操作：$expected", screen.contains(expected))
        }

        listOf(
            "fun advanceStatus()",
            "fun cancelOrder()",
            "orderRepository.updateOrderStatus",
            "\"submitted\" -> \"confirmed\"",
            "\"confirmed\" -> \"delivering\"",
            "\"delivering\" -> \"completed\""
        ).forEach { expected ->
            assertTrue("订单详情 ViewModel 缺少状态流转：$expected", viewModel.contains(expected))
        }

        assertTrue(repository.contains("suspend fun updateOrderStatus(orderId: String, status: String)"))
        assertTrue(dao.contains("UPDATE orders SET status = :status WHERE id = :orderId"))
        assertTrue(supabaseRepository.contains("override suspend fun updateOrderStatus"))
        assertTrue(supabaseRepository.contains("mapOf(\"status\" to normalizedStatus)"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath).normalize(),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath).normalize()
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }
}
