package com.myorderapp.ui.components

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CozyMotionSourceTest {

    @Test
    fun `navigation and key feedback use shared cozy motion tokens`() {
        val motion = readMainSource("ui/components/Motion.kt")
        val navGraph = readMainSource("ui/navigation/NavGraph.kt")
        val ordering = readMainSource("ui/order/OrderingScreen.kt")
        val couple = readMainSource("ui/couple/CoupleMenuScreen.kt")
        val profile = readMainSource("ui/profile/ProfileScreen.kt")

        listOf(
            "const val Exit = 140",
            "const val Toast = 160",
            "const val CartFly = 620",
            "const val PressedScale = 0.96f",
            "const val SoftPressedScale = 0.985f",
            "const val ButtonPressedScale = 0.97f"
        ).forEach { expected ->
            assertTrue("Motion tokens missing: $expected", motion.contains(expected))
        }

        assertTrue(navGraph.contains("CozyMotion.Standard"))
        assertTrue(navGraph.contains("CozyMotion.Exit"))
        assertTrue(ordering.contains("CozyMotion.CartFly"))
        assertTrue(ordering.contains("CozyMotion.PressedScale"))
        assertTrue(couple.contains("CozyMotion.Toast"))
        assertTrue(profile.contains("CozyMotion.SoftPressedScale"))

        listOf(navGraph, ordering, couple).forEach { source ->
            assertFalse("animation duration should use CozyMotion token", source.contains("tween(160)"))
            assertFalse("animation duration should use CozyMotion token", source.contains("durationMillis = 620"))
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
