package com.myorderapp.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.ui.components.CandyCoinIcon
import com.myorderapp.ui.components.CozyMainTopBar
import com.myorderapp.ui.components.CozyMotionVisibility
import com.myorderapp.ui.components.CozyPage
import com.myorderapp.ui.components.OrderGuidanceEmptyState
import com.myorderapp.ui.theme.OnSurface
import com.myorderapp.ui.theme.OnSurfaceVariant
import com.myorderapp.ui.theme.OutlineVariant
import com.myorderapp.ui.theme.Primary
import com.myorderapp.ui.theme.Secondary
import com.myorderapp.ui.theme.SecondaryContainer
import com.myorderapp.ui.theme.SurfaceContainerLow
import com.myorderapp.ui.theme.SurfaceVariant
import com.myorderapp.ui.theme.Tertiary
import com.myorderapp.ui.theme.TertiaryContainer
import org.koin.androidx.compose.koinViewModel

private enum class OrderFilter(val label: String) {
    ALL("全部"),
    PENDING("待确认"),
    COMPLETED("已完成"),
    CANCELLED("已取消")
}

private val OrderSurface = Color(0xFFFEF8F2)

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = koinViewModel(),
    onOrderClick: (String) -> Unit = {},
    onGoOrderingClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(OrderFilter.ALL) }
    val visibleOrders = remember(uiState.orders, selectedFilter) {
        when (selectedFilter) {
            OrderFilter.ALL -> uiState.orders
            OrderFilter.PENDING -> uiState.orders.filter { it.status != "completed" && it.status != "cancelled" }
            OrderFilter.COMPLETED -> uiState.orders.filter { it.status == "completed" }
            OrderFilter.CANCELLED -> uiState.orders.filter { it.status == "cancelled" }
        }
    }

    CozyPage(decorative = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 172.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                userScrollEnabled = visibleOrders.size > 3
            ) {
                item {
                    CozyMotionVisibility {
                        OrdersFilterTabs(
                            selected = selectedFilter,
                            onSelected = { selectedFilter = it }
                        )
                    }
                }

                if (visibleOrders.isEmpty()) {
                    item {
                        CozyMotionVisibility(delayMillis = 40) {
                            EmptyOrdersState(
                                filter = selectedFilter,
                                onGoOrderingClick = onGoOrderingClick
                            )
                        }
                    }
                } else {
                    items(visibleOrders, key = { it.id }) { order ->
                        CozyMotionVisibility(delayMillis = (visibleOrders.indexOf(order).coerceAtMost(4)) * 28) {
                            StitchOrderCard(order = order, onClick = { onOrderClick(order.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersTopBar() {
    CozyMainTopBar(title = "订单 - 甜蜜点菜记录")
    return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .background(OrderSurface.copy(alpha = 0.94f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "喜欢",
                tint = Primary
            )
        }
        Text(
            text = "订单 - 甜蜜点菜记录",
            color = Primary,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 32.sp),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "通知",
                tint = Primary
            )
        }
    }
}

@Composable
private fun OrdersFilterTabs(
    selected: OrderFilter,
    onSelected: (OrderFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(OrderFilter.entries.toList(), key = { it.name }) { filter ->
            HandDrawnTab(
                text = filter.label,
                selected = filter == selected,
                onClick = { onSelected(filter) }
            )
        }
    }
}

@Composable
private fun HandDrawnTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Surface(
        modifier = Modifier
            .height(40.dp)
            .scale(if (pressed) 0.96f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Primary else Color(0xFFFFFCF8),
        border = BorderStroke(1.dp, if (selected) Secondary else OutlineVariant)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else Secondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun EmptyOrdersState(
    filter: OrderFilter,
    onGoOrderingClick: () -> Unit
) {
    val title = when (filter) {
        OrderFilter.ALL -> "暂无订单哦~"
        OrderFilter.PENDING -> "暂时没有待确认订单"
        OrderFilter.COMPLETED -> "还没有完成的点菜记录"
        OrderFilter.CANCELLED -> "还没有取消的点菜记录"
    }
    OrderGuidanceEmptyState(
        title = title,
        subtitle = "点菜后，这里会显示你们的小饭桌记录",
        actionText = "去点菜",
        onAction = onGoOrderingClick
    )
}

@Composable
private fun StitchOrderCard(
    order: OrderRecord,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val completed = order.status == "completed"
    val cancelled = order.status == "cancelled"
    val cardColor = if (completed) SurfaceContainerLow.copy(alpha = 0.92f) else SecondaryContainer.copy(alpha = 0.42f)
    val badge = order.status.toOrderBadgeText(order.buyerRole)
    val message = order.status.toOrderMessage(order.buyerName)
    val dishSummary = order.items
        .take(3)
        .joinToString("、") { it.menuItemName }
        .ifBlank { order.buyerNote.ifBlank { "还没有菜品明细" } }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.985f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(1.dp, Secondary.copy(alpha = 0.58f)),
        shadowElevation = 0.dp
    ) {
        Box {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = RoundedCornerShape(bottomStart = 14.dp),
                color = when {
                    cancelled -> SurfaceVariant
                    completed -> SurfaceVariant
                    order.buyerRole.isEaterRole() -> TertiaryContainer
                    else -> Secondary
                }
            ) {
                Text(
                    text = badge,
                    color = when {
                        cancelled || completed -> OnSurfaceVariant
                        order.buyerRole.isEaterRole() -> Color(0xFF753C27)
                        else -> Color.White
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OrderAvatar(order = order)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.buyerName.ifBlank { if (order.buyerRole == "keeper") "饲养员" else "吃货小宝" },
                            color = Secondary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = order.createdAt.toFriendlyOrderTime(),
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = if (completed) 0.42f else 0.62f),
                    border = BorderStroke(1.dp, if (completed) OutlineVariant else Secondary.copy(alpha = 0.42f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = message,
                            color = OnSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (completed) Icons.AutoMirrored.Filled.ReceiptLong else Icons.Outlined.Restaurant,
                                contentDescription = null,
                                tint = if (completed) Secondary else Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = dishSummary,
                                color = if (completed) Secondary else Primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CandyCoinIcon(modifier = Modifier.size(18.dp))
                            Text(
                                text = if (cancelled) "糖糖币已返还 ${order.candyCoinsSpent} 枚" else "消耗糖糖币 ${order.candyCoinsSpent} 枚",
                                color = OnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (!cancelled) {
                    val actionText = if (order.buyerRole.isEaterRole()) "去准备" else "呼叫饲养员"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        SquishyOrderActionButton(text = actionText, onClick = onClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SquishyOrderActionButton(
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(modifier = Modifier.scale(if (pressed) 0.96f else 1f)) {
        Surface(
            modifier = Modifier.clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
            shape = RoundedCornerShape(999.dp),
            color = Primary,
            border = BorderStroke(1.dp, Secondary.copy(alpha = 0.58f))
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun OrderAvatar(order: OrderRecord) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Secondary.copy(alpha = 0.58f)),
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (order.buyerAvatarUrl.isNotBlank()) {
                AsyncImage(
                    model = order.buyerAvatarUrl,
                    contentDescription = order.buyerName.ifBlank { "点菜人头像" },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = if (order.status == "completed") Icons.Filled.Favorite else Icons.Outlined.Person,
                    contentDescription = null,
                    tint = if (order.status == "completed") Primary else Tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun String.toOrderBadgeText(role: String): String = when (this) {
    "submitted" -> if (role.isEaterRole()) "呼叫饲养员" else "等你来投喂"
    "confirmed" -> "正在准备"
    "delivering" -> "准备中"
    "completed" -> "已送达胃里"
    "cancelled" -> "已取消"
    else -> "待确认"
}

private fun String.isEaterRole(): Boolean = this == "eater" || this == "foodie"

private fun String.toOrderMessage(buyerName: String): String = when (this) {
    "submitted" -> "\"${buyerName.ifBlank { "对方" }}想吃这些，快去看看吧！\""
    "confirmed", "delivering" -> "\"这顿饭正在安排中，香味马上到。\""
    "completed" -> "\"这顿安排上啦。\""
    "cancelled" -> "\"这单已经取消，下一顿再约。\""
    else -> "\"对方的小愿望已经送到厨房啦。\""
}

private fun String.toFriendlyOrderTime(): String {
    if (isBlank()) return "刚刚下单"
    return take(16).replace("T", " ")
}
