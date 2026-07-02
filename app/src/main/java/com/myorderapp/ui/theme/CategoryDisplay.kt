package com.myorderapp.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryDisplay {
    val allCategories = listOf("中餐", "西餐", "甜品", "饮品", "日料", "韩餐", "东南亚")

    fun icon(category: String): ImageVector = when (category) {
        "中餐" -> Icons.Default.RamenDining
        "西餐" -> Icons.Default.Restaurant
        "甜品" -> Icons.Default.Cake
        "饮品" -> Icons.Default.LocalCafe
        "日料" -> Icons.Default.SetMeal
        "韩餐" -> Icons.Default.SoupKitchen
        "东南亚" -> Icons.Default.Restaurant
        else -> Icons.Default.Restaurant
    }

    fun bgColor(category: String): Color = Color(0xFFF0EBE3)

    fun iconAndBg(category: String): Pair<ImageVector, Color> = icon(category) to bgColor(category)
}

fun whoLikesDisplay(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "都爱吃" to Color(0xFFA8C5A0)
    whoLikes.any { it == "你" || it == "我" } -> "你爱吃" to Color(0xFFD4A574)
    whoLikes.any { it == "她" } -> "她爱吃" to Color(0xFFD4A574)
    else -> "未标记" to Color(0xFF999999)
}

fun whoLikesDisplayHome(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "都爱吃" to Color(0xFFA8C5A0)
    whoLikes.any { it == "你" || it == "我" } -> "你爱吃" to Color(0xFFD4A574)
    whoLikes.any { it == "她" } -> "她爱吃" to Color(0xFFD4A574)
    else -> "还没有人吃过" to Color(0xFF999999)
}
