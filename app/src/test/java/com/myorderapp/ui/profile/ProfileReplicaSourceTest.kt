package com.myorderapp.ui.profile

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileReplicaSourceTest {

    @Test
    fun `profile screen is native personal center without old membership history or tutorial entries`() {
        val source = readMainSource("ui/profile/ProfileScreen.kt")

        listOf(
            "CozyPage",
            "CozyPage(decorative = false)",
            "ImmersiveProfileHeader",
            "ProfileHeader",
            "SimulatedCurrencyBalanceCard",
            "ProfileActionRow",
            "LogoutButton",
            "SecondaryContainer.copy(alpha = 0.16f)",
            "BorderStroke(1.dp, OutlineVariant)",
            ".offset(x = (-36).dp, y = (-38).dp)",
            ".weight(1f)",
            ".clipToBounds()",
            "contentPadding = PaddingValues(bottom = navigationBottomPadding + 132.dp)",
            "糯米小狗",
            "账号设置",
            "帮助与客服",
            "我的店铺",
            "订单记录",
            "onOrdersClick",
            "设置头像和昵称",
            "ProfileEditDialog",
            "saveProfileEdits"
        ).forEach { expected ->
            assertTrue("Profile page missing function copy: $expected", source.contains(expected))
        }
        val mainSource = readMainSource("MainActivity.kt")
        assertTrue("个人中心顶部栏应由全局壳固定在内容外", mainSource.contains("Routes.PROFILE -> \"个人中心\""))
        assertTrue(mainSource.contains("padding(top = if (showMainShell) mainTopBarHeight else 0.dp)"))
        assertFalse("Profile page should avoid intentional shadow styling", source.contains(".shadow("))

        listOf(
            "会员",
            "饭票",
            "VIP",
            "MemberUnlockCard",
            "TicketCard",
            "联系客服",
            "QuestionAnswer",
            "浏览历史",
            "新手教程",
            "WebView"
        ).forEach { forbidden ->
            assertFalse("Profile page should not contain: $forbidden", source.contains(forbidden))
        }
        assertFalse("Profile top bar should be fixed outside the scrolling list", source.contains("item { ProfileTopBar() }"))
    }

    @Test
    fun `profile screen supports gallery avatar picker and visible logout action`() {
        val source = readMainSource("ui/profile/ProfileScreen.kt")
        val viewModel = readMainSource("ui/profile/ProfileViewModel.kt")

        listOf(
            "ActivityResultContracts.GetContent()",
            "avatarPicker.launch(\"image/*\")",
            "从相册选择头像",
            "头像从本地相册选择",
            "选择后先预览，保存资料后生效",
            "保存资料",
            "LogoutButton",
            "LogoutConfirmDialog",
            "确认要离开吗？",
            "小狗会想念你的哦...",
            "再留一会",
            "狠心退出",
            "退出登录",
            "Color(0xFFBA1A1A)",
            "authViewModel.logout(onLoggedOut = onLoginClick)"
        ).forEach { expected ->
            assertTrue("Profile page missing gallery avatar or logout ability: $expected", source.contains(expected))
        }

        assertFalse(source.contains("头像图片链接"))
        assertTrue(viewModel.contains("saveAvatarUri(context: Context, uri: Uri)"))
        assertTrue(viewModel.contains("copy(avatarUrl = localPath)"))
        assertTrue(viewModel.contains("saveProfileEdits(context: Context, nickname: String, avatarUri: Uri?)"))
        assertFalse(viewModel.contains("customTags"))
        assertFalse(viewModel.contains("newTag"))
        assertFalse(viewModel.contains("fun addTag()"))

        val authViewModel = readMainSource("ui/auth/AuthViewModel.kt")
        val supabaseRepository = readMainSource("data/repository/SupabaseProfileRepository.kt")
        assertTrue(authViewModel.contains("session.clear()"))
        assertTrue(authViewModel.contains("onLoggedOut()"))
        assertTrue(supabaseRepository.contains("session.saveNickname(updated.nickname)"))
        assertTrue(supabaseRepository.contains("session.saveAvatar(updated.avatarUrl ?: \"\")"))
    }

    @Test
    fun `profile page keeps Stitch page 5 actions only while pairing backend remains available`() {
        val source = readMainSource("ui/profile/ProfileScreen.kt")

        listOf(
            "Icons.Filled.Storefront",
            "邀请对方",
            "PairManagementDialog",
            "Icons.Filled.PersonAdd",
            "viewModel.generatePairCode(selectedRoleKey)",
            "viewModel.previewPairInvite(uiState.joinPairCode)",
            "rolePrefs.edit().putString(KEY_SELECTED_ROLE, role).apply()",
            "rolePrefs.edit().remove(KEY_SELECTED_ROLE).apply()",
            "viewModel::onJoinPairCodeChanged",
            "viewModel.joinPair(uiState.joinPairCode, uiState.invitePreview?.inviteeRole)"
        ).forEach { expected ->
            assertTrue("Profile page missing Stitch action affordance: $expected", source.contains(expected))
        }

        listOf(
            "订单通知",
            "邀请小伙伴",
            "KEY_ORDER_NOTIFICATIONS_ENABLED",
            "Icons.Filled.Notifications"
        ).forEach { forbidden ->
            assertFalse("Profile page should match Stitch page 5 and not show: $forbidden", source.contains(forbidden))
        }

        val viewModel = readMainSource("ui/profile/ProfileViewModel.kt")
        val supabaseRepository = readMainSource("data/repository/SupabaseProfileRepository.kt")
        val inMemoryRepository = readMainSource("data/repository/InMemoryProfileRepository.kt")
        val supabaseGeneratePairCode = functionBody(supabaseRepository, "generatePairCode")
        val inMemoryGeneratePairCode = functionBody(inMemoryRepository, "generatePairCode")
        assertTrue(viewModel.contains("profileRepository.getPairInfo()"))
        assertTrue(viewModel.contains("fun previewPairInvite(code: String)"))
        assertTrue(viewModel.contains("fun joinPair(code: String, inviteeRole: String? = null, onSuccess: (String) -> Unit = {})"))
        assertTrue(viewModel.contains("onSuccess(roleToSave)"))
        assertTrue(viewModel.contains("pairCode = code"))
        assertTrue(viewModel.contains("invitePreview = null"))
        assertTrue(viewModel.contains("请先在首页选择饲养员或吃货"))
        assertTrue(viewModel.contains("未找到有效邀请，请确认对方已选择身份并生成邀请码"))
        assertTrue(supabaseRepository.contains("KEY_PENDING_PAIR_CODE"))
        assertTrue(supabaseRepository.contains("override suspend fun saveSelectedRole(role: String?)"))
        assertTrue(supabaseRepository.contains("selected_role"))
        assertTrue(supabaseRepository.contains("override suspend fun previewPairInvite(code: String): PairInvitePreview?"))
        assertTrue(inMemoryRepository.contains("pendingPairCode = code"))
        assertTrue(supabaseGeneratePairCode.contains("copy(pairId = code"))
        assertTrue(inMemoryGeneratePairCode.contains("copy(pairId = code"))
        assertTrue(supabaseGeneratePairCode.contains("saveSelectedRole(normalizedRole)"))
        assertTrue(supabaseGeneratePairCode.contains("setPairId(code"))
        assertTrue(supabaseRepository.contains("copy(pairId = normalizedCode"))
        assertTrue(inMemoryRepository.contains("copy(pairId = code.uppercase()"))
    }

    private fun functionBody(source: String, functionName: String): String {
        val start = source.indexOf("fun $functionName")
        require(start >= 0) { "Function not found: $functionName" }
        val bodyStart = source.indexOf('{', start)
        require(bodyStart >= 0) { "Function body not found: $functionName" }
        var depth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(bodyStart + 1, index)
                }
            }
        }
        error("Function body not closed: $functionName")
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
