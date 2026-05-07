package com.myorderapp.ui.meal

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.myorderapp.ui.theme.CategoryDisplay

data class MealTypeOption(val type: String, val label: String, val emoji: String)

@Composable
fun StartMealScreen(
    viewModel: MealViewModel = koinViewModel(),
    initialMealType: String = "lunch",
    onResultClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val mealTypes = listOf(
        MealTypeOption("breakfast", "早餐", "🌅"),
        MealTypeOption("lunch", "午餐", "☀️"),
        MealTypeOption("dinner", "晚餐", "🌙"),
        MealTypeOption("supper", "夜宵", "🌃")
    )

    LaunchedEffect(uiState.bothSubmitted) {
        if (uiState.bothSubmitted && uiState.mealId.isNotBlank()) {
            onResultClick(uiState.mealId)
        }
    }

    if (uiState.step == 0) {
        // Step 0: 选择餐次
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 56.dp)
        ) {
            Text("发起点餐", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("选好餐次，开始点菜吧", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))

            mealTypes.forEach { mt ->
                Card(
                    onClick = { viewModel.selectMealType(mt.type) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mt.emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(mt.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                when (mt.type) {
                                    "breakfast" -> "豆浆油条 or 牛奶面包？"
                                    "lunch" -> "中午吃什么？世纪难题"
                                    "dinner" -> "丰盛一点，犒劳自己"
                                    "supper" -> "深夜放毒，快乐加倍"
                                    else -> ""
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Step 1: 点菜界面
    val filteredDishes = remember(uiState.searchQuery, uiState.allDishes) {
        viewModel.getFilteredDishes()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 56.dp, bottom = 100.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🍽️ 点菜中", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text(
                        mealTypes.find { it.type == uiState.mealType }?.label ?: uiState.mealType,
                        fontSize = 13.sp, color = Color(0xFFFF6B35)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                placeholder = { Text("搜索菜品添加到点餐...", fontSize = 13.sp) },
                shape = RoundedCornerShape(22.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // My selections panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧑 我的选择 (${uiState.mySelections.size}道)",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (uiState.mySubmitted) {
                            Surface(shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f)) {
                                Text("已提交 ✓", fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = Color(0xFF4CAF50))
                            }
                        }
                    }

                    if (uiState.mySelections.isEmpty()) {
                        Text("从下方搜索并添加菜品 👇", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.mySelections.forEach { item ->
                            MyDishChip(item, onRemove = { viewModel.removeMyDish(it) })
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Partner selections panel — 只在有真实对方数据时才显示
        item {
            val hasPartnerActivity = uiState.partnerSelections.isNotEmpty() || uiState.partnerSubmitted
            AnimatedVisibility(
                visible = hasPartnerActivity,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👧 ${uiState.partnerName}的选择 (${uiState.partnerSelections.size}道)",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (uiState.partnerSubmitted) {
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.1f)) {
                                    Text("已提交 ✓", fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = Color(0xFF4CAF50))
                                }
                            }
                        }

                        if (uiState.partnerSelections.isEmpty()) {
                            Text("${uiState.partnerName}已加入，尚未选菜", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.partnerSelections.forEach { item ->
                                PartnerDishChip(item)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Available dishes header
        item {
            Text(
                "📋 菜品库 · 点击添加",
                modifier = Modifier.padding(horizontal = 20.dp),
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Dish grid
        items(filteredDishes.chunked(2)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { dish ->
                    val isSelected = uiState.mySelections.any { it.dishId == dish.id }
                    DishGridCard(
                        dish = dish,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) viewModel.addDish(dish)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Bottom bar
    if (uiState.step == 1) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "我已选 ${uiState.mySelections.size} 道菜",
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                    if (uiState.partnerSubmitted) {
                        Text("对方已提交选择", fontSize = 11.sp, color = Color(0xFF4CAF50))
                    } else if (uiState.mySubmitted) {
                        Text("等待对方提交...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = { viewModel.submitMySelection() },
                    enabled = uiState.mySelections.isNotEmpty() && !uiState.mySubmitted,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
                ) {
                    Text(
                        if (uiState.mySubmitted) "已提交 ✓" else "提交(${uiState.mySelections.size}道)",
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MyDishChip(item: com.myorderapp.domain.model.MealItem, onRemove: (String) -> Unit) {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(item.dishCategory)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(bg),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.dishName, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.cookTimeMin}分钟", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("✕", fontSize = 16.sp, color = Color(0xFFBDBDBD),
            modifier = Modifier.clickable { onRemove(item.id) }.padding(4.dp))
    }
}

@Composable
private fun PartnerDishChip(item: com.myorderapp.domain.model.MealItem) {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(item.dishCategory)
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(bg),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(item.dishName, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("${item.cookTimeMin}分钟", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DishGridCard(
    dish: com.myorderapp.domain.model.Dish,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(dish.category)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B35)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 26.sp) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                dish.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                "${dish.cookTimeMin}分钟 · ${"⭐".repeat(dish.difficulty)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("✓ 已添加", fontSize = 10.sp, color = Color(0xFFFF6B35),
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
