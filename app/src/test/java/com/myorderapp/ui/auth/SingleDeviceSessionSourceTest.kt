package com.myorderapp.ui.auth

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleDeviceSessionSourceTest {

    @Test
    fun `login claims one device and blocks active sessions`() {
        val authViewModel = readMainSource("ui/auth/AuthViewModel.kt")
        val profileRepository = readMainSource("data/repository/SupabaseProfileRepository.kt")

        listOf(
            "profileRepo.canStartDeviceSession(profile)",
            "profileRepo.claimCurrentDeviceSession(profile)",
            "登录失败：无法读取账号设备状态，请稍后重试",
            "登录失败：无法绑定当前设备，请稍后重试",
            "账号正在其他设备使用。可在原设备退出",
            "手机号/普通账号暂不支持邮箱接管",
            "sendDeviceSwitchEmail",
            "DEVICE_SWITCH_REDIRECT_URL",
            "profileRepo.releaseCurrentDeviceSession()"
        ).forEach { expected ->
            assertTrue("single-device auth flow missing: $expected", authViewModel.contains(expected))
        }

        listOf(
            "DEVICE_SESSION_TIMEOUT: Duration = Duration.ofDays(30)",
            "session_id",
            "session_updated_at",
            "claimCurrentDeviceSession",
            "return null",
            "releaseCurrentDeviceSession",
            "checkSessionValid"
        ).forEach { expected ->
            assertTrue("single-device repository guard missing: $expected", profileRepository.contains(expected))
        }
    }

    @Test
    fun `device switch deep link is wired into app shell`() {
        val manifest = readMainResource("AndroidManifest.xml")
        val navGraph = readMainSource("ui/navigation/NavGraph.kt")
        val mainActivity = readMainSource("MainActivity.kt")
        val authScreen = readMainSource("ui/auth/AuthScreen.kt")

        assertTrue(manifest.contains("android:path=\"/switch-device\""))
        assertTrue(navGraph.contains("DEVICE_SWITCH"))
        assertTrue(navGraph.contains("DeviceSwitchScreen"))
        assertTrue(mainActivity.contains("orderdisk://auth/switch-device"))
        assertTrue(mainActivity.contains("profileRepository.checkSessionValid()"))
        assertTrue(authScreen.contains("发送邮箱验证，切换到当前设备"))
        assertTrue(authScreen.contains("手机号当前按账号密码登录，不会发送短信验证码。"))
    }

    @Test
    fun `single device database migration contains required session columns`() {
        val sqlPath = listOf(
            Paths.get("table/16_single_device_session.sql"),
            Paths.get("../table/16_single_device_session.sql")
        ).firstOrNull { Files.exists(it) } ?: error("SQL migration not found")
        val sql = Files.readString(sqlPath)

        assertTrue(sql.contains("add column if not exists session_id text not null default ''"))
        assertTrue(sql.contains("add column if not exists session_updated_at text not null default ''"))
        assertTrue(sql.contains("idx_profiles_session_id"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("src/main/java").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }

    private fun readMainResource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main").resolve(relativePath),
            Paths.get("app/src/main").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Main resource not found: $relativePath")
        return Files.readString(sourcePath)
    }
}
