package com.myorderapp.domain.model

data class Address(
    val id: String,
    val userId: String,
    val contactName: String,
    val contactPhone: String,
    val addressLine1: String,
    val addressLine2: String = "",
    val tag: String = "",
    val isDefault: Boolean = false
)
