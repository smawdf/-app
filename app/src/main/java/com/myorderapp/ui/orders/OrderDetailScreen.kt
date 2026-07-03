package com.myorderapp.ui.orders

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myorderapp.ui.util.yuanText
import org.koin.androidx.compose.koinViewModel

private const val COUPLE_HOME_PREFS = "couple_home_prefs"
private const val KEY_SELECTED_ROLE = "selected_role"
private const val ROLE_CARETAKER = "caretaker"

@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit = {},
    viewModel: OrderDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(orderId) {
        viewModel.load(orderId)
    }

    val order = uiState.order
    val nextActionText = order?.status?.nextActionText()
    val selectedRole = context.getSharedPreferences(COUPLE_HOME_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_SELECTED_ROLE, null)
    val canAdvanceOrder = selectedRole == ROLE_CARETAKER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("订单详情", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        uiState.message?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(message, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::dismissMessage) {
                        Text("知道了")
                    }
                }
            }
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
            if (nextActionText != null && !canAdvanceOrder) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "只有饲养员可以更新订单进度",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "请先在首页切换为饲养员，再处理这份点菜单",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (nextActionText != null || order.status !in setOf("completed", "cancelled")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    nextActionText?.let { actionText ->
                        Button(
                            onClick = { viewModel.advanceStatus(canAdvance = canAdvanceOrder) },
                            enabled = canAdvanceOrder,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(actionText)
                        }
                    }
                    if (order.status !in setOf("completed", "cancelled")) {
                        TextButton(
                            onClick = viewModel::cancelOrder,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB85C5C))
                        ) {
                            Text("取消订单")
                        }
                    }
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
    "confirmed" -> "饲养员已接单"
    "delivering" -> "准备中"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    else -> this
}

private fun String.nextActionText(): String? = when (this) {
    "submitted" -> "饲养员接单"
    "confirmed" -> "开始准备"
    "delivering" -> "完成这顿饭"
    else -> null
}
