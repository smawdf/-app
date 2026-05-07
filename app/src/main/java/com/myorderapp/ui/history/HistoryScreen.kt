package com.myorderapp.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel

data class HistoryEntry(
    val id: String, val date: String, val dayOfMonth: Int, val mealType: String,
    val mealTypeColor: Color, val dishes: String, val myCount: Int, val partnerCount: Int,
    val note: String, val noteColor: Color, val bgColor: Color
)

data class TopDish(val rank: Int, val name: String, val count: Int, val medal: String, val medalColor: Color)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val historyEntries = uiState.meals.mapIndexed { index, meal ->
        val color = when (meal.mealType) {
            "breakfast" -> MaterialTheme.colorScheme.secondary; "lunch" -> MaterialTheme.colorScheme.primary
            "dinner" -> MaterialTheme.colorScheme.primary; "supper" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        HistoryEntry(
            id = meal.id, date = meal.date, dayOfMonth = meal.date.split("-").lastOrNull()?.toIntOrNull() ?: 0,
            mealType = meal.mealType, mealTypeColor = color,
            dishes = meal.items.joinToString(" + ") { it.dishName },
            myCount = meal.items.count { it.chosenBy == "u1" },
            partnerCount = meal.items.count { it.chosenBy != "u1" },
            note = "", noteColor = MaterialTheme.colorScheme.primary, bgColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    val topDishes = uiState.meals
        .flatMap { meal -> meal.items.map { it.dishName } }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
        .mapIndexed { index, (name, count) ->
            val (medal, color) = when (index) {
                0 -> "🥇" to MaterialTheme.colorScheme.primary
                1 -> "🥈" to MaterialTheme.colorScheme.secondary
                2 -> "🥉" to MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else -> "${index + 1}" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
            TopDish(index + 1, name, count, medal, color)
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 56.dp, bottom = 16.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📊 历史记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp, 0.dp, 0.dp, 14.dp),
                        color = if (uiState.viewMode == "calendar") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { if (uiState.viewMode != "calendar") viewModel.toggleViewMode() }) {
                        Text("📅 日历", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 10.sp,
                            color = if (uiState.viewMode == "calendar") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (uiState.viewMode == "calendar") FontWeight.SemiBold else FontWeight.Normal)
                    }
                    Surface(shape = RoundedCornerShape(0.dp, 14.dp, 14.dp, 0.dp),
                        color = if (uiState.viewMode == "list") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { if (uiState.viewMode != "list") viewModel.toggleViewMode() }) {
                        Text("📋 列表", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 10.sp,
                            color = if (uiState.viewMode == "list") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (uiState.viewMode == "list") FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("${uiState.totalMeals}", "总点餐次数", "累计", Modifier.weight(1f))
                StatCard("0", "尝试新菜品", "本月", Modifier.weight(1f))
                StatCard("${uiState.streakDays}", "连续点餐天", "当前", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (uiState.viewMode == "calendar") {
            item {
                Text("最近几天", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Placeholder calendar
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("最近点餐", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(historyEntries) { entry ->
            HistoryCard(entry, partnerName = uiState.partnerName.ifBlank { "对方" })
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("🏆 最爱菜品 Top 5", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(topDishes) { dish ->
            TopDishRow(dish)
            Spacer(modifier = Modifier.height(4.dp))
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun StatCard(value: String, label: String, subLabel: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subLabel, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun HistoryCard(entry: HistoryEntry, partnerName: String) {
    Card(shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(entry.bgColor),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("${entry.dayOfMonth}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = entry.mealTypeColor)
                Text(entry.mealType, fontSize = 9.sp, color = entry.mealTypeColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.dishes, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("你选了${entry.myCount}道 · ${partnerName}选了${entry.partnerCount}道", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.note.isNotBlank()) Text(entry.note, fontSize = 10.sp, color = entry.noteColor)
            }
        }
    }
}

@Composable
fun TopDishRow(dish: TopDish) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(dish.medal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(dish.name, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("点了 ${dish.count} 次", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
