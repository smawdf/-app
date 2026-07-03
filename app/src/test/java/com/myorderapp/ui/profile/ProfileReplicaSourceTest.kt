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
            "重新生成邀请码",
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

        val viewModel = readMainSource("ui/profile/ProfileViewModel.kt")
        val supabaseRepository = readMainSource("data/repository/SupabaseProfileRepository.kt")
        val inMemoryRepository = readMainSource("data/repository/InMemoryProfileRepository.kt")
        assertTrue("生成邀请码后应刷新伴侣状态", viewModel.contains("profileRepository.getPairInfo()"))
        assertTrue("生成邀请码后应保留可复制的绑定码", viewModel.contains("pairCode = code"))
        assertTrue("生成邀请码后应提示用户发给对方", viewModel.contains("邀请码已生成，可以发给对方"))
        assertTrue("云端仓储生成邀请码时应保存 pairId", supabaseRepository.contains("copy(pairId = code"))
        assertTrue("本地仓储生成邀请码时应保存 pairId", inMemoryRepository.contains("copy(pairId = code"))
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
