package com.myorderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myorderapp.data.local.entity.CandyCoinRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CandyCoinRecordDao {
    @Query("SELECT * FROM candy_coin_records ORDER BY createdAt DESC")
    fun observeRecords(): Flow<List<CandyCoinRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CandyCoinRecordEntity)
}
