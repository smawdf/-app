package com.myorderapp.ui.orders

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
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
            "订单详情",
            "返回",
            "暂无备注",
            "订单进度",
            "饲养员接单",
            "开始准备",
            "完成这顿饭",
            "取消订单",
            "知道了",
            "viewModel.advanceStatus(canAdvance = canAdvanceOrder)",
            "viewModel::cancelOrder"
        ).forEach { expected ->
            assertTrue("订单详情缺少状态操作：$expected", screen.contains(expected))
        }

        listOf(
            "COUPLE_HOME_PREFS",
            "KEY_SELECTED_ROLE",
            "ROLE_CARETAKER",
            "canAdvanceOrder",
            "enabled = canAdvanceOrder",
            "只有饲养员可以更新订单进度",
            "请先在首页切换为饲养员，再处理这份点菜单"
        ).forEach { expected ->
            assertTrue("订单详情缺少饲养员权限控制：$expected", screen.contains(expected))
        }

        listOf(
            "fun advanceStatus(canAdvance: Boolean = true)",
            "if (!canAdvance)",
            "只有饲养员可以更新订单进度",
            "fun cancelOrder()",
            "orderRepository.updateOrderStatus",
            "\"submitted\" -> \"confirmed\"",
            "\"confirmed\" -> \"delivering\"",
            "\"delivering\" -> \"completed\"",
            "饲养员已接单",
            "开始准备今天的饭",
            "这顿饭已完成",
            "订单状态已更新",
            "订单已取消"
        ).forEach { expected ->
            assertTrue("订单详情 ViewModel 缺少状态流转：$expected", viewModel.contains(expected))
        }

        listOf("楗", "鍚", "璁", "閰", "鈥").forEach { mojibake ->
            assertFalse("订单详情页不应包含乱码片段：$mojibake", screen.contains(mojibake))
            assertFalse("订单详情 ViewModel 不应包含乱码片段：$mojibake", viewModel.contains(mojibake))
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
