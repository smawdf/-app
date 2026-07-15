package com.myorderapp.ui.auth

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailConfirmationDisabledSourceTest {
    @Test
    fun `registration requires immediate session when email confirmation is disabled`() {
        val onboarding = readSource("ui/onboarding/OnboardingViewModel.kt")
        val screen = readSource("ui/onboarding/OnboardingScreen.kt")
        val auth = readSource("ui/auth/AuthViewModel.kt")

        assertTrue(onboarding.contains("Supabase 仍要求邮箱验证"))
        assertFalse(screen.contains("邮箱注册后可能需要先确认邮件"))
        assertTrue(auth.contains("当前不使用邮箱验证"))
        assertFalse(auth.contains("请先打开验证邮件确认后再登录"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )
}
