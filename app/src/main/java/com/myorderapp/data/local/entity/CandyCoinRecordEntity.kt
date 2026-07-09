package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "candy_coin_records")
data class CandyCoinRecordEntity(
    @PrimaryKey val id: String,
    val pairId: String = "",
    val type: String,
    val amount: Int,
    val balanceAfter: Int,
    val actorRole: String,
    val targetRole: String,
    val note: String,
    val createdAt: String
)
