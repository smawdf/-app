package com.myorderapp.domain.repository

import com.myorderapp.domain.model.CandyCoinRecord
import kotlinx.coroutines.flow.Flow

interface CandyCoinLedgerRepository {
    fun observeRecords(): Flow<List<CandyCoinRecord>>
    suspend fun addRecord(record: CandyCoinRecord)
    suspend fun loadFromCloud()
}
