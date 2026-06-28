package com.myorderapp.ui.adddish

object AddDishSaveMessages {
    fun primarySaveFailed(error: Throwable): String {
        val detail = error.message?.trim().orEmpty()
        if (detail.isBlank()) return "保存失败，请重试"
        return "保存失败：${detail.take(80)}"
    }
}
