package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val contactName: String,
    val contactPhone: String,
    val addressLine1: String,
    val addressLine2: String,
    val tag: String,
    val isDefault: Boolean
)
