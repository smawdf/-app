package com.myorderapp.ui.couple

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoupleMenuScreenSourceTest {

    @Test
    fun `couple menu home uses relationship identity sections`() {
        val source = readMainSource("ui/couple/CoupleMenuScreen.kt")

        listOf(
            "今天也要好好吃饭",
            "饲养员",
            "吃货",
            "一起吃饭",
            "选择身份",
            "已选择",
            "当前用户头像",
            "已切换为饲养员",
            "已切换为吃货",
            "现在可以上传菜单啦",
            "去点菜入口已开启",
            "负责上传菜单",
            "负责去点菜",
            "上传菜单",
            "去点菜",
            "另一半",
            "纪念日小日历"
        ).forEach { expected ->
            assertTrue("缺少情侣身份首页文案：$expected", source.contains(expected))
        }

        assertTrue(source.contains("rememberSaveable"))
        assertTrue(source.contains("getSharedPreferences(COUPLE_HOME_PREFS"))
        assertTrue(source.contains("putString(KEY_SELECTED_ROLE, role.storageKey)"))
        assertTrue(source.contains("prefs.getString(KEY_SELECTED_ROLE, null).toCoupleRole()"))
        assertTrue(source.contains("selectRole(CoupleRole.Caretaker)"))
        assertTrue(source.contains("selectRole(CoupleRole.Eater)"))
        assertTrue(source.contains("IdentitySwitchToast"))
        assertTrue(source.contains("RoleToastState"))
        assertTrue(source.contains("profileRepository.getProfile().collectAsState"))
        assertTrue(source.contains("daysEatingTogether(profile)"))
        assertTrue(source.contains("profile?.pairedAt"))
        assertTrue(source.contains("LaunchedEffect(toastState?.id)"))
        assertTrue(source.contains("delay(1500)"))
        assertTrue(source.contains(".align(Alignment.Center)"))
        assertTrue(source.contains("ToolInk.copy(alpha = 0.68f)"))
        assertTrue(source.contains("Color.Transparent"))
        assertFalse("弹框出现时不应模糊背景", source.contains(".blur("))
        assertFalse("身份切换提示不应再使用系统 Toast", source.contains("Toast.makeText"))
        assertTrue(source.contains("CurrentUserSlot(selectedRole = selectedRole)"))
        assertTrue(source.contains("RoleFunctionCard"))
        assertFalse("首页不应自己再画一条底部导航", source.contains("CoupleHomeBottomBar"))
        assertFalse("首页不应包含 VIP", source.contains("VIP"))
        assertFalse("首页不应包含小程序三点胶囊", source.contains("•••"))
        assertFalse("首页不应保留装扮入口", source.contains("装扮"))
        assertFalse("首页不应保留成就入口", source.contains("成就"))
        assertFalse("首页不应保留任务大厅游戏化入口", source.contains("任务大厅"))
        assertFalse("选择身份不应暗示直接去点菜", source.contains("前往点菜"))
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
