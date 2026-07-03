package com.myorderapp.ui.auth

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRememberCredentialsSourceTest {

    @Test
    fun `login screen can remember account and password`() {
        val screen = readMainSource("ui/auth/AuthScreen.kt")
        val viewModel = readMainSource("ui/auth/AuthViewModel.kt")
        val sessionManager = readMainSource("data/remote/supabase/SessionManager.kt")

        listOf(
            "记住账号密码",
            "Checkbox",
            "uiState.rememberCredentials",
            "viewModel.onRememberCredentialsChanged"
        ).forEach { expected ->
            assertTrue("登录页缺少记住账号密码入口：$expected", screen.contains(expected))
        }

        listOf(
            "rememberCredentials: Boolean = false",
            "session.isRememberCredentialsEnabled()",
            "session.getSavedPassword()",
            "onRememberCredentialsChanged",
            "session.saveRememberedCredentials("
        ).forEach { expected ->
            assertTrue("登录状态缺少记住账号密码逻辑：$expected", viewModel.contains(expected))
        }

        listOf(
            "saveRememberedCredentials",
            "remember_credentials",
            "saved_password",
            "getSavedPassword",
            "isRememberCredentialsEnabled"
        ).forEach { expected ->
            assertTrue("SessionManager 缺少本地凭据持久化：$expected", sessionManager.contains(expected))
        }
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }
}
