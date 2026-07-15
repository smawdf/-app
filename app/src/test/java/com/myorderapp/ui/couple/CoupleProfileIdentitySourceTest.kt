package com.myorderapp.ui.couple

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CoupleProfileIdentitySourceTest {
    @Test
    fun `home uses profile nickname and partner cloud avatar`() {
        val screen = readSource("ui/couple/CoupleMenuScreen.kt")
        val profile = readSource("domain/model/Profile.kt")
        val repository = readSource("data/repository/SupabaseProfileRepository.kt")

        assertTrue(screen.contains("profile?.nickname?.takeIf { it.isNotBlank() }"))
        assertTrue(screen.contains("text = name"))
        assertTrue(screen.contains("maxLines = 2"))
        assertTrue(screen.contains("avatarUrl = pairInfo.partnerAvatarUrl"))
        assertTrue(profile.contains("val partnerAvatarUrl: String? = null"))
        assertTrue(repository.contains("partnerAvatarUrl = partnerAvatarUrl.takeIfCloudAvatarUrl()"))
        assertTrue(repository.contains("partnerAvatarUrl = partner?.avatarUrl?.takeIfCloudAvatarUrl()"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )
}
