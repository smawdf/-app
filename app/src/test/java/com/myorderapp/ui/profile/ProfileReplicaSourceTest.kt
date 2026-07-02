package com.myorderapp.ui.profile

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileReplicaSourceTest {

    @Test
    fun `profile screen is personal center without old membership history or tutorial entries`() {
        val source = readMainSource("ui/profile/ProfileScreen.kt")

        listOf(
            "未设置昵称",
            "订单通知设置",
            "邀请小伙伴",
            "厨房设置",
            "设置头像和名称",
            "ProfileEditDialog",
            "updateNickname",
            "updateAvatar"
        ).forEach { expected ->
            assertTrue("缺少我的页功能文案：$expected", source.contains(expected))
        }

        listOf(
            "会员",
            "饭票",
            "VIP",
            "MemberUnlockCard",
            "TicketCard",
            "联系客服",
            "QuestionAnswer",
            "onLogoutClick",
            "浏览历史",
            "新手教程"
        ).forEach { forbidden ->
            assertFalse("我的页不应该包含：$forbidden", source.contains(forbidden))
        }
    }

    @Test
    fun `profile screen keeps sharing and shop management affordances`() {
        val source = readMainSource("ui/profile/ProfileScreen.kt")

        listOf(
            "已开启",
            "未开启",
            "KEY_ORDER_NOTIFICATIONS_ENABLED",
            "getSharedPreferences(PROFILE_PREFS",
            "putBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED",
            "PairManagementDialog",
            "生成邀请码",
            "输入 6 位绑定码",
            "解除绑定",
            "viewModel::generatePairCode",
            "viewModel.joinPair(uiState.joinPairCode)",
            "viewModel::unpair",
            "Icons.Outlined.NotificationsNone",
            "Icons.Outlined.PersonAdd",
            "Icons.Outlined.Storefront"
        ).forEach { expected ->
            assertTrue("缺少我的页入口或状态：$expected", source.contains(expected))
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
