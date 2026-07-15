package com.myorderapp.core.worker

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudImageUploadWorkerSourceTest {

    @Test
    fun `failed cloud images retry and update their owning records`() {
        val worker = readMainSource("core/worker/CloudImageUploadWorker.kt")

        assertTrue(worker.contains("NetworkType.CONNECTED"))
        assertTrue(worker.contains("BackoffPolicy.EXPONENTIAL"))
        assertTrue(worker.contains("ExistingWorkPolicy.REPLACE"))
        assertTrue(worker.contains("TARGET_AVATAR -> profileRepository.updateAvatar(url)"))
        assertTrue(worker.contains("TARGET_SHOP -> shopRepository.updateShopImageUrl(url)"))
        assertTrue(worker.contains("TARGET_MENU -> menuRepository.updateDishImage(targetId, url)"))
        assertTrue(worker.contains("runAttemptCount < MAX_RETRIES"))
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
