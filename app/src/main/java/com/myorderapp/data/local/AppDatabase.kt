package com.myorderapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.myorderapp.data.local.dao.DishDao
import com.myorderapp.data.local.dao.MealDao
import com.myorderapp.data.local.dao.AddressDao
import com.myorderapp.data.local.dao.CartDao
import com.myorderapp.data.local.dao.CandyCoinRecordDao
import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.dao.OrderDao
import com.myorderapp.data.local.dao.ProfileDao
import com.myorderapp.data.local.dao.WishlistDao
import com.myorderapp.data.local.entity.AddressEntity
import com.myorderapp.data.local.entity.CartItemEntity
import com.myorderapp.data.local.entity.CandyCoinRecordEntity
import com.myorderapp.data.local.entity.DishEntity
import com.myorderapp.data.local.entity.MealEntity
import com.myorderapp.data.local.entity.MealItemEntity
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.data.local.entity.OrderEntity
import com.myorderapp.data.local.entity.OrderItemEntity
import com.myorderapp.data.local.entity.ProfileEntity
import com.myorderapp.data.local.entity.WishlistEntity
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DishEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        ProfileEntity::class,
        WishlistEntity::class,
        MenuDishEntity::class,
        CartItemEntity::class,
        AddressEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        CandyCoinRecordEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun mealDao(): MealDao
    abstract fun profileDao(): ProfileDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun menuDishDao(): MenuDishDao
    abstract fun cartDao(): CartDao
    abstract fun addressDao(): AddressDao
    abstract fun orderDao(): OrderDao
    abstract fun candyCoinRecordDao(): CandyCoinRecordDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cart_items` (
                        `id` TEXT NOT NULL,
                        `shopId` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `shopCoverUrl` TEXT NOT NULL,
                        `minOrderPrice` REAL NOT NULL,
                        `deliveryFee` REAL NOT NULL,
                        `menuItemId` TEXT NOT NULL,
                        `menuItemName` TEXT NOT NULL,
                        `menuItemImageUrl` TEXT NOT NULL,
                        `unitPrice` REAL NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `addedAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `addresses` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `contactName` TEXT NOT NULL,
                        `contactPhone` TEXT NOT NULL,
                        `addressLine1` TEXT NOT NULL,
                        `addressLine2` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `orders` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `shopId` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `shopCoverUrl` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `addressSnapshot` TEXT NOT NULL,
                        `buyerNote` TEXT NOT NULL,
                        `subtotal` REAL NOT NULL,
                        `deliveryFee` REAL NOT NULL,
                        `totalPrice` REAL NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `order_items` (
                        `id` TEXT NOT NULL,
                        `orderId` TEXT NOT NULL,
                        `menuItemId` TEXT NOT NULL,
                        `menuItemName` TEXT NOT NULL,
                        `menuItemImageUrl` TEXT NOT NULL,
                        `unitPrice` REAL NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `subtotal` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `menu_dishes` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `price` REAL NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `menu_dishes` ADD COLUMN `originPrice` REAL")
                db.execSQL("ALTER TABLE `menu_dishes` ADD COLUMN `monthlySales` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `menu_dishes` ADD COLUMN `stock` INTEGER NOT NULL DEFAULT 32")
                db.execSQL("ALTER TABLE `menu_dishes` ADD COLUMN `isAvailable` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `menu_dishes` ADD COLUMN `isSignature` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `pairId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `buyerName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `buyerAvatarUrl` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `buyerRole` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `profiles` ADD COLUMN `candyCoins` INTEGER NOT NULL DEFAULT 66")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `candyCoinsSpent` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `candy_coin_records` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `balanceAfter` INTEGER NOT NULL,
                        `actorRole` TEXT NOT NULL,
                        `targetRole` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orderdisk.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
