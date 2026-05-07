package com.myorderapp.ui.dishlibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.ui.theme.CategoryDisplay
import com.myorderapp.ui.theme.whoLikesDisplay
import org.koin.androidx.compose.koinViewModel

data class LibraryDish(
    val id: String,
    val name: String,
    val source: String,
    val category: String,
    val cookTimeMin: Int,
    val whoLikes: String,
    val whoLikesColor: Color,
    val emoji: String,
    val bgColor: Color
)

@Composable
fun DishLibraryScreen(
    viewModel: DishLibraryViewModel = koinViewModel(),
    onDishClick: (String, String) -> Unit = { _, _ -> },
    onAddDishClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters = remember(uiState.dishes) {
        listOf("全部", "自建", "收藏") + uiState.dishes.map { it.category }.distinct().sorted()
    }

    val dishes = uiState.dishes.map { dish ->
        val (emoji, bg) = CategoryDisplay.emojiAndBg(dish.category)
        val (whoStr, whoColor) = whoLikesDisplay(dish.whoLikes)
        LibraryDish(
            id = dish.id, name = dish.name,
            source = if (dish.source == "custom") "自建" else "收藏",
            category = dish.category, cookTimeMin = dish.cookTimeMin,
            whoLikes = whoStr, whoLikesColor = whoColor,
            emoji = emoji, bgColor = bg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("菜品库", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            FilledTonalButton(
                onClick = onAddDishClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFF6B35))
            ) { Text("+ 添加", color = Color.White, fontSize = 14.sp) }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索菜品库...", fontSize = 12.sp) },
            shape = RoundedCornerShape(19.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { filter ->
                val isSelected = when (filter) {
                    "全部" -> uiState.sourceFilter == "全部" && uiState.categoryFilter == "全部"
                    "自建" -> uiState.sourceFilter == "自建"
                    "收藏" -> uiState.sourceFilter == "收藏"
                    else -> uiState.categoryFilter == filter
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        when (filter) {
                            "全部" -> { viewModel.onSourceFilterChanged("全部"); viewModel.onCategoryFilterChanged("全部") }
                            "自建" -> viewModel.onSourceFilterChanged("自建")
                            "收藏" -> viewModel.onSourceFilterChanged("收藏")
                            else -> viewModel.onCategoryFilterChanged(filter)
                        }
                    },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFF3E0), selectedLabelColor = Color(0xFFFF6B35)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = isSelected,
                        borderColor = if (isSelected) Color(0xFFFF6B35) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        selectedBorderColor = Color(0xFFFF6B35)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("共 ${dishes.size} 道菜", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dishes) { dish ->
                DishGridCard(dish = dish, onClick = {
                    onDishClick(dish.id, if (dish.source == "自建") "custom" else "external")
                })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DishGridCard(dish: LibraryDish, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(dish.bgColor),
                    contentAlignment = Alignment.Center
                ) { Text(dish.emoji, fontSize = 30.sp) }
                Spacer(modifier = Modifier.height(8.dp))
                Text(dish.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${dish.source} · ${dish.cookTimeMin}分钟",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dish.whoLikes, fontSize = 10.sp, color = dish.whoLikesColor)
            }
            if (dish.source == "自建") {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFFFF6B35)
                ) {
                    Text("自建", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
