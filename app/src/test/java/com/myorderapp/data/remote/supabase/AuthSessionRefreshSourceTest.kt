package com.myorderapp.data.remote.supabase

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionRefreshSourceTest {

    @Test
    fun `cloud sync refreshes the real jwt before authenticated requests`() {
        val provider = readMain("data/remote/supabase/SupabaseClientProvider.kt")
        val coordinator = readMain("data/sync/CloudSyncCoordinator.kt")
        val profile = readMain("data/repository/SupabaseProfileRepository.kt")

        assertTrue(provider.contains("alwaysAutoRefresh = true"))
        assertTrue(provider.contains("jwtExpiresAtEpochSeconds"))
        assertTrue(provider.contains("auth.refreshCurrentSession()"))
        assertTrue(coordinator.contains("SupabaseClientProvider.ensureFreshAuthSession()"))
        assertTrue(profile.contains("refresh_auth_session"))
    }

    private fun readMain(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first(Files::exists))
    }
}
