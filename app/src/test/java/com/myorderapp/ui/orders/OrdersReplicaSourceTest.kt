package com.myorderapp.ui.orders

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdersReplicaSourceTest {

    @Test
    fun `orders screen uses real date calendar and empty states`() {
        val source = readMainSource("ui/orders/OrdersScreen.kt")

        listOf(
            "订单",
            "一共记录了",
            "个订单",
            "个日记",
            "厨房订单",
            "美食日记",
            "YearMonth.now()",
            "LocalDate.now()",
            "displayedMonth.toChineseMonthText()",
            "当前筛选：",
            "回到今天",
            "暂无订单哦~",
            "暂无日记哦~"
        ).forEach { expected ->
            assertTrue("缺少订单页真实日期功能文案：$expected", source.contains(expected))
        }

        assertFalse("订单页不应使用固定假月份", source.contains("2026年6月"))
        assertFalse("订单页不应包含小程序三点胶囊", source.contains("•••"))
        assertFalse("订单页不应包含 MiniProgramHeader", source.contains("MiniProgramHeader"))
    }

    @Test
    fun `orders calendar filter is collapsed until the user taps date entry`() {
        val source = readMainSource("ui/orders/OrdersScreen.kt")

        assertTrue("订单页应使用显式状态控制日期筛选展开", source.contains("showCalendarFilter"))
        assertTrue("订单页应提供点击入口展开日期筛选", source.contains("onCalendarClick"))
        assertTrue("日期筛选卡片应只在 showCalendarFilter 为 true 时渲染", source.contains("if (showCalendarFilter)"))
        assertTrue("日期筛选应支持切换月份", source.contains("onPreviousMonth"))
        assertTrue("日期筛选应支持选择具体日期", source.contains("onDateSelected"))
        assertFalse("日期筛选卡片不应作为默认 item 无条件渲染", source.contains("item {\n            CalendarFilterCard()"))
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
