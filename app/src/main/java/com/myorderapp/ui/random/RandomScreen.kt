package com.myorderapp.ui.random

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.myorderapp.ui.theme.CategoryDisplay

@Composable
fun RandomScreen(
    viewModel: RandomViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onDishClick: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters = remember(uiState.candidates) {
        listOf("全部") + uiState.candidates.map { it.category }.distinct().sorted()
    }

    // 旋转动画
    val spinAngle = remember { Animatable(0f) }
    LaunchedEffect(uiState.isSpinning) {
        if (uiState.isSpinning) {
            spinAngle.animateTo(
                targetValue = 720f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
            spinAngle.snapTo(0f)
        }
    }
    val spinAngleValue by remember { derivedStateOf { spinAngle.value } }

    // 脉冲动画
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(uiState.selectedDish) {
        if (uiState.selectedDish != null && !uiState.isSpinning) {
            pulseScale.animateTo(1.1f, tween(200))
            pulseScale.animateTo(1f, tween(200))
        }
    }
    val pulseScaleValue by remember { derivedStateOf { pulseScale.value } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("← 返回", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
                modifier = Modifier.clickable { onBack() })
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("🎲 随机推荐", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("摇一摇，今天吃什么？", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        // 分类筛选
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { filter ->
                val selected = filter == uiState.selectedCategory
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.onCategorySelected(filter) },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 转盘动画区域
        Box(
            modifier = Modifier
                .size(240.dp)
                .rotate(spinAngleValue),
            contentAlignment = Alignment.Center
        ) {
            // 外圈装饰
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            )

            // 当前菜名（旋转时快速切换）
            if (uiState.isSpinning) {
                Text(
                    uiState.currentName.ifBlank { "？" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "🍳",
                    fontSize = 48.sp,
                    modifier = Modifier.scale(pulseScaleValue)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.isFromApi && !uiState.isSpinning) {
            Surface(shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                Text("来自在线随机抽取", fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GO 按钮
        Button(
            onClick = { viewModel.spin() },
            enabled = !uiState.isSpinning,
            modifier = Modifier
                .width(220.dp)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        ) {
            Text(
                if (uiState.isSpinning) "转动中..."
                else if (uiState.selectedDish != null) "🔄 再来一次！"
                else "🎯 转一转！",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("已摇 ${uiState.spinCount} 次", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        // 推荐结果
        if (uiState.selectedDish != null) {
            Text("🎉 本次推荐", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            val dish = uiState.selectedDish!!
            val (emoji, bg) = CategoryDisplay.emojiAndBg(dish.category)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.cacheClickedDish(dish.id)
                        onDishClick(dish.id, dish.source)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 图片或emoji
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 64.sp)
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                dish.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("→", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${dish.category} · ${dish.cookTimeMin}分钟 · ${"⭐".repeat(dish.difficulty)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (dish.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                dish.notes,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                when {
                                    dish.externalSource == "spoonacular" -> "Spoonacular"
                                    dish.externalSource == "juhe" -> "聚合数据"
                                    dish.source == "builtin" -> "内置菜谱"
                                    dish.source == "custom" -> "自建菜品"
                                    else -> dish.externalSource ?: "外部"
                                },
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.spin() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text("🔄 再摇一次", fontSize = 14.sp)
                }
            }
        }

        // 候选列表
        if (uiState.candidates.isNotEmpty() && uiState.selectedDish != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("📋 候选菜品", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))

            uiState.candidates.take(4).forEach { dish ->
                val (emoji, bg) = CategoryDisplay.emojiAndBg(dish.category)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            viewModel.cacheClickedDish(dish.id)
                            onDishClick(dish.id, dish.source)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 24.sp) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dish.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${dish.cookTimeMin}分钟 · ${"⭐".repeat(dish.difficulty)}",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("→", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
