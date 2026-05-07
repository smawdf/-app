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

    fun bgColor(category: String): Color = when (category) {
        "中餐" -> Color(0xFFFFE0B2)
        "西餐" -> Color(0xFFC8E6C9)
        "甜品" -> Color(0xFFF3E5F5)
        "饮品" -> Color(0xFFFFF3E0)
        "日料" -> Color(0xFFE0F7FA)
        "韩餐" -> Color(0xFFFCE4EC)
        "东南亚" -> Color(0xFFFFF8E1)
        else -> Color(0xFFE8EAF6)
    }

    fun emojiAndBg(category: String): Pair<String, Color> = emoji(category) to bgColor(category)
}

fun whoLikesDisplay(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "❤️ 都爱吃" to Color(0xFFFF6B35)
    whoLikes.any { it == "你" || it == "我" } -> "⭐ 你爱吃" to Color(0xFF4CAF50)
    whoLikes.any { it == "她" } -> "⭐ 她爱吃" to Color(0xFF4CAF50)
    else -> "未标记" to Color(0xFF999999)
}

fun whoLikesDisplayHome(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "❤️ 都爱吃" to Color(0xFFFF6B35)
    whoLikes.any { it == "你" || it == "我" } -> "⭐ 你爱吃" to Color(0xFF4CAF50)
    whoLikes.any { it == "她" } -> "⭐ 她爱吃" to Color(0xFF4CAF50)
    else -> "还没有人吃过" to Color(0xFF999999)
}
