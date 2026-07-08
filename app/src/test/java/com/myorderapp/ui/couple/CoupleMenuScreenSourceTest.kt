package com.myorderapp.ui.couple

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoupleMenuScreenSourceTest {

    @Test
    fun `couple menu home uses native Stitch relationship and role sections`() {
        val source = readMainSource("ui/couple/CoupleMenuScreen.kt")

        listOf(
            "CozyPage",
            "RelationshipCard",
            "QuickActionGrid",
            "RoleSwitcher",
            "LatestOrderNudge",
            "HomeDecorativeBubbles",
            "RoleLabelPill",
            "CozyPage(decorative = false)",
            "CozySurface.copy(alpha = 0.94f)",
            ".size(84.dp)",
            "vertical: Boolean = false",
            "vertical = true",
            "showPetFallback = true",
            "今天也要一起好好吃饭",
            "饲养员",
            "吃货",
            "一起吃饭",
            "选择身份",
            "当前用户",
            "邀请对方",
            "已切换为",
            "上传菜单",
            "去点菜",
            "当前：",
            "切换为",
            "点了 \$dishCount 道菜，等你开做",
            "点菜单已送达，等饲养员接单"
        ).forEach { expected ->
            assertTrue("Missing native couple home marker: $expected", source.contains(expected))
        }

        assertTrue(source.contains("rememberSaveable"))
        assertTrue("首页顶部栏应固定在滚动内容外", source.indexOf("HomeHeader()") < source.indexOf("verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("getSharedPreferences(COUPLE_HOME_PREFS"))
        assertTrue(source.contains("putString(KEY_SELECTED_ROLE, role.storageKey)"))
        assertTrue(source.contains("prefs.getString(KEY_SELECTED_ROLE, null).toCoupleRole()"))
        assertTrue(source.contains("selectRole(CoupleRole.Caretaker)"))
        assertTrue(source.contains("selectRole(CoupleRole.Eater)"))
        assertTrue(source.contains("IdentitySwitchToast"))
        assertTrue(source.contains("shadowElevation = 0.dp"))
        assertTrue("当前用户槽位应足够展示 当前角色：饲养员", source.contains("Modifier.width(128.dp)"))
        assertTrue("饲养员/吃货角色切换卡片应为正方形，避免文字被遮挡", source.contains("modifier.aspectRatio(1f)"))
        assertTrue("首页顶部标题应和全局顶部栏一样使用主色加粗", source.contains("color = CozyRose") && source.contains("fontWeight = FontWeight.Black"))
        assertTrue(source.contains("RoleToastState"))
        assertTrue(source.contains("profileRepository.getProfile().collectAsState"))
        assertTrue(source.contains("profileRepository.getPairInfo()"))
        assertTrue(source.contains("orderRepository.observeOrders().collectAsState"))
        assertTrue(source.contains("orderRepository.refreshOrders()"))
        assertTrue(source.contains("activeOrderStatuses"))
        assertTrue(source.contains("currentUserId = profile?.userId.orEmpty()"))
        assertTrue(source.contains("val isMyOrder = currentUserId.isNotBlank() && order.userId == currentUserId"))
        assertTrue(source.contains("onClick = onOrdersClick"))
        assertTrue(source.contains("PairInfo"))
        assertTrue(source.contains("PartnerSlot"))
        assertTrue(source.contains("pairInfo.isPaired"))
        assertTrue(source.contains("pairInfo.isOnline"))
        assertTrue(source.contains("pairInfo.partnerName.ifBlank"))
        assertTrue(source.contains("daysEatingTogether(profile)"))
        assertTrue(source.contains("profile?.pairedAt"))
        assertTrue(source.contains("LaunchedEffect(toastState?.id)"))
        assertTrue(source.contains("delay(1600)"))
        assertTrue(source.contains("AsyncImage("))
        assertTrue(source.contains("ContentScale.Crop"))
        assertTrue(source.contains("profile?.nickname?.takeIf"))
        assertTrue(source.contains("今天也想和你好好吃饭"))
        assertTrue(source.contains("我的店铺"))
        assertTrue(source.contains("看看今天想吃什么"))
        assertFalse(source.contains("Toast.makeText"))
        assertFalse(source.contains("CoupleHomeBottomBar"))
        assertFalse(source.contains("RoleFunctionCard"))
        assertFalse(source.contains("WebView"))
        assertFalse(source.contains("StitchScreen"))
        assertFalse(source.contains("VIP"))
        assertFalse(source.contains("TA"))
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
