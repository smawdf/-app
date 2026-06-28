package com.myorderapp.ui.adddish

import java.util.Locale

object AddDishImageUploadPolicy {
    fun shouldQueueUpload(imageUrl: String): Boolean {
        val value = imageUrl.trim().lowercase(Locale.US)
        return value.startsWith("content://") || value.startsWith("file://")
    }
}
