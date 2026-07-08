package com.myorderapp.ui.couple

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnniversaryReplicaSourceTest {

    @Test
    fun `anniversary page keeps native Stitch structure and Chinese copy`() {
        val source = readMainSource("ui/couple/AnniversaryScreen.kt")

        listOf(
            "AnniversaryHeader",
            "AnniversaryHeroCard",
            "NextAnniversaryCard",
            "CalendarCard",
            "CalendarTypeToggle",
            "SweetMomentsTimeline",
            "SweetMomentItem",
            "StitchGlassCard",
            "drawNoodleBowl",
            "verticalArrangement = Arrangement.spacedBy(16.dp)",
            "Spacer(modifier = Modifier.height(24.dp))",
            "contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)",
            "height(126.dp)",
            "height(42.dp)",
            "heightIn(min = 114.dp)",
            "outline = true",
            "shadowElevation = 0.dp",
            "Icons.Filled.Cake",
            "Color(0xFFFFEFF3).copy(alpha = 0.46f)",
            "Color(0xFFFFE7EC).copy(alpha = 0.54f)",
            "AnniversaryCard.copy(alpha = 0.58f)",
            "AnniversaryBorder.copy(alpha = 0.48f)",
            "contentPadding = PaddingValues(horizontal = 22.dp, vertical = 22.dp)",
            "modifier = Modifier.size(82.dp)",
            "fontSize = 17.sp, lineHeight = 28.sp",
            "纪念日",
            "恋爱第",
            "每一次心跳，都在为你倒数",
            "距离下一站浪漫",
            "阳历",
            "农历",
            "甜蜜时刻",
            "还没有记录",
            "完成点菜后，可以在这里沉淀你们自己的甜蜜时刻。",
            "支持文字输入，也可以在日历里选择日期。",
            "保存纪念日"
        ).forEach { expected ->
            assertTrue("Missing anniversary replica marker: $expected", source.contains(expected))
        }

        listOf(
            "\u7efe",
            "\u939d",
            "\u7490\u6fe0",
            "\u95c3\u51b2\u5dfb",
            "\u9350\u6ed3\u5dfb",
            "\u9422\u6ed9\u6e5d",
            "\u6dc7\u6fee\u74e8"
        ).forEach { mojibake ->
            assertFalse("Anniversary page should not contain mojibake: $mojibake", source.contains(mojibake))
        }

        assertTrue(source.contains("profileRepository.saveProfile(current.copy(pairedAt = date.toString()))"))
        assertTrue(source.contains("android.icu.util.ChineseCalendar"))
        assertTrue(source.contains("parseAnniversaryInput"))
        assertTrue(source.contains("solarToLunar"))
        assertTrue(source.contains("lunarToSolar"))
        assertFalse("Anniversary page should not show the full calendar immediately on entry", source.contains("YearMonth.from(state.startDate)"))
        assertTrue(source.contains("AnniversaryPrimary.copy(alpha = 0.08f)"))
        assertTrue(source.contains("NextAnniversaryCard(state = state, onClick = { showEditor = true })"))
        assertTrue(source.contains("Icons.AutoMirrored.Filled.ArrowBack"))
        assertTrue(source.contains("contentDescription = \"返回\""))
        assertFalse("纪念日页顶部不应出现 Stitch 截图没有的设置按钮", source.contains("text = \"设置\""))
        assertFalse("纪念日页应使用原型的返回箭头，不使用关闭 X", source.contains("Icons.Filled.Close"))
        assertFalse(source.contains("WebView"))
        assertFalse(source.contains("StitchScreen"))
        assertFalse(source.contains("<html"))
        assertFalse(source.contains("OrderDisk"))
        assertFalse(source.contains("第一次打卡这家火锅店，很辣但很开心！"))
        assertFalse(source.contains("今天的家常菜很好吃，特别是西红柿炒蛋。"))
        assertFalse(source.contains("周末早晨的咖啡，阳光刚刚好。"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath from ${Paths.get("").toAbsolutePath()}")
        return Files.readString(sourcePath)
    }
}
