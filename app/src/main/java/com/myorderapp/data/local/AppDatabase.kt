package com.myorderapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.myorderapp.data.local.dao.DishDao
import com.myorderapp.data.local.dao.MealDao
import com.myorderapp.data.local.dao.ProfileDao
import com.myorderapp.data.local.entity.DishEntity
import com.myorderapp.data.local.entity.MealEntity
import com.myorderapp.data.local.entity.MealItemEntity
import com.myorderapp.data.local.entity.ProfileEntity

@Database(
    entities = [DishEntity::class, MealEntity::class, MealItemEntity::class, ProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dishDao(): DishDao
    abstract fun mealDao(): MealDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orderdisk.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
