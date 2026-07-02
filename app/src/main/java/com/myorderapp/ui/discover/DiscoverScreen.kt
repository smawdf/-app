package com.myorderapp.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.myorderapp.ui.components.OrderCard
import com.myorderapp.ui.components.OrderPrimaryButton
import com.myorderapp.ui.components.OrderSearchField
import org.koin.androidx.compose.koinViewModel

private val DiscoverPrimary = Color(0xFF5F95B5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var detailItem by remember { mutableStateOf<DiscoverDishSearchItem?>(null) }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    uiState.pendingAddItem?.let { item ->
        AddToShopDialog(
            item = item,
            categories = uiState.categories,
            price = uiState.addPrice,
            category = uiState.addCategory,
            errorMessage = uiState.errorMessage,
            onPriceChange = viewModel::onAddPriceChanged,
            onCategoryChange = viewModel::onAddCategoryChanged,
            onDismiss = viewModel::dismissAddDialog,
            onConfirm = viewModel::confirmAddToMenu
        )
    }

    detailItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { detailItem = null }) {
            DiscoverDishDetailSheet(
                item = item,
                onAddToMenu = {
                    detailItem = null
                    viewModel.addToMenu(item)
                },
                onClose = { detailItem = null }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FB))
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "发现",
                    color = Color(0xFF1F2933),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "把每天吃饭变成两个人的小任务",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                OrderSearchField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    placeholder = "搜索菜品、做法、食材",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            uiState.errorMessage?.let {
                item {
                    Text(
                        text = "部分网络结果暂不可用",
                        color = Color(0xFF6B7885),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            when {
                uiState.query.isBlank() -> Unit

                uiState.isSearching -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                        color = DiscoverPrimary,
                        strokeWidth = 2.dp
                    )
                            Text(
                                text = "正在搜索菜品...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                uiState.results.isEmpty() -> {
                    item {
                        Text(
                            text = "没有找到相关菜品",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                else -> {
                    items(uiState.results, key = { it.id }) { result ->
                        DiscoverResultCard(
                            result = result,
                            onClick = { detailItem = result },
                            onAddToMenu = viewModel::addToMenu
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverResultCard(
    result: DiscoverDishSearchItem,
    onClick: () -> Unit,
    onAddToMenu: (DiscoverDishSearchItem) -> Unit
) {
    OrderCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE2EEF5)),
                contentAlignment = Alignment.Center
            ) {
                if (!result.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = result.imageUrl,
                        contentDescription = result.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = DiscoverPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    color = Color(0xFF1F2933),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = result.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onAddToMenu(result) },
                    enabled = !result.isAdded,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiscoverPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFF0ECE4),
                        disabledContentColor = Color(0xFF6B7885)
                    )
                ) {
                    Text(if (result.isAdded) "已在我的小店" else "加入我的小店")
                }
            }
        }
    }
}

@Composable
private fun AddToShopDialog(
    item: DiscoverDishSearchItem,
    categories: List<String>,
    price: String,
    category: String,
    errorMessage: String?,
    onPriceChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入我的小店", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = price,
                    onValueChange = onPriceChange,
                    label = { Text("售价") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("分类", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it }) { option ->
                        Surface(
                            onClick = { onCategoryChange(option) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (option == category) DiscoverPrimary else Color(0xFFEFF4F8)
                        ) {
                            Text(
                                text = option,
                                color = if (option == category) Color.White else Color(0xFF1F2933),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("自定义分类") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            OrderPrimaryButton(text = "确认加入", onClick = onConfirm)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DiscoverDishDetailSheet(
    item: DiscoverDishSearchItem,
    onAddToMenu: () -> Unit,
    onClose: () -> Unit
) {
    val suggestedPrice = "¥%.2f".format(item.price)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE2EEF5)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = DiscoverPrimary, modifier = Modifier.size(42.dp))
            }
        }
        Text(item.name, color = Color(0xFF1F2933), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(item.subtitle.ifBlank { "暂无描述" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text("建议售价 $suggestedPrice", color = DiscoverPrimary, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("关闭") }
            Button(
                onClick = onAddToMenu,
                enabled = !item.isAdded,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DiscoverPrimary, contentColor = Color.White)
            ) {
                Text(if (item.isAdded) "已在我的小店" else "加入我的小店")
            }
        }
    }
}
