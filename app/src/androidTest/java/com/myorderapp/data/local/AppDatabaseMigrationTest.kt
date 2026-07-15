package com.myorderapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrateFrom10To11PreservesMenuDishesAndAddsDeletionTable() {
        helper.createDatabase(TEST_DB_NAME, 10).apply {
            execSQL(
                """
                INSERT INTO menu_dishes (
                    id, pairId, name, price, originPrice, imageUrl, category,
                    description, sortOrder, monthlySales, stock, isAvailable,
                    isSignature, updatedAt
                ) VALUES (
                    'dish-1', 'pair-1', 'Noodles', 18.0, 20.0, '', 'Main',
                    'Warm noodles', 0, 5, 12, 1, 0, '2026-07-10T00:00:00Z'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            11,
            true,
            AppDatabase.MIGRATION_10_11
        )
        try {
            val dishCursor = migrated.query("SELECT id, pairId, name FROM menu_dishes WHERE id = 'dish-1'")
            try {
                assertTrue(dishCursor.moveToFirst())
                assertEquals("pair-1", dishCursor.getString(dishCursor.getColumnIndexOrThrow("pairId")))
                assertEquals("Noodles", dishCursor.getString(dishCursor.getColumnIndexOrThrow("name")))
            } finally {
                dishCursor.close()
            }

            migrated.execSQL(
                "INSERT INTO menu_dish_deletions (id, pairId, deletedAt) VALUES ('dish-2', 'pair-1', '2026-07-10T00:01:00Z')"
            )
            val deletionCursor = migrated.query("SELECT id FROM menu_dish_deletions WHERE id = 'dish-2'")
            try {
                assertTrue(deletionCursor.moveToFirst())
                assertEquals("dish-2", deletionCursor.getString(0))
            } finally {
                deletionCursor.close()
            }
        } finally {
            migrated.close()
        }
    }

    private companion object {
        const val TEST_DB_NAME = "app-database-migration-test"
    }
}
