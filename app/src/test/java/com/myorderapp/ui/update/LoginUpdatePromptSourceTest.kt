package com.myorderapp.ui.update

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUpdatePromptSourceTest {

    @Test
    fun `login checks and presents cancellable update dialog`() {
        val mainSource = File("src/main/java/com/myorderapp/MainActivity.kt").readText()
        val dialogSource = File("src/main/java/com/myorderapp/ui/update/LoginUpdateDialog.kt").readText()

        assertTrue(mainSource.contains("LaunchedEffect(isLoggedIn)"))
        assertTrue(mainSource.contains("updateViewModel.checkForUpdate()"))
        assertTrue(mainSource.contains("showLoginUpdateDialog = true"))
        assertTrue(dialogSource.contains("Text(\"取消\""))
        assertTrue(dialogSource.contains("else -> \"立即更新\""))
    }
}
