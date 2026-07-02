package com.myorderapp.ui.localization

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VisibleChineseTextTest {

    @Test
    fun `ordering flow source does not contain English visible copy`() {
        val files = listOf(
            "app/src/main/java/com/myorderapp/ui/order/OrderingScreen.kt",
            "app/src/main/java/com/myorderapp/ui/order/OrderingViewModel.kt",
            "app/src/main/java/com/myorderapp/ui/menu/MenuManagementScreen.kt",
            "app/src/main/java/com/myorderapp/ui/menu/MenuManagementViewModel.kt",
            "app/src/main/java/com/myorderapp/ui/shop/components/CartSheet.kt",
            "app/src/main/java/com/myorderapp/ui/checkout/CheckoutScreen.kt",
            "app/src/main/java/com/myorderapp/ui/orders/OrdersScreen.kt",
            "app/src/main/java/com/myorderapp/ui/orders/OrderDetailScreen.kt",
            "app/src/main/java/com/myorderapp/ui/couple/CoupleMenuScreen.kt",
            "app/src/main/java/com/myorderapp/ui/couple/AnniversaryScreen.kt",
            "app/src/main/java/com/myorderapp/ui/discover/DiscoverScreen.kt",
            "app/src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt",
            "app/src/main/java/com/myorderapp/ui/navigation/BottomNavItem.kt",
            "app/src/main/java/com/myorderapp/data/repository/SingleShopRepository.kt"
        )

        val violations = visibleEnglishStrings(files)
        assertTrue(
            "发现英文可见文案：\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun `ui visible literals do not contain English copy`() {
        val uiRoot = projectRoot().resolve("app/src/main/java/com/myorderapp/ui")
        val violations = mutableListOf<String>()
        Files.walk(uiRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { path ->
                    val source = Files.readString(path)
                    source.lineSequence()
                        .forEachIndexed { index, line ->
                            val values = if (line.isVisibleCopyLine()) {
                                quotedStringRegex.findAll(line)
                                    .map { it.groupValues[1] }
                                    .filter { it.hasAsciiLetter() && !it.isAllowedVisibleString() }
                                    .toList()
                            } else {
                                emptyList()
                            }

                            if (values.isNotEmpty()) {
                                val relative = projectRoot().relativize(path)
                                violations += "$relative:${index + 1} -> ${values.joinToString()}"
                            }
                        }
                }
        }

        assertTrue(
            "发现英文可见文案：\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    private fun visibleEnglishStrings(relativeFiles: List<String>): List<String> {
        val root = projectRoot()
        val violations = mutableListOf<String>()
        relativeFiles.forEach { relative ->
            val path = root.resolve(relative)
            val source = Files.readString(path)
            quotedStringRegex.findAll(source).forEach { match ->
                val value = match.groupValues[1]
                if (value.hasAsciiLetter() && !value.isAllowedTechnicalString()) {
                    val line = source.substring(0, match.range.first).count { it == '\n' } + 1
                    violations += "${root.relativize(path)}:$line -> \"$value\""
                }
            }
        }
        return violations
    }

    private fun projectRoot(): Path {
        val cwd = Paths.get("").toAbsolutePath()
        return if (cwd.resolve("app/src/main").toFile().exists()) cwd else cwd.parent ?: cwd
    }

    private fun String.hasAsciiLetter(): Boolean = any { it in 'A'..'Z' || it in 'a'..'z' }

    private fun String.isAllowedTechnicalString(): Boolean {
        val trimmed = trim()
        if (trimmed.isBlank()) return true
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true
        if (trimmed.contains('/')) return true
        if (trimmed.contains("$")) return true
        if (trimmed.contains("%.")) return true
        if (trimmed.startsWith("image/")) return true
        if (trimmed == "new") return true
        if (trimmed == "avatars") return true
        if (trimmed.endsWith(".jpg")) return true
        if (trimmed.startsWith("avatar_")) return true
        if (trimmed.matches(Regex("[A-Za-z0-9_{}.-]+"))) return true
        return false
    }

    private fun String.isVisibleCopyLine(): Boolean {
        val trimmed = trim()
        return visibleCopyMarkers.any { marker -> marker in trimmed }
    }

    private fun String.isAllowedVisibleString(): Boolean {
        val trimmed = trim()
        if (trimmed.isBlank()) return true
        if ("$" in trimmed) return true
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true
        if (trimmed.startsWith("image/")) return true
        if (trimmed.endsWith(".jpg")) return true
        return false
    }

    private companion object {
        val quotedStringRegex = Regex(""""((?:[^"\\]|\\.)*)"""")
        val visibleCopyMarkers = listOf(
            "Text(\"",
            "title = { Text(\"",
            "text = { Text(\"",
            "placeholder = { Text(\"",
            "label = { Text(\"",
            "contentDescription = \"",
            "showSnackbar(\"",
            "Toast."
        )
    }
}
