package com.myorderapp.data.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudSyncCoordinatorTest {
    @Test
    fun tasksContinueAfterFailureAndReportProgress() = runBlocking {
        val executed = mutableListOf<String>()
        val progress = mutableListOf<Int>()
        val failures = runCloudSyncTasks(
            tasks = listOf(
                CloudSyncTask("profile") { executed += "profile" },
                CloudSyncTask("shop") {
                    executed += "shop"
                    error("offline")
                },
                CloudSyncTask("orders") { executed += "orders" }
            ),
            onProgress = { completed, _ -> progress += completed }
        )

        assertEquals(listOf("profile", "shop", "orders"), executed)
        assertEquals(listOf(1, 2, 3), progress)
        assertEquals(listOf("shop"), failures)
    }
}
