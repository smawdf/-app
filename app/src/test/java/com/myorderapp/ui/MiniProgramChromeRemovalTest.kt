package com.myorderapp.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.Assert.assertFalse
import org.junit.Test

class MiniProgramChromeRemovalTest {

    @Test
    fun `main ui and previews do not contain mini program capsule chrome`() {
        val roots = listOf(
            Paths.get("app/src/main/java/com/myorderapp/ui"),
            Paths.get("docs/previews")
        ).filter { Files.exists(it) }

        val scanned = roots.flatMap { root ->
            Files.walk(root).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) }
                    .filter { it.extension in setOf("kt", "html", "svg") }
                    .toList()
            }
        }

        val forbidden = listOf("•••", "MiniProgramCapsule", "MiniProgramHeader", "mini-capsule")
        scanned.forEach { path ->
            val text = Files.readString(path)
            forbidden.forEach { token ->
                assertFalse("${path.toDisplay()} 不应包含小程序胶囊标记 $token", text.contains(token))
            }
        }
    }

    private fun Path.toDisplay(): String = toAbsolutePath().toString()
}
