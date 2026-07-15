package com.myorderapp.data.repository

import java.time.OffsetDateTime

internal fun String.isCloudTimestampAfter(other: String): Boolean {
    val left = toCloudInstantOrNull() ?: return false
    val right = other.toCloudInstantOrNull() ?: return true
    return left.isAfter(right)
}

private fun String.toCloudInstantOrNull() = runCatching {
    OffsetDateTime.parse(this).toInstant()
}.getOrNull()
