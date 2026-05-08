package com.myorderapp.ui.meal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    // 系统返回键：Step 1 → Step 0
    BackHandler(enabled = uiState.step == 1) {
        viewModel.goBackToStep0()
    }

    if (uiState.step == 0) {
        // Step 0: 选择餐次
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 56.dp)
        ) {
            Text(
                "发起点餐",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "选好餐次，开始点菜吧",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            mealTypes.forEach { mt ->
                Card(
                    onClick = { viewModel.selectMealType(mt.type) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mt.emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                mt.label,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when (mt.type) {
                                    "breakfast" -> "豆浆油条 or 牛奶面包？"
                                    "lunch" -> "中午吃什么？世纪难题"
                                    "dinner" -> "丰盛一点，犒劳自己"
                                    "supper" -> "深夜放毒，快乐加倍"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Step 1: 点菜界面 — 双人左右布局
    val filteredDishes = remember(uiState.searchQuery, uiState.allDishes) {
        viewModel.getFilteredDishes()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.goBackToStep0() }) {
                Text("← 返回", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🍽️ 点菜中", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                mealTypes.find { it.type == uiState.mealType }?.label ?: uiState.mealType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = { Text("搜索菜品添加到点餐...", style = MaterialTheme.typography.bodySmall) },
            shape = RoundedCornerShape(22.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ── 双人左右面板 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 左侧：我的选择
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uiState.myName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.mySubmitted) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("已提交 ✓", style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    if (uiState.mySelections.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("点击菜品添加", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.mySelections) { item ->
                                MyDishChip(item, onRemove = { viewModel.removeMyDish(it) })
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${uiState.mySelections.size}道", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp, bottom = 8.dp))
                }
            }

            // 右侧：对方选择
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👧 ${uiState.partnerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.partnerSubmitted) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("已提交 ✓", style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    if (uiState.partnerSelections.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("等待选菜...", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.partnerSelections) { item ->
                                PartnerDishChip(item)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${uiState.partnerSelections.size}道", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp, bottom = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 菜品库 ──
        Text(
            "📋 菜品库 · 点击添加",
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .padding(horizontal = 20.dp)
        ) {
            items(filteredDishes.chunked(2)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { dish ->
                        val isSelected = uiState.mySelections.any { it.dishId == dish.id }
                        DishGridCard(
                            dish = dish, isSelected = isSelected,
                            onClick = { if (!isSelected) viewModel.addDish(dish) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "我已选 ${uiState.mySelections.size} 道菜",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (uiState.partnerSubmitted) {
                        Text("对方已提交选择", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    } else if (uiState.mySubmitted) {
                        Text("等待对方提交...", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = { viewModel.submitMySelection() },
                    enabled = uiState.mySelections.isNotEmpty() && !uiState.mySubmitted,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        if (uiState.mySubmitted) "已提交 ✓" else "提交(${uiState.mySelections.size}道)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.dishName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.cookTimeMin}分钟",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "✕",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onRemove(item.id) }.padding(4.dp)
        )
    }
}

@Composable
private fun PartnerDishChip(item: com.myorderapp.domain.model.MealItem) {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(item.dishCategory)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            item.dishName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${item.cookTimeMin}分钟",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 26.sp) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                dish.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                "${dish.cookTimeMin}分钟 · ${"⭐".repeat(dish.difficulty)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "✓ 已添加",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
