package com.myorderapp.ui.orders

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersReplicaSourceTest {

    @Test
    fun `orders screen follows Stitch page 4 tabs and order cards`() {
        val source = readMainSource("ui/orders/OrdersScreen.kt")

        listOf(
            "订单 - 甜蜜点菜记录",
            "OrderFilter.ALL -> uiState.orders",
            "OrderFilter.PENDING -> uiState.orders.filter",
            "OrderFilter.COMPLETED -> uiState.orders.filter",
            "OrdersFilterTabs",
            "HandDrawnTab",
            "StitchOrderCard",
            "SquishyOrderActionButton",
            "去准备",
            "暂无订单哦~",
            "暂时没有待确认订单",
            "还没有完成的点菜记录",
            "statusBarsPadding()",
            "OrdersTopBar()",
            "OrderSurface.copy(alpha = 0.94f)",
            "border = BorderStroke(1.dp, Secondary.copy(alpha = 0.58f))",
            ".weight(1f)",
            "shadowElevation = 0.dp"
        ).forEach { expected ->
            assertTrue("订单页缺少 page_4 原型结构或文案：$expected", source.contains(expected))
        }

        assertTrue("订单页顶部栏应固定在滚动内容外", source.indexOf("OrdersTopBar()") < source.indexOf("LazyColumn("))
        assertFalse("page_4 原型不包含日历筛选", source.contains("showCalendarFilter"))
        assertFalse("page_4 原型不包含美食日记模式", source.contains("FoodDiaryRecordCard"))
        assertFalse("page_4 原型不包含厨房/日记切换", source.contains("MiniModeChip"))
        assertFalse("订单页不应包含小程序三点胶囊", source.contains("•••"))
        assertFalse("订单页不应包含 MiniProgramHeader", source.contains("MiniProgramHeader"))
    }

    @Test
    fun `orders wording matches couple dining instead of delivery platform`() {
        val ordersSource = readMainSource("ui/orders/OrdersScreen.kt")
        val cartSheetSource = readMainSource("ui/shop/components/CartSheet.kt")

        assertTrue("订单准备状态应显示为准备中", ordersSource.contains("\"delivering\" -> \"准备中\""))
        assertFalse("订单页不应显示外卖配送状态", ordersSource.contains("配送中"))
        assertFalse("购物车浮层不应显示平台化服务费", cartSheetSource.contains("服务费"))
        assertFalse("购物车不应显示外卖配送费", cartSheetSource.contains("配送费"))
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
