package com.myorderapp.data.sync

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncSerializationSourceTest {

    @Test
    fun `full cloud sync calls are serialized`() {
        val source = readMainSource("data/sync/CloudSyncCoordinator.kt")

        assertTrue(source.contains("private val syncMutex = Mutex()"))
        assertTrue(source.contains("suspend fun syncAll() = syncMutex.withLock"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
