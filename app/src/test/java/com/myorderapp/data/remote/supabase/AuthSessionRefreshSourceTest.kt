package com.myorderapp.data.remote.supabase

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionRefreshSourceTest {

    @Test
    fun `cloud sync refreshes the real jwt before authenticated requests`() {
        val provider = readMain("data/remote/supabase/SupabaseClientProvider.kt")
        val coordinator = readMain("data/sync/CloudSyncCoordinator.kt")
        val profile = readMain("data/repository/SupabaseProfileRepository.kt")
        val main = readMain("MainActivity.kt")
        val menu = readMain("data/repository/RoomMenuRepository.kt")
        val profileViewModel = readMain("ui/profile/ProfileViewModel.kt")

        assertTrue(provider.contains("alwaysAutoRefresh = true"))
        assertTrue(provider.contains("jwtExpiresAtEpochSeconds"))
        assertTrue(provider.contains("auth.refreshCurrentSession()"))
        assertTrue(coordinator.contains("SupabaseClientProvider.ensureFreshAuthSession()"))
        assertTrue(coordinator.contains("client.auth.awaitInitialization()"))
        assertTrue(coordinator.contains("requiresLogin = true"))
        assertTrue(coordinator.contains("SessionStatus.NetworkError"))
        assertTrue(profile.contains("refresh_auth_session"))
        assertFalse(main.contains("cloudSyncState.requiresLogin"))
        assertFalse(main.contains("sessionManager.clear()"))
        assertTrue(menu.contains("flatMapLatest(menuDishDao::observeByPair)"))
        assertTrue(menu.contains("combine(currentSession.pairId, currentSession.userId)"))
        val joined = profileViewModel.indexOf("val success = profileRepository.joinPair(code)")
        val syncAfterJoin = profileViewModel.indexOf("cloudSyncCoordinator.syncAll()", joined)
        assertTrue(joined >= 0 && syncAfterJoin > joined)
    }

    private fun readMain(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first(Files::exists))
    }
}
