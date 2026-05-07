package com.myorderapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.domain.model.Dish
import com.myorderapp.ui.theme.CategoryDisplay
import com.myorderapp.ui.theme.whoLikesDisplayHome
import org.koin.androidx.compose.koinViewModel

data class RecentDish(
    val id: String, val name: String, val category: String,
    val difficulty: Int, val cookTimeMin: Int,
    val whoLikes: String, val whoLikesColor: Color,
    val emoji: String, val bgColor: Color, val source: String = "custom"
)

fun Dish.toHomeRecent(): RecentDish {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(category)
    val (whoStr, whoColor) = whoLikesDisplayHome(whoLikes)
    return RecentDish(id = id, name = name, category = category,
        difficulty = difficulty, cookTimeMin = cookTimeMin,
        whoLikes = whoStr, whoLikesColor = whoColor,
        emoji = emoji, bgColor = bg, source = source)
}

// 根据时间生成问候语
private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..5 -> "夜深了 🌙"
        in 6..8 -> "早上好 ☀️"
        in 9..11 -> "上午好 🌤️"
        in 12..13 -> "中午好 ☀️"
        in 14..17 -> "下午好 🌈"
        in 18..20 -> "傍晚好 🌅"
        else -> "晚上好 🌙"
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onSearchClick: () -> Unit = {},
    onRandomClick: () -> Unit = {},
    onAddDishClick: () -> Unit = {},
    onDishClick: (String, String) -> Unit = { _, _ -> },
    onStartMeal: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentDishes = uiState.recentDishes.map { it.toHomeRecent() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ── Header ──
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            greeting(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "今天吃什么？",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35)
                        )
                    }
                    // 历史记录按钮
                    Surface(
                        onClick = onHistoryClick,
                        shape = CircleShape,
                        color = Color(0xFFFFF3E0)
                    ) {
                        Text("📋", fontSize = 18.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }

        // ── Hero Banner ──
        item {
            val meal = uiState.todayMeal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .clickable { onStartMeal() },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 渐变背景
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF6B35), Color(0xFFFF8A65))
                                )
                            )
                    )
                    // 装饰圆
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 30.dp, y = (-20).dp)
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-30).dp, y = 20.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            if (meal == null) "今天想吃点什么？"
                            else when (meal.status) {
                                "ordering" -> "点餐进行中..."
                                "confirmed" -> "双方已确认！"
                                "completed" -> "今天吃过了~ 🎉"
                                else -> "今天想吃点什么？"
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (meal == null) "来发起今日点餐吧"
                            else "点击查看详情",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White
                        ) {
                            Text(
                                if (meal == null) "🍽️  发起点餐"
                                else "👀  查看详情",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFFFF6B35)
                            )
                        }
                    }
                }
            }
        }

        // ── Quick Actions ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(
                    emoji = "🔍", label = "搜菜谱", caption = "全网搜索",
                    color = Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f),
                    onClick = onSearchClick
                )
                ActionCard(
                    emoji = "🎲", label = "摇一摇", caption = "随机推荐",
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.weight(1f),
                    onClick = onRandomClick
                )
                ActionCard(
                    emoji = "✏️", label = "加菜品", caption = "自定义添加",
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.weight(1f),
                    onClick = onAddDishClick
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Recent Dishes ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "最近菜品",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onHistoryClick) {
                    Text("全部 →", fontSize = 12.sp, color = Color(0xFFFF6B35))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(recentDishes) { dish ->
            RecentDishCard(
                dish = dish,
                onClick = { onDishClick(dish.id, dish.source) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ActionCard(
    emoji: String, label: String, caption: String,
    color: Color, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 30.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(caption, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentDishCard(dish: RecentDish, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(dish.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(dish.emoji, fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dish.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${dish.category} · ${"⭐".repeat(dish.difficulty)} · ${dish.cookTimeMin}分钟",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = dish.whoLikesColor.copy(alpha = 0.1f)
            ) {
                Text(
                    dish.whoLikes,
                    fontSize = 11.sp,
                    color = dish.whoLikesColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
