package com.myorderapp.ui.theme

import androidx.compose.ui.graphics.Color

object CategoryDisplay {
    val allCategories = listOf("中餐", "西餐", "甜品", "饮品", "日料", "韩餐", "东南亚")

    fun emoji(category: String): String = when (category) {
        "中餐" -> "🍜"
        "西餐" -> "🥗"
        "甜品" -> "🍰"
        "饮品" -> "☕"
        "日料" -> "🍣"
        "韩餐" -> "🥘"
        "东南亚" -> "🍲"
        else -> "🍽️"
    }

    fun bgColor(category: String): Color = Color(0xFFF0EBE3)

    fun emojiAndBg(category: String): Pair<String, Color> = emoji(category) to bgColor(category)
}

fun whoLikesDisplay(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "❤️ 都爱吃" to Color(0xFFA8C5A0)
    whoLikes.any { it == "你" || it == "我" } -> "⭐ 你爱吃" to Color(0xFFD4A574)
    whoLikes.any { it == "她" } -> "⭐ 她爱吃" to Color(0xFFD4A574)
    else -> "未标记" to Color(0xFF999999)
}

fun whoLikesDisplayHome(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "❤️ 都爱吃" to Color(0xFFA8C5A0)
    whoLikes.any { it == "你" || it == "我" } -> "⭐ 你爱吃" to Color(0xFFD4A574)
    whoLikes.any { it == "她" } -> "⭐ 她爱吃" to Color(0xFFD4A574)
    else -> "还没有人吃过" to Color(0xFF999999)
}
