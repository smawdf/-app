package com.myorderapp.ui.notifications

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderNotificationSourceTest {

    @Test
    fun `order notifications are gated by user setting and android permission`() {
        val manifest = readSource("app/src/main/AndroidManifest.xml", "src/main/AndroidManifest.xml")
        val helper = readMainSource("ui/notifications/OrderNotificationHelper.kt")
        val home = readMainSource("ui/couple/CoupleMenuScreen.kt")
        val main = readMainSource("MainActivity.kt")

        listOf(
            "android.permission.POST_NOTIFICATIONS",
            "NotificationChannel",
            "NotificationCompat.Builder",
            "notifyActiveOrderIfAllowed",
            "KEY_ORDER_NOTIFICATIONS_ENABLED",
            "KEY_LAST_NOTIFIED_ORDER_ID",
            "有新的点菜单等你确认",
            "这顿饭正在准备中"
        ).forEach { expected ->
            assertTrue("缺少订单通知能力：$expected", (manifest + helper + home).contains(expected))
        }

        listOf(
            "Manifest.permission.POST_NOTIFICATIONS",
            "ContextCompat.checkSelfPermission",
            "PackageManager.PERMISSION_GRANTED"
        ).forEach { expected ->
            assertTrue("通知 helper 缺少系统权限门禁：$expected", helper.contains(expected))
        }

        listOf(
            "ActivityResultContracts.RequestPermission()",
            "notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)",
            "Build.VERSION_CODES.TIRAMISU"
        ).forEach { expected ->
            assertTrue("主流程缺少通知权限申请：$expected", main.contains(expected))
        }
    }

    private fun readMainSource(relativePath: String): String =
        readSource("app/src/main/java/com/myorderapp/$relativePath", "src/main/java/com/myorderapp/$relativePath")

    private fun readSource(vararg paths: String): String {
        val sourcePath = paths.map { Paths.get(it) }.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: ${paths.joinToString()}")
        return Files.readString(sourcePath)
    }
}
