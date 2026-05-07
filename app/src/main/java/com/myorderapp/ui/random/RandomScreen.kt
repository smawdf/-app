package com.myorderapp.ui.random

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

    // 旋转动画
    val spinAngle = remember { Animatable(0f) }
    val spinScale = remember { Animatable(1f) }
    LaunchedEffect(uiState.isSpinning) {
        if (uiState.isSpinning) {
            spinScale.snapTo(1.08f)
            spinAngle.animateTo(
                targetValue = 1080f,
                animationSpec = tween(2000, easing = FastOutSlowInEasing)
            )
            spinAngle.snapTo(0f)
            spinScale.animateTo(1f, tween(300))
        }
    }
    val spinAngleValue by remember { derivedStateOf { spinAngle.value } }
    val spinScaleValue by remember { derivedStateOf { spinScale.value } }

    // 结果脉冲
    val resultScale = remember { Animatable(0f) }
    LaunchedEffect(uiState.selectedDish) {
        if (uiState.selectedDish != null && !uiState.isSpinning) {
            resultScale.snapTo(0.6f)
            resultScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
        }
    }
    val resultScaleValue by remember { derivedStateOf { resultScale.value } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "随机推荐",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            "不知道吃什么？摇一摇吧！",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 自定义筛选 ──
        Text(
            "筛选条件",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = uiState.categoryFilter,
                onValueChange = { viewModel.onCategoryFilterChanged(it) },
                label = { Text("分类") },
                placeholder = { Text("如：中餐、川菜") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = uiState.maxTimeFilter,
                onValueChange = { viewModel.onMaxTimeChanged(it) },
                label = { Text("时间(分钟)") },
                placeholder = { Text("最大时长") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 难度选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "难度：",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 4.dp)
            )
            for (i in 1..5) {
                Text(
                    text = if (i <= uiState.difficultyFilter && uiState.difficultyFilter > 0) "⭐" else "☆",
                    fontSize = 22.sp,
                    modifier = Modifier.clickable { viewModel.onDifficultyChanged(i) }
                )
            }
            if (uiState.difficultyFilter > 0) {
                Text(
                    text = "不限",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { viewModel.onDifficultyChanged(0) }
                        .padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 转盘区域 ──
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(spinScaleValue)
                .rotate(spinAngleValue),
            contentAlignment = Alignment.Center
        ) {
            // 外圈环
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            )
            // 中圈
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            )
            // 内圈
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
            )

            // 旋转中的文字
            if (uiState.isSpinning) {
                Text(
                    uiState.currentName.ifBlank { "？" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // 静止态 - 大号 emoji
                Text(
                    if (uiState.selectedDish != null) "🎉" else "🍳",
                    fontSize = 52.sp,
                    modifier = Modifier.scale(if (uiState.selectedDish != null) 1f else 1f)
                )
            }
        }

        // 转盘指示器 - 小三角
        Box(
            modifier = Modifier
                .offset(y = (-4).dp)
                .size(14.dp)
                .rotate(180f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 来自API标签
        if (uiState.isFromApi && !uiState.isSpinning && uiState.selectedDish != null) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "✨ 来自在线推荐",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── GO 按钮 ──
        Button(
            onClick = { viewModel.spin() },
            enabled = !uiState.isSpinning,
            modifier = Modifier
                .width(200.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (uiState.isSpinning) 0.dp else 4.dp
            )
        ) {
            Text(
                when {
                    uiState.isSpinning -> "转动中..."
                    uiState.selectedDish != null -> "🔄  再来一次！"
                    else -> "🎯  转一转！"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            "已随机 ${uiState.spinCount} 次 · 已尝 ${uiState.shownDishIds.size} 道不重复",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 推荐结果 ──
        if (uiState.selectedDish != null) {
            val dish = uiState.selectedDish!!
            val (emoji, bg) = CategoryDisplay.emojiAndBg(dish.category)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .scale(resultScaleValue)
            ) {
                Text(
                    "🎉 今天吃这个！",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 结果卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.cacheClickedDish(dish.id)
                            onDishClick(dish.id, dish.source)
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 顶部大图区域
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                .background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 56.sp)
                        }

                        Column(modifier = Modifier.padding(18.dp)) {
                            // 菜名 + 箭头
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    dish.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("→",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 元信息
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        dish.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    "${dish.cookTimeMin}分钟",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "⭐".repeat(dish.difficulty),
                                    fontSize = 11.sp
                                )
                            }

                            if (dish.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    dish.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 来源标签
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    when {
                                        dish.externalSource == "juhe" -> "聚合数据"
                                        dish.source == "builtin" -> "内置菜谱"
                                        dish.source == "custom" -> "自建菜品"
                                        else -> dish.externalSource ?: "推荐"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 再摇一次按钮
                OutlinedButton(
                    onClick = { viewModel.spin() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("🔄 再摇一次", style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── 候选列表 ──
            if (uiState.candidates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    "📋 更多候选",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.candidates
                        .filter { it.id != uiState.selectedDish?.id }
                        .take(4)
                        .forEach { dish ->
                            val (e, b) = CategoryDisplay.emojiAndBg(dish.category)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.cacheClickedDish(dish.id)
                                        onDishClick(dish.id, dish.source)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(b),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(e, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            dish.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${dish.cookTimeMin}分钟 · ${"⭐".repeat(dish.difficulty)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(180f).size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
