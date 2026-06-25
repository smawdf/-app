package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val pairId: String = "",
    val nickname: String = "",
    val avatarUrl: String? = null,
    val tastePrefsJson: String = "{}",
    val allergiesJson: String = "[]",
    val createdAt: String = "",
    val updatedAt: String = ""
)
