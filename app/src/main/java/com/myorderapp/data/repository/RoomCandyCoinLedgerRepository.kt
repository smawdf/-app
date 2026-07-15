package com.myorderapp.data.repository

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.data.local.dao.CandyCoinRecordDao
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.CandyCoinRecord
import com.myorderapp.domain.repository.CandyCoinLedgerRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class RoomCandyCoinLedgerRepository(
    private val dao: CandyCoinRecordDao,
    private val session: SessionManager,
    private val cloudErrorLogger: CloudErrorLogger? = null
) : CandyCoinLedgerRepository {
    private val client by lazy { SupabaseClientProvider.client }

    override fun observeRecords(): Flow<List<CandyCoinRecord>> {
        return dao.observeRecords(localPairId()).map { records -> records.map { it.toDomain() } }
    }

    override suspend fun addRecord(record: CandyCoinRecord) {
        dao.insert(record.toEntity(localPairId()))
        if (!session.isLoggedIn.value) return
        val pairId = session.currentPairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID } ?: return
        try {
            val alreadyStored = client.from("candy_coin_records").select {
                filter {
                    eq("id", record.id)
                    eq("pair_id", pairId)
                }
            }.decodeList<RemoteCandyCoinRecord>().isNotEmpty()
            if (!alreadyStored) {
                client.from("candy_coin_records").upsert(record.toRemotePayload(pairId, session.currentUserId)) { select() }
            }
        } catch (e: Exception) {
            cloudErrorLogger?.log("candy_coin", "sync_record", e, "pairId=$pairId recordId=${record.id}")
        }
    }

    override suspend fun loadFromCloud() {
        if (!session.isLoggedIn.value) return
        val pairId = session.currentPairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID } ?: return
        try {
            val records = client.from("candy_coin_records").select {
                filter { eq("pair_id", pairId) }
            }.decodeList<RemoteCandyCoinRecord>()
            val remoteIds = records.mapTo(mutableSetOf()) { it.id }
            records.forEach { dao.insert(it.toDomain().toEntity(pairId)) }
            dao.getAll(pairId)
                .filterNot { it.id in remoteIds }
                .forEach { local ->
                    client.from("candy_coin_records").upsert(
                        local.toDomain().toRemotePayload(pairId, session.currentUserId)
                    ) { select() }
                }
        } catch (e: Exception) {
            cloudErrorLogger?.log("candy_coin", "load_records", e, "pairId=$pairId")
        }
    }

    private fun CandyCoinRecord.toRemotePayload(pairId: String, userId: String) = RemoteCandyCoinRecord(
        id = id,
        pairId = pairId,
        userId = userId.ifBlank { null },
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        actorRole = actorRole,
        targetRole = targetRole,
        note = note,
        createdAt = createdAt
    )

    private fun RemoteCandyCoinRecord.toDomain() = CandyCoinRecord(
        id = id,
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        actorRole = actorRole,
        targetRole = targetRole,
        note = note,
        createdAt = createdAt
    )

    private companion object {
        const val DEFAULT_PAIR_ID = "00000000-0000-0000-0000-000000000000"
    }

    private fun localPairId(): String {
        return session.currentPairId.takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }.orEmpty()
    }
}

@Serializable
private data class RemoteCandyCoinRecord(
    val id: String,
    @SerialName("pair_id") val pairId: String,
    @SerialName("user_id") val userId: String? = null,
    val type: String,
    val amount: Int,
    @SerialName("balance_after") val balanceAfter: Int,
    @SerialName("actor_role") val actorRole: String,
    @SerialName("target_role") val targetRole: String,
    val note: String,
    @SerialName("created_at") val createdAt: String
)
