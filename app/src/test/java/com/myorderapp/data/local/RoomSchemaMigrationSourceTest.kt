package com.myorderapp.data.local

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomSchemaMigrationSourceTest {
    @Test
    fun databaseExportsCurrentVersionAndRegistersMigrations() {
        val database = readMainSource("data/local/AppDatabase.kt")
        val schemaCandidates = listOf(
            Paths.get("schemas/com.myorderapp.data.local.AppDatabase/14.json"),
            Paths.get("app/schemas/com.myorderapp.data.local.AppDatabase/14.json")
        )
        assertTrue(database.contains("version = 14"))
        assertTrue(database.contains("MIGRATION_10_11"))
        assertTrue(database.contains("MIGRATION_11_12"))
        assertTrue(database.contains("MIGRATION_12_13"))
        assertTrue(database.contains("MIGRATION_13_14"))
        assertTrue(database.contains("viewerUserIdsJson"))
        assertTrue(database.contains("momentImageUrl"))
        assertTrue(database.contains("menu_dish_deletions"))
        assertTrue(database.contains("syncState"))
        assertTrue(schemaCandidates.any(Files::exists))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        return Files.readString(candidates.first { Files.exists(it) })
    }
}
