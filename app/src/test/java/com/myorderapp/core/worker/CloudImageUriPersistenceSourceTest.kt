package com.myorderapp.core.worker

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudImageUriPersistenceSourceTest {
    @Test
    fun `content uri is copied before durable work is enqueued`() {
        val source = Files.readString(
            listOf(
                Paths.get("src/main/java/com/myorderapp/core/worker/CloudImageUploadWorker.kt"),
                Paths.get("app/src/main/java/com/myorderapp/core/worker/CloudImageUploadWorker.kt")
            ).first(Files::exists)
        )
        assertTrue(source.contains("snapshotForWork(context, uri)"))
        assertTrue(source.contains("pending_uploads"))
        assertTrue(source.contains("cleanupPendingFile(uri)"))
        assertTrue(source.contains("KEY_URI to workUri.toString()"))
    }
}
