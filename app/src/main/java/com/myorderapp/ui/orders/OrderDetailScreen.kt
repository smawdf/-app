package com.myorderapp.ui.orders

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.OrderTimelineEntry
import com.myorderapp.ui.components.CandyCoinIcon
import com.myorderapp.ui.components.CozyCard
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyPill
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.util.yuanText
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit = {},
    viewModel: OrderDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.load(orderId)
    }

    val order = uiState.order
    val isHistoricalOrder = order != null && order.pairId != uiState.activePairId
    val canAdvanceOrder = uiState.isCaretaker && !isHistoricalOrder
    val nextActionText = order?.status?.nextActionText()?.takeIf { canAdvanceOrder }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEF8F2))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OrderDetailTopBar(onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                uiState.message?.let { message ->
                    item {
                        CozyCard(
                            containerColor = Color.White.copy(alpha = 0.56f),
                            borderColor = Color.White.copy(alpha = 0.72f),
                            radius = 24
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CozyRose)
                                Text(message, color = CozyCocoa, modifier = Modifier.weight(1f))
                                TextButton(onClick = viewModel::dismissMessage) {
                                    Text("知道了", color = CozyRose)
                                }
                            }
                        }
                    }
                }

                if (order == null) {
                    item {
                        CozyCard(
                            containerColor = Color.White.copy(alpha = 0.58f),
                            borderColor = Color.White.copy(alpha = 0.72f),
                            radius = 24
                        ) {
                            Text("正在读取订单...", color = CozyMuted)
                        }
                    }
                } else {
                    item { OrderSummaryCard(order = order) }

                    if (!isHistoricalOrder && !canAdvanceOrder && order.status in setOf("submitted", "confirmed")) {
                        item { CaretakerOnlyCard() }
                    }

                    if (!isHistoricalOrder && (nextActionText != null || order.status !in setOf("completed", "cancelled"))) {
                        item {
                            OrderActionRow(
                                nextActionText = nextActionText,
                                canCancel = order.status !in setOf("completed", "cancelled"),
                                onAdvance = viewModel::advanceStatus,
                                onCancel = viewModel::cancelOrder
                            )
                        }
                    }

                    item { TimelineCard(order = order) }
                    item { OrderItemsCard(order = order) }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = CozyRose)
        }
        Text(
            text = "订单详情",
            color = CozyRose,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun OrderSummaryCard(order: OrderRecord) {
    CozyCard(
        containerColor = Color.White.copy(alpha = 0.62f),
        borderColor = Color.White.copy(alpha = 0.72f),
        radius = 24,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFD1DC).copy(alpha = 0.62f),
                    modifier = Modifier.size(52.dp)
                ) {
                    AsyncImage(
                        model = order.shopCoverUrl.takeIf { it.isNotBlank() } ?: com.myorderapp.R.drawable.shop_banner_stitch,
                        contentDescription = "${order.shopName.ifBlank { "店铺" }}的头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.shopName, color = CozyCocoa, fontWeight = FontWeight.Black)
                }
                CozyPill(order.status.toOrderStatusText(), selected = true, color = CozyRose)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF8F5),
                    modifier = Modifier.size(30.dp)
                ) {
                    if (order.buyerAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = order.buyerAvatarUrl,
                            contentDescription = "${order.buyerName.ifBlank { "点单人" }}的头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Filled.RestaurantMenu,
                            contentDescription = null,
                            tint = CozyRose,
                            modifier = Modifier.padding(7.dp).size(16.dp)
                        )
                    }
                }
                Text(order.buyerDetailText(), color = CozyRose, fontWeight = FontWeight.SemiBold)
            }
            Text(order.addressSnapshot.ifBlank { "小饭桌信息待补充" }, color = CozyMuted)
            Text(order.buyerNote.ifBlank { "暂无备注" }, color = CozyMuted)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("合计", color = CozyCocoa, fontWeight = FontWeight.Black)
                Text(yuanText(order.totalPrice), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    CandyCoinIcon(modifier = Modifier.size(20.dp))
                    Text("糖糖币", color = CozyMuted)
                }
                Text(
                    text = if (order.status == "cancelled") "已返还 ${order.candyCoinsSpent} 枚" else "消耗 ${order.candyCoinsSpent} 枚",
                    color = if (order.status == "cancelled") CozyMuted else CozyRose,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CaretakerOnlyCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFD1DC).copy(alpha = 0.52f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("待饲养员确认", color = CozyCocoa, fontWeight = FontWeight.Black)
            Text("订单已送达，只有饲养员可以确认接单并开始准备。", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OrderActionRow(
    nextActionText: String?,
    canCancel: Boolean,
    onAdvance: () -> Unit,
    onCancel: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        nextActionText?.let { actionText ->
            GradientOrderActionButton(
                text = actionText,
                onClick = onAdvance,
                modifier = Modifier.weight(1f)
            )
        }
        if (canCancel) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB85C5C))
            ) {
                Text("取消订单", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GradientOrderActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF894C5C),
        modifier = modifier
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color(0xFFFFFCF8), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimelineCard(order: OrderRecord) {
    val entries = order.progressTimelineEntries()
    CozyCard(
        containerColor = Color.White.copy(alpha = 0.58f),
        borderColor = Color.White.copy(alpha = 0.72f),
        radius = 24
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("订单进度", color = CozyCocoa, fontWeight = FontWeight.Black)
            if (entries.isEmpty()) {
                Text("暂无进度记录", color = CozyMuted)
            } else {
                entries.forEach { entry ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (entry.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (entry.isCompleted) CozyRose else CozyMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(entry.title, color = CozyCocoa, fontWeight = FontWeight.SemiBold)
                            if (entry.timestamp.isNotBlank()) {
                                Text(entry.timestamp, color = CozyMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItemsCard(order: OrderRecord) {
    CozyCard(
        containerColor = Color.White.copy(alpha = 0.62f),
        borderColor = Color.White.copy(alpha = 0.72f),
        radius = 24,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("菜品明细", color = CozyCocoa, fontWeight = FontWeight.Black)
                Text("共 ${order.items.sumOf { it.quantity }} 份", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
            }
            order.items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CozyRose.copy(alpha = 0.08f))
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.menuItemName, color = CozyCocoa, fontWeight = FontWeight.SemiBold)
                        Text("${item.quantity} 份 x ${yuanText(item.unitPrice)}", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(yuanText(item.subtotal), color = CozyCocoa, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun String.toOrderStatusText(): String = when (this) {
    "submitted", "confirmed" -> "待饲养员确认"
    "preparing", "delivering" -> "准备中"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    else -> this
}

private fun String.nextActionText(): String? = when (this) {
    "submitted", "confirmed" -> "确认接单"
    "preparing", "delivering" -> "完成这顿饭"
    else -> null
}

private val orderProgressSteps = listOf(
    "submitted" to "待饲养员确认",
    "preparing" to "准备中",
    "completed" to "已完成"
)

private fun OrderRecord.progressTimelineEntries(): List<OrderTimelineEntry> {
    if (status == "cancelled") {
        return listOf(
            OrderTimelineEntry(title = "待饲养员确认", timestamp = createdAt, isCompleted = true),
            OrderTimelineEntry(title = "已取消", timestamp = "", isCompleted = true)
        )
    }
    val normalizedStatus = when (status) {
        "confirmed" -> "submitted"
        "delivering" -> "preparing"
        else -> status
    }
    val currentIndex = orderProgressSteps.indexOfFirst { it.first == normalizedStatus }.coerceAtLeast(0)
    return orderProgressSteps.mapIndexed { index, (step, title) ->
        val isCompleted = index <= currentIndex
        val matchingEntry = timeline.firstOrNull { it.title == title || it.title == step.toOrderStatusText() }
        val timestamp = when {
            !isCompleted -> ""
            index == 0 -> matchingEntry?.timestamp?.takeIf { it.isNotBlank() } ?: createdAt
            matchingEntry != null && matchingEntry.timestamp.isNotBlank() && matchingEntry.timestamp != createdAt -> matchingEntry.timestamp
            else -> ""
        }
        OrderTimelineEntry(
            title = title,
            timestamp = timestamp,
            isCompleted = isCompleted
        )
    }
}

private fun OrderRecord.buyerDetailText(): String {
    val buyer = buyerName.ifBlank { "对方" }
    val count = items.sumOf { it.quantity }.coerceAtLeast(items.size)
    return "$buyer 点了 $count 道菜"
}
