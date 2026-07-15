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

    @Test
    fun migrateFrom11To12PreservesOrdersAndAddsSyncState() {
        helper.createDatabase(TEST_DB_11_12, 11).apply {
            execSQL(
                """
                INSERT INTO orders (
                    id, userId, pairId, buyerName, buyerAvatarUrl, buyerRole,
                    shopId, shopName, shopCoverUrl, status, addressSnapshot,
                    buyerNote, subtotal, deliveryFee, totalPrice, candyCoinsSpent, createdAt
                ) VALUES (
                    'order-1', 'user-1', 'pair-1', 'Eater', '', 'eater',
                    'shop-1', 'Sweet Shop', '', 'submitted', 'Pickup',
                    '', 18.0, 0.0, 18.0, 18, '2026-07-15T00:00:00Z'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB_11_12,
            12,
            true,
            AppDatabase.MIGRATION_11_12
        )
        try {
            val cursor = migrated.query("SELECT id, syncState FROM orders WHERE id = 'order-1'")
            try {
                assertTrue(cursor.moveToFirst())
                assertEquals("order-1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                assertEquals("synced", cursor.getString(cursor.getColumnIndexOrThrow("syncState")))
            } finally {
                cursor.close()
            }
        } finally {
            migrated.close()
        }
    }

    @Test
    fun migrateFrom12To13PreservesOrdersAndAddsMomentImageUrl() {
        helper.createDatabase(TEST_DB_12_13, 12).apply {
            execSQL(
                """
                INSERT INTO orders (
                    id, userId, pairId, buyerName, buyerAvatarUrl, buyerRole,
                    shopId, shopName, shopCoverUrl, status, addressSnapshot,
                    buyerNote, subtotal, deliveryFee, totalPrice, candyCoinsSpent,
                    createdAt, syncState
                ) VALUES (
                    'order-2', 'user-1', 'pair-1', 'Eater', '', 'eater',
                    'shop-1', 'Sweet Shop', '', 'completed', 'Pickup',
                    '', 18.0, 0.0, 18.0, 18, '2026-07-15T00:00:00Z', 'synced'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB_12_13,
            13,
            true,
            AppDatabase.MIGRATION_12_13
        )
        try {
            val cursor = migrated.query("SELECT id, momentImageUrl FROM orders WHERE id = 'order-2'")
            try {
                assertTrue(cursor.moveToFirst())
                assertEquals("order-2", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("momentImageUrl")))
            } finally {
                cursor.close()
            }
        } finally {
            migrated.close()
        }
    }

    @Test
    fun migrateFrom13To14PreservesOrdersAndAddsViewerUserIds() {
        helper.createDatabase(TEST_DB_13_14, 13).apply {
            execSQL(
                """
                INSERT INTO orders (
                    id, userId, pairId, buyerName, buyerAvatarUrl, buyerRole,
                    shopId, shopName, shopCoverUrl, status, addressSnapshot,
                    buyerNote, subtotal, deliveryFee, totalPrice, candyCoinsSpent,
                    momentImageUrl, createdAt, syncState
                ) VALUES (
                    'order-3', 'user-1', 'pair-1', 'Eater', '', 'eater',
                    'shop-1', 'Sweet Shop', '', 'completed', 'Pickup',
                    '', 18.0, 0.0, 18.0, 18, '', '2026-07-15T00:00:00Z', 'synced'
                )
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB_13_14,
            14,
            true,
            AppDatabase.MIGRATION_13_14
        )
        try {
            val cursor = migrated.query("SELECT viewerUserIdsJson FROM orders WHERE id = 'order-3'")
            try {
                assertTrue(cursor.moveToFirst())
                assertEquals("[]", cursor.getString(0))
            } finally {
                cursor.close()
            }
        } finally {
            migrated.close()
        }
    }

    private companion object {
        const val TEST_DB_NAME = "app-database-migration-test"
        const val TEST_DB_11_12 = "app-database-migration-11-12-test"
        const val TEST_DB_12_13 = "app-database-migration-12-13-test"
        const val TEST_DB_13_14 = "app-database-migration-13-14-test"
    }
}
