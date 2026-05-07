package com.myorderapp.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import com.myorderapp.ui.theme.CategoryDisplay

data class SearchResult(
    val id: String,
    val name: String,
    val category: String,
    val cookTimeMin: Int,
    val difficulty: Int,
    val description: String,
    val source: String,
    val sourceColor: Color,
    val emoji: String,
    val bgColor: Color,
    val imageUrl: String? = null
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onDishClick: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = remember(uiState.results) {
        listOf("全部") + uiState.results.map { it.category }.distinct().sorted()
    }

    val searchResults = uiState.results.map { dish ->
        val emoji = CategoryDisplay.emoji(dish.category)
        val bgColor = CategoryDisplay.bgColor(dish.category)
        val sourceColor = when (dish.externalSource) {
            "spoonacular" -> Color(0xFF2196F3)
            "juhe" -> Color(0xFFFF6B35)
            else -> if (dish.source == "custom") Color(0xFF4CAF50) else Color(0xFFFF6B35)
        }
        SearchResult(
            id = dish.id, name = dish.name, category = dish.category,
            cookTimeMin = dish.cookTimeMin, difficulty = dish.difficulty,
            description = dish.notes.ifBlank { "${dish.category}风味" },
            source = when {
                dish.externalSource == "spoonacular" -> "Spoonacular"
                dish.externalSource == "juhe" -> "聚合数据"
                dish.source == "custom" -> "自建"
                dish.source == "builtin" -> "内置"
                else -> dish.externalSource ?: "外部"
            },
            sourceColor = sourceColor, emoji = emoji, bgColor = bgColor,
            imageUrl = dish.imageUrl
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 56.dp, bottom = 16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("搜索菜谱", style = MaterialTheme.typography.displayLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = { Text("搜索菜品、食材...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "搜索中...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (uiState.results.isNotEmpty()) {
                    Text(
                        "共 ${uiState.results.size} 条结果",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        uiState.sources.forEach { src ->
                            val isSpoonacular = src.contains("Spoonacular")
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSpoonacular)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    src,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (isSpoonacular)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.clickable { viewModel.dismissError() }
                ) {
                    Text(
                        uiState.errorMessage!!,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    val isSelected = category == uiState.selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = isSelected,
                            borderColor = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.results.isNotEmpty()) {
            item {
                Text("搜索结果", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(searchResults) { result ->
            SearchResultCard(
                result = result,
                onClick = {
                    viewModel.cacheClickedDish(result.id)
                    onDishClick(result.id, "external")
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SearchResultCard(result: SearchResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(result.bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (result.imageUrl != null) {
                    AsyncImage(
                        model = result.imageUrl,
                        contentDescription = result.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(result.emoji, fontSize = 30.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${result.category} · ${result.cookTimeMin}分钟 · ${"⭐".repeat(result.difficulty)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    result.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = result.sourceColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        result.source,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = result.sourceColor
                    )
                }
            }
        }
    }
}
