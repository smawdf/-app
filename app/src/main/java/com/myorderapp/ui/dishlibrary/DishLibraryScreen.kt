package com.myorderapp.ui.dishlibrary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.myorderapp.domain.model.Dish
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
    val imageUrl: String? = null,
    val bgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishLibraryScreen(
    viewModel: DishLibraryViewModel = koinViewModel(),
    onDishClick: (String, String) -> Unit = { _, _ -> },
    onAddDishClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedDishes = viewModel.pagedDishes.collectAsLazyPagingItems()

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
            Text("菜品库", style = MaterialTheme.typography.displayLarge)
            FilledTonalButton(
                onClick = onAddDishClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("添加", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索菜品库...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 来源筛选
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            var sourceExpanded by remember { mutableStateOf(false) }
            val sourceOptions = listOf("全部", "我的菜单", "收藏")
            ExposedDropdownMenuBox(
                expanded = sourceExpanded,
                onExpandedChange = { sourceExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = uiState.sourceFilter,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelMedium
                )
                ExposedDropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                    sourceOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                viewModel.onSourceFilterChanged(opt)
                                sourceExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "共 ${pagedDishes.itemCount} 道菜",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val refreshState = pagedDishes.loadState.refresh
            val appendState = pagedDishes.loadState.append

            if (refreshState is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (refreshState is LoadState.Error) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryMessage(
                        title = "菜品加载失败",
                        body = refreshState.error.message ?: "请稍后重试",
                        actionText = "重试",
                        onAction = { pagedDishes.retry() }
                    )
                }
            }

            if (refreshState is LoadState.NotLoading && pagedDishes.itemCount == 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryMessage(
                        title = "还没有菜品",
                        body = if (uiState.searchQuery.isBlank()) "添加一道常吃的菜吧" else "换个关键词试试",
                        actionText = if (uiState.searchQuery.isBlank()) "添加" else null,
                        onAction = if (uiState.searchQuery.isBlank()) onAddDishClick else null
                    )
                }
            }

            items(
                count = pagedDishes.itemCount,
                key = { index -> pagedDishes[index]?.id ?: "dish_$index" }
            ) { index ->
                val dish = pagedDishes[index]?.toLibraryDish()
                if (dish != null) {
                    DishGridCard(
                        dish = dish,
                        onClick = {
                            onDishClick(dish.id, if (dish.source == "我的菜单") "custom" else "external")
                        },
                        modifier = Modifier.animateItem(),
                        onDeleteClick = if (dish.source == "我的菜单") {{ viewModel.deleteDish(dish.id) }} else null
                    )
                }
            }

            if (appendState is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
            if (appendState is LoadState.Error) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TextButton(
                        onClick = { pagedDishes.retry() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("继续加载失败，点此重试")
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun LibraryMessage(
    title: String,
    body: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionText != null && onAction != null) {
            FilledTonalButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                Text(actionText)
            }
        }
    }
}

private fun Dish.toLibraryDish(): LibraryDish {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(category)
    val (whoStr, whoColor) = whoLikesDisplay(whoLikes)
    return LibraryDish(
        id = id,
        name = name,
        source = when (source) {
            "custom" -> "我的菜单"
            "builtin" -> "内置"
            else -> "收藏"
        },
        category = category,
        cookTimeMin = cookTimeMin,
        whoLikes = whoStr,
        whoLikesColor = whoColor,
        emoji = emoji,
        imageUrl = imageUrl,
        bgColor = bg
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DishGridCard(
    dish: LibraryDish,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除「${dish.name}」吗？") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDeleteClick?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { if (dish.source == "我的菜单") showMenu = true }
            ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box {
                Column(modifier = Modifier.padding(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(100.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(dish.bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dish.imageUrl != null) {
                            AsyncImage(
                                model = dish.imageUrl,
                                contentDescription = dish.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(dish.emoji, fontSize = 30.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        dish.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${dish.source} · ${dish.cookTimeMin}分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                Text(
                    dish.whoLikes,
                    style = MaterialTheme.typography.labelSmall,
                    color = dish.whoLikesColor
                )
            }
            if (dish.source == "我的菜单") {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "我的菜单",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

        // Long-press menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; showDeleteDialog = true }
            )
        }
    }
}
