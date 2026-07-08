package com.myorderapp.data.repository

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.CandyCoinRecordDao
import com.myorderapp.domain.model.CandyCoinRecord
import com.myorderapp.domain.repository.CandyCoinLedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCandyCoinLedgerRepository(
    private val dao: CandyCoinRecordDao
) : CandyCoinLedgerRepository {
    override fun observeRecords(): Flow<List<CandyCoinRecord>> {
        return dao.observeRecords().map { records -> records.map { it.toDomain() } }
    }

    override suspend fun addRecord(record: CandyCoinRecord) {
        dao.insert(record.toEntity())
    }
}
