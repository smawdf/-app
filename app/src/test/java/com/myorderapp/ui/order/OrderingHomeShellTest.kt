package com.myorderapp.ui.order

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderingHomeShellTest {

    @Test
    fun `home screen participates in the app tab bar`() {
        val source = readMainSource("MainActivity.kt")

        assertTrue(source.contains("currentDestination?.route in bottomNavRoutes"))
        assertTrue(source.contains("navigateAsTab(route: String)"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )

        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath from ${Paths.get("").toAbsolutePath()}")

        return Files.readString(sourcePath)
    }
}
