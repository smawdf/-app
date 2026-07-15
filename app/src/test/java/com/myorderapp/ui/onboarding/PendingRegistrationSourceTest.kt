package com.myorderapp.ui.onboarding

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingRegistrationSourceTest {
    @Test
    fun `email confirmation registration resumes profile on matching login`() {
        val onboarding = readSource("ui/onboarding/OnboardingViewModel.kt")
        val screen = readSource("ui/onboarding/OnboardingScreen.kt")
        val session = readSource("data/remote/supabase/SessionManager.kt")
        val profile = readSource("data/repository/SupabaseProfileRepository.kt")
        val auth = readSource("ui/auth/AuthViewModel.kt")

        assertTrue(onboarding.contains("savePendingRegistration"))
        assertTrue(onboarding.contains("requiresLoginAfterRegistration = true"))
        assertTrue(screen.contains("if (uiState.requiresLoginAfterRegistration) onLoginClick()"))
        assertTrue(session.contains("pendingEmail != normalizedEmail"))
        assertTrue(profile.contains("applyPendingRegistration"))
        assertTrue(profile.contains("CloudImageUploadWorker.enqueue"))
        assertTrue(auth.contains("profileRepo.applyPendingRegistration(state.email, profile)"))
    }

    private fun readSource(relativePath: String): String = Files.readString(
        listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        ).first(Files::exists)
    )
}
