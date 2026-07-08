package com.myorderapp.ui

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.Assert.assertFalse
import org.junit.Test

class VisibleMarketplaceWordingRemovalTest {

    @Test
    fun `main ui does not show delivery platform sales wording`() {
        val root = Paths.get("app/src/main/java/com/myorderapp/ui")
        if (!Files.exists(root)) return

        val scanned = Files.walk(root).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { it.extension == "kt" }
                .toList()
        }

        val forbidden = listOf("月售", "月销", "销量", "配送费", "配送中", "骑手", "附近商家", "推荐商家", "外卖", "服务费")
        scanned.forEach { path ->
            val text = Files.readString(path)
            forbidden.forEach { word ->
                assertFalse("${path.toDisplay()} 不应包含外卖平台化文案 $word", text.contains(word))
            }
        }
    }

    @Test
    fun `main ui does not expose empty click handlers`() {
        val root = Paths.get("app/src/main/java/com/myorderapp/ui")
        if (!Files.exists(root)) return

        val emptyClick = Regex("""onClick\s*=\s*\{\s*\}""")
        val scanned = Files.walk(root).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .filter { it.extension == "kt" }
                .toList()
        }

        scanned.forEach { path ->
            val text = Files.readString(path)
            assertFalse("${path.toDisplay()} 不应包含空点击处理", emptyClick.containsMatchIn(text))
        }
    }

    private fun Path.toDisplay(): String = toAbsolutePath().toString()
}
