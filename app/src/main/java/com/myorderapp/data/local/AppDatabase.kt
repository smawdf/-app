package com.myorderapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.myorderapp.data.local.dao.DishDao
import com.myorderapp.data.local.dao.MealDao
import com.myorderapp.data.local.dao.ProfileDao
import com.myorderapp.data.local.dao.WishlistDao
import com.myorderapp.data.local.entity.DishEntity
import com.myorderapp.data.local.entity.MealEntity
import com.myorderapp.data.local.entity.MealItemEntity
import com.myorderapp.data.local.entity.ProfileEntity
import com.myorderapp.data.local.entity.WishlistEntity
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DishEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        ProfileEntity::class,
        WishlistEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun mealDao(): MealDao
    abstract fun profileDao(): ProfileDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wishlist_items` (
                        `id` TEXT NOT NULL,
                        `pairId` TEXT NOT NULL,
                        `dishId` TEXT NOT NULL,
                        `dishName` TEXT NOT NULL,
                        `dishCategory` TEXT NOT NULL,
                        `dishImageUrl` TEXT,
                        `externalSource` TEXT,
                        `addedBy` TEXT NOT NULL,
                        `addedByName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_items_mealId` ON `meal_items` (`mealId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orderdisk.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
