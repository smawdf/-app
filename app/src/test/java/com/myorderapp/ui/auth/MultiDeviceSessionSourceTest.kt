package com.myorderapp.ui.auth

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiDeviceSessionSourceTest {
    @Test
    fun `login does not claim or evict another device`() {
        val auth = readSource("ui/auth/AuthViewModel.kt")
        val main = readSource("MainActivity.kt")
        val screen = readSource("ui/auth/AuthScreen.kt")

        assertFalse(auth.contains("claimCurrentDeviceSession"))
        assertFalse(auth.contains("releaseCurrentDeviceSession"))
        assertFalse(main.contains("checkSessionValid"))
        assertFalse(main.contains("DEVICE_SESSION_CHECK_INTERVAL_MS"))
        assertTrue(screen.contains("可在多台设备登录"))
    }

    @Test
    fun `background work remains bound to its local account`() {
        val imageWorker = readSource("core/worker/CloudImageUploadWorker.kt")
        val orderWorker = readSource("core/worker/OrderSyncWorker.kt")
        listOf(imageWorker, orderWorker).forEach { worker ->
            assertTrue(worker.contains("session.currentUserId != expectedUserId"))
            assertTrue(worker.contains("session.currentSessionId != expectedSessionId"))
            assertFalse(worker.contains("checkSessionValid"))
        }
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )
}
