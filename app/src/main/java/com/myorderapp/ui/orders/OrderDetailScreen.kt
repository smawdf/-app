package com.myorderapp.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myorderapp.ui.util.yuanText
import org.koin.androidx.compose.koinViewModel

@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit = {},
    viewModel: OrderDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.load(orderId)
    }

    val order = uiState.order

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("订单详情", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (order != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(order.shopName, fontWeight = FontWeight.Bold)
                    Text(order.addressSnapshot)
                    Text(order.buyerNote.ifBlank { "暂无备注" })
                    Text(order.status.toOrderStatusText(), color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("订单进度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(order.timeline) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.isCompleted) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(entry.title, fontWeight = FontWeight.Bold)
                            Text(entry.timestamp, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(order.items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.menuItemName, fontWeight = FontWeight.Bold)
                            Text("${item.quantity} 份 × ${yuanText(item.unitPrice)}")
                        }
                    }
                }
            }
        }
    }
}

private fun String.toOrderStatusText(): String = when (this) {
    "submitted" -> "已提交"
    "confirmed" -> "已接单"
    "delivering" -> "配送中"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    else -> this
}
