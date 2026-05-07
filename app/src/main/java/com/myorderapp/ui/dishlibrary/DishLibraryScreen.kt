package com.myorderapp.ui.dishlibrary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
            emoji = emoji, imageUrl = dish.imageUrl, bgColor = bg
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
            Text("菜品库", style = MaterialTheme.typography.displayLarge)
            FilledTonalButton(
                onClick = onAddDishClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("+ 添加", style = MaterialTheme.typography.labelLarge) }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索菜品库...", style = MaterialTheme.typography.bodySmall) },
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

        // 来源筛选 + 分类筛选 下拉框
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 来源下拉
            var sourceExpanded by remember { mutableStateOf(false) }
            val sourceOptions = listOf("全部", "自建", "收藏")
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
                                if (opt == "全部") viewModel.onCategoryFilterChanged("全部")
                                sourceExpanded = false
                            }
                        )
                    }
                }
            }

            // 分类下拉
            var catExpanded by remember { mutableStateOf(false) }
            val categories = remember(uiState.dishes) {
                listOf("全部") + uiState.dishes.map { it.category }.distinct().sorted()
            }
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = uiState.categoryFilter,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelMedium
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.onCategoryFilterChanged(cat)
                                catExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "共 ${dishes.size} 道菜",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dishes, key = { it.id }) { dish ->
                DishGridCard(
                    dish = dish,
                    onClick = {
                        onDishClick(dish.id, if (dish.source == "自建") "custom" else "external")
                    },
                    modifier = Modifier.animateItem(),
                    onDeleteClick = if (dish.source == "自建") {{ viewModel.deleteDish(dish.id) }} else null
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
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
                onLongClick = { if (dish.source == "自建") showMenu = true }
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
            if (dish.source == "自建") {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "自建",
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
                text = { Text("🗑 删除", color = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; showDeleteDialog = true }
            )
        }
    }
}
