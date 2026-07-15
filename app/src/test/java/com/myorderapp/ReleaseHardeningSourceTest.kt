package com.myorderapp

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseHardeningSourceTest {
    @Test
    fun releaseShrinksAndPrivateDataIsExcludedFromBackup() {
        val gradle = read("app/build.gradle.kts", "build.gradle.kts")
        val backup = read("app/src/main/res/xml/backup_rules.xml", "src/main/res/xml/backup_rules.xml")
        val extraction = read("app/src/main/res/xml/data_extraction_rules.xml", "src/main/res/xml/data_extraction_rules.xml")

        assertTrue(gradle.contains("isMinifyEnabled = true"))
        assertTrue(gradle.contains("isShrinkResources = true"))
        assertTrue(backup.contains("domain=\"sharedpref\" path=\".\""))
        assertTrue(backup.contains("domain=\"database\" path=\".\""))
        assertTrue(extraction.contains("<device-transfer>"))
    }

    private fun read(vararg candidates: String): String {
        val path = candidates.map(Paths::get).first { Files.exists(it) }
        return Files.readString(path)
    }
}
