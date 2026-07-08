package com.myorderapp.domain.model

data class CandyCoinRecord(
    val id: String,
    val type: String,
    val amount: Int,
    val balanceAfter: Int,
    val actorRole: String,
    val targetRole: String,
    val note: String,
    val createdAt: String
)
