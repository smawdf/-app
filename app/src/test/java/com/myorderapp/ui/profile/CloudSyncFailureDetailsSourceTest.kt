package com.myorderapp.ui.profile

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncFailureDetailsSourceTest {
    @Test
    fun `profile shows concrete failed cloud areas`() {
        val source = Files.readString(
            listOf(
                Paths.get("src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt"),
                Paths.get("app/src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt")
            ).first(Files::exists)
        )
        assertTrue(source.contains("failedSyncSteps = uiState.cloudSyncState.failedSteps"))
        assertTrue(source.contains("failedSyncSteps.toSyncFailureText()"))
        assertTrue(source.contains("\"orders\" to \"订单\""))
        assertTrue(source.contains("\"preferences\" to \"偏好设置\""))
    }
}
