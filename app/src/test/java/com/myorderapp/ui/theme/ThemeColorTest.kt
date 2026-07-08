package com.myorderapp.ui.theme

import androidx.compose.ui.graphics.Color
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorTest {
    @Test
    fun `primary theme color matches Stitch warm pink palette`() {
        assertEquals(Color(0xFF894C5C), Primary)
        assertEquals(Color(0xFFF4A7B9), PrimaryContainer)
        assertNotEquals(Color(0xFF5F95B5), Primary)
    }

    @Test
    fun `auth screens use the same pink app palette`() {
        val authVisuals = readMainSource("ui/auth/AuthVisuals.kt")
        val authScreen = readMainSource("ui/auth/AuthScreen.kt")
        val onboardingScreen = readMainSource("ui/onboarding/OnboardingScreen.kt")
        val resetPasswordScreen = readMainSource("ui/auth/ResetPasswordScreen.kt")
        val combinedAuthSource = authVisuals + authScreen + onboardingScreen + resetPasswordScreen

        listOf(
            "val AuthPrimaryEnd = Primary",
            "val AuthInk = OnBackground",
            "val AuthMuted = OnSurfaceVariant",
            "Color(0xFFFEF8F2)",
            "border = BorderStroke(1.dp, AuthFieldStroke.copy(alpha = 0.72f))",
            "containerColor = Color(0xFFFF9FB7)",
            "AuthPrimaryEnd",
            "AuthInk",
            "AuthMuted"
        ).forEach { expected ->
            assertTrue("Auth page missing pink theme token: $expected", combinedAuthSource.contains(expected))
        }

        listOf(
            "0xFF2E7F99",
            "0xFF7FBBD0",
            "0xFF173B44",
            "0xFF6F858B",
            "0xFFD7EBF1",
            "0xFFE4F7F4"
        ).forEach { forbidden ->
            assertFalse("Auth page should not keep old blue-green palette: $forbidden", combinedAuthSource.contains(forbidden))
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
