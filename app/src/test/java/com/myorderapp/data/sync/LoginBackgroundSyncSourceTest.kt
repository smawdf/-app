package com.myorderapp.data.sync

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginBackgroundSyncSourceTest {
    @Test
    fun `login completes before cloud sync and failures are area scoped`() {
        val auth = readSource("ui/auth/AuthViewModel.kt")
        val main = readSource("MainActivity.kt")
        val coordinator = readSource("data/sync/CloudSyncCoordinator.kt")
        val logger = readSource("data/remote/supabase/CloudErrorLogger.kt")

        val loggedIn = auth.indexOf("isLoggedIn = true, isLoading = false")
        val backgroundSync = auth.indexOf("cloudSyncCoordinator.syncInBackground()", loggedIn)
        assertTrue(loggedIn >= 0 && backgroundSync > loggedIn)
        assertFalse(auth.contains("cloudSyncCoordinator.syncAll()"))
        assertTrue(main.contains("restoredSessionAtStartup"))
        assertTrue(main.contains("cloudSyncCoordinator.syncInBackground()"))
        assertTrue(coordinator.contains("currentErrorSequence(errorArea)"))
        assertTrue(logger.contains("areaErrorSequences"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )
}
