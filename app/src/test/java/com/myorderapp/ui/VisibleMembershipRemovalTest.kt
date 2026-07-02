package com.myorderapp.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.Assert.assertFalse
import org.junit.Test

class VisibleMembershipRemovalTest {

    @Test
    fun `main ui and previews do not contain membership ticket or vip wording`() {
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

        val forbidden = listOf("会员", "饭票", "VIP")
        scanned.forEach { path ->
            val text = Files.readString(path)
            forbidden.forEach { word ->
                assertFalse("${path.toDisplay()} 不应包含 $word", text.contains(word))
            }
        }
    }

    private fun Path.toDisplay(): String = toAbsolutePath().toString()
}
