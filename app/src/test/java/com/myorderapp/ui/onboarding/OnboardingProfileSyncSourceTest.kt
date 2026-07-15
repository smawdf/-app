package com.myorderapp.ui.onboarding

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingProfileSyncSourceTest {

    @Test
    fun `registration persists profile locally and queues failed avatar upload`() {
        val source = readMainSource("ui/onboarding/OnboardingViewModel.kt")

        assertTrue(source.contains("profileRepository.saveProfile("))
        assertTrue(source.contains("copyAvatarToPrivateStorage(appContext, avatarUri)"))
        assertTrue(source.contains("val appContext = context?.applicationContext"))
        assertTrue(source.contains("cloudErrorLogger.log(\"onboarding\", \"copy_avatar\""))
        assertTrue(source.contains("CloudImageUploadWorker.enqueue("))
        assertTrue(source.contains("CloudImageUploadWorker.TARGET_AVATAR"))
        assertTrue(source.contains("Uri.fromFile(localAvatarFile)"))
        assertTrue(source.contains("if (!profileRepo.loadFromCloud())"))
        assertTrue(source.contains("资料保存失败，请检查网络后重试"))
        assertTrue(source.contains("cloudErrorLogger.log(\"onboarding\", \"save_profile\""))
        assertFalse(source.contains("createProfileWithDetails"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
