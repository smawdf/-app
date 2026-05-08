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
    val dataSources = remember(uiState.results) {
        val sourceLabels = uiState.results.map { dish ->
            when (dish.externalSource) {
                "juhe" -> "聚合数据"
                "tianapi" -> "天行数据"
                "jisuapi" -> "极速数据"
                else -> if (dish.source == "custom" || dish.source == "builtin") "本地" else null
            }
        }.filterNotNull().distinct()
        listOf("全部") + sourceLabels
    }

    val searchResults = uiState.results.map { dish ->
        val defaultEmoji = "🍽️"
        val defaultBgColor = Color(0xFFF5F0E8)
        val sourceColor = when (dish.externalSource) {
            "juhe" -> Color(0xFFFF6B35)
            "tianapi" -> Color(0xFF2196F3)
            "jisuapi" -> Color(0xFF9C27B0)
            else -> if (dish.source == "custom") Color(0xFF4CAF50) else Color(0xFFFF6B35)
        }
        SearchResult(
            id = dish.id, name = dish.name, category = dish.category,
            cookTimeMin = dish.cookTimeMin, difficulty = dish.difficulty,
            description = dish.notes.ifBlank { "" },
            source = when {
                dish.externalSource == "juhe" -> "聚合数据"
                dish.externalSource == "tianapi" -> "天行数据"
                dish.externalSource == "jisuapi" -> "极速数据"
                dish.source == "custom" -> "自建"
                dish.source == "builtin" -> "内置"
                else -> dish.externalSource ?: "外部"
            },
            sourceColor = sourceColor, emoji = defaultEmoji, bgColor = defaultBgColor,
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

            if (uiState.isSearching) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            PrimaryScrollableTabRow(
                selectedTabIndex = dataSources.indexOf(uiState.selectedSource).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                dataSources.forEach { source ->
                    val selected = source == uiState.selectedSource
                    Tab(
                        selected = selected,
                        onClick = { viewModel.onSourceSelected(source) },
                        text = {
                            Text(
                                source,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
                    "${result.cookTimeMin}分钟 · ${"⭐".repeat(result.difficulty)}",
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
