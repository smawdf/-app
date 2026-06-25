package com.myorderapp.data.repository

class CloudDishLoadState {
    private var loadedUserId: String? = null
    private var loadedPairId: String? = null

    fun shouldLoad(isLoggedIn: Boolean, userId: String, pairId: String): Boolean {
        if (!isLoggedIn || userId.isBlank() || pairId.isBlank()) {
            reset()
            return false
        }
        return loadedUserId != userId || loadedPairId != pairId
    }

    fun markLoaded(userId: String, pairId: String) {
        loadedUserId = userId
        loadedPairId = pairId
    }

    fun reset() {
        loadedUserId = null
        loadedPairId = null
    }
}
