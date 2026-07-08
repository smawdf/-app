package com.myorderapp.ui.order

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderingHomeShellTest {

    @Test
    fun `main shell is native compose and not a web page container`() {
        val mainActivity = readMainSource("MainActivity.kt")
        val navGraph = readMainSource("ui/navigation/NavGraph.kt")

        assertTrue(mainActivity.contains("NavGraph("))
        assertTrue(mainActivity.contains("FloatingLiquidBottomBar"))
        assertTrue(navGraph.contains("CoupleMenuScreen"))
        assertTrue(navGraph.contains("OrderingScreen"))

        assertFalse(mainActivity.contains("AndroidView("))
        assertFalse(mainActivity.contains("WebView("))
        assertFalse(navGraph.contains("StitchScreen"))
        assertFalse(navGraph.contains("file:///android_asset"))
    }

    private fun readMainSource(relativePath: String): String {
        val sourcePath = resolvePath(
            "src/main/java/com/myorderapp/$relativePath",
            "app/src/main/java/com/myorderapp/$relativePath"
        )

        return Files.readString(sourcePath)
    }

    private fun resolvePath(vararg candidates: String) =
        candidates
            .map { Paths.get(it) }
            .firstOrNull { Files.exists(it) }
            ?: error("Path not found from ${Paths.get("").toAbsolutePath()}. Tried: ${candidates.joinToString()}")
}
