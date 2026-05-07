package com.myorderapp.ui.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.ui.theme.CategoryDisplay
import org.koin.androidx.compose.koinViewModel

@Composable
fun MealResultScreen(
    mealId: String = "",
    viewModel: MealViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onNewMeal: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val myDishes = uiState.mySelections
    val partnerDishes = uiState.partnerSelections
    val partnerName = uiState.partnerName.ifBlank { "对方" }
    val totalDishes = myDishes.size + partnerDishes.size
    val totalTime = (myDishes + partnerDishes).sumOf { it.cookTimeMin }
    val categories = (myDishes + partnerDishes).map { it.dishCategory }.distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        // ── Hero Header ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF4CAF50),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点餐完成！",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${uiState.mealType.let {
                        when(it) {
                            "breakfast" -> "早餐" ; "lunch" -> "午餐"
                            "dinner" -> "晚餐" ; "supper" -> "夜宵" ; else -> it
                        }
                    }} · 共 $totalDishes 道菜 · 预计 ${totalTime}分钟",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── My Selection ──
        SectionHeader(emoji = "🧑", title = "我的选择", count = myDishes.size,
            color = Color(0xFFFF6B35))
        Spacer(modifier = Modifier.height(8.dp))

        if (myDishes.isEmpty()) {
            EmptyState("你还没有选菜")
        } else {
            myDishes.forEach { item ->
                DishResultCard(item = item, accentColor = Color(0xFFFF6B35))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Divider ──
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 1.dp
            ) {
                Text("VS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // ── Partner Selection ──
        SectionHeader(emoji = "👧", title = "${partnerName}的选择", count = partnerDishes.size,
            color = Color(0xFF4CAF50))

        if (partnerDishes.isEmpty()) {
            EmptyState("${partnerName}还没有选菜")
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            partnerDishes.forEach { item ->
                DishResultCard(item = item, accentColor = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Summary Card ──
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 点餐摘要", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem("我的菜品", "${myDishes.size}道")
                    SummaryItem("对方菜品", "${partnerDishes.size}道")
                    SummaryItem("分类数", "${categories.size}类")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem("预计总耗时", "${totalTime}分钟")
                    SummaryItem("人均菜品", "${"%.1f".format(totalDishes / 2.0)}道")
                    SummaryItem("搭配评分", "${if (categories.size >= 3) "⭐⭐⭐" else if (categories.size >= 2) "⭐⭐" else "⭐"}")
                }
                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("包含分类：${categories.joinToString(" · ")}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Actions ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onBack() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("← 返回", fontSize = 14.sp)
            }
            Button(
                onClick = { viewModel.confirmMeal(); onNewMeal() },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("✅ 确认完成，写入记录", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(emoji: String, title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.1f)) {
            Text(
                "${count}道",
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun DishResultCard(item: com.myorderapp.domain.model.MealItem, accentColor: Color) {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(item.dishCategory)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 26.sp) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.dishName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${item.dishCategory} · ${item.cookTimeMin}分钟 · ${"⭐".repeat(item.difficulty)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(alpha = 0.1f)) {
                Text(
                    item.chosenByName,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
