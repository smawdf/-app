package com.myorderapp.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.ui.components.OrderCard
import com.myorderapp.ui.components.OrderGuidanceEmptyState
import com.myorderapp.ui.util.yuanText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.koin.androidx.compose.koinViewModel

private val PrimaryBlue = Color(0xFF5F95B5)
private val SoftBackground = Color(0xFFF7F9FB)
private val Ink = Color(0xFF1F2933)
private val Muted = Color(0xFF6B7885)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val Border = Color(0xFFE7EEF3)

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = koinViewModel(),
    onOrderClick: (String) -> Unit = {},
    onGoOrderingClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var diarySelected by remember { mutableStateOf(false) }
    var showCalendarFilter by remember { mutableStateOf(false) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val kitchenOrders = remember(uiState.orders, selectedDate) {
        uiState.orders.filter { order ->
            order.status != "completed" && order.createdAt.toLocalDateOrNull() == selectedDate
        }
    }
    val diaryOrders = remember(uiState.orders, selectedDate) {
        uiState.orders.filter { order ->
            order.status == "completed" && order.createdAt.toLocalDateOrNull() == selectedDate
        }
    }
    val visibleOrders = if (diarySelected) diaryOrders else kitchenOrders

    val kitchenOrderCount = kitchenOrders.size
    val diaryOrderCount = diaryOrders.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { OrdersHeader(title = "订单") }

        item {
            OrdersSummaryTabs(
                orderCount = kitchenOrderCount,
                diaryCount = diaryOrderCount,
                diarySelected = diarySelected,
                selectedDate = selectedDate,
                onKitchenClick = { diarySelected = false },
                onDiaryClick = { diarySelected = true },
                onCalendarClick = { showCalendarFilter = !showCalendarFilter }
            )
        }

        if (showCalendarFilter) {
            item {
                CalendarFilterCard(
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                    onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                    onDateSelected = { date ->
                        selectedDate = date
                        displayedMonth = YearMonth.from(date)
                        showCalendarFilter = false
                    },
                    onReset = {
                        selectedDate = LocalDate.now()
                        displayedMonth = YearMonth.now()
                        showCalendarFilter = false
                    }
                )
            }
        }

        if (diarySelected && visibleOrders.isEmpty()) {
            item {
                EmptyOrdersState(
                    text = "暂无日记哦~",
                    onGoOrderingClick = onGoOrderingClick
                )
            }
        } else if (visibleOrders.isEmpty()) {
            item {
                EmptyOrdersState(
                    text = "暂无订单哦~",
                    onGoOrderingClick = onGoOrderingClick
                )
            }
        } else {
            items(visibleOrders, key = { it.id }) { order ->
                if (diarySelected) {
                    FoodDiaryRecordCard(order = order, onClick = { onOrderClick(order.id) })
                } else {
                    OrderRecordCard(order = order, onClick = { onOrderClick(order.id) })
                }
            }
        }
    }
}

@Composable
private fun OrdersHeader(title: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            color = Ink,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OrdersSummaryTabs(
    orderCount: Int,
    diaryCount: Int,
    diarySelected: Boolean,
    selectedDate: LocalDate,
    onKitchenClick: () -> Unit,
    onDiaryClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (diarySelected) "一共记录了${diaryCount}个日记" else "一共记录了${orderCount}个订单",
                    color = Ink,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = selectedDate.toChineseDateText(),
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            Surface(shape = RoundedCornerShape(999.dp), color = SurfaceWhite) {
                Row(modifier = Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    SegmentChip(text = "厨房订单", selected = !diarySelected, onClick = onKitchenClick)
                    SegmentChip(text = "美食日记", selected = diarySelected, onClick = onDiaryClick)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.EventNote,
                contentDescription = "日期筛选",
                tint = PrimaryBlue,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clickable(onClick = onCalendarClick)
                    .size(34.dp)
            )
        }
    }
}

@Composable
private fun SegmentChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) PrimaryBlue else SurfaceWhite
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else Ink,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CalendarFilterCard(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onPreviousMonth)
                Text(
                    text = displayedMonth.toChineseMonthText(),
                    color = Ink,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold
                )
                MonthButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = onNextMonth)
            }

            CalendarGrid(
                displayedMonth = displayedMonth,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "当前筛选：${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.12f),
                        contentColor = PrimaryBlue
                    )
                ) {
                    Text("回到今天", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = displayedMonth.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.weekIndexFromSunday()
    val days = (1..displayedMonth.lengthOfMonth()).map { displayedMonth.atDay(it) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    color = Muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        (0 until 6).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0 until 7).forEach { dayOfWeek ->
                    val index = week * 7 + dayOfWeek
                    val date = days.getOrNull(index - leadingBlanks)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val selected = date == selectedDate
                            Surface(
                                shape = CircleShape,
                                color = if (selected) PrimaryBlue else Color.Transparent,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { onDateSelected(date) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        color = if (selected) Color.White else Ink,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color(0xFFE2EEF5), modifier = Modifier.size(34.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun EmptyOrdersState(text: String, onGoOrderingClick: () -> Unit) {
    OrderGuidanceEmptyState(
        title = text,
        subtitle = "点菜后，这里会显示你的厨房订单和历史记录",
        actionText = "去点菜",
        onAction = onGoOrderingClick
    )
}

@Composable
private fun OrderRecordCard(order: OrderRecord, onClick: () -> Unit) {
    OrderCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFE2EEF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = PrimaryBlue)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(order.shopName, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(order.status.toOrderStatusText(), color = PrimaryBlue, fontSize = 14.sp)
                Text(order.createdAt, color = Muted, fontSize = 12.sp)
            }
            Text(yuanText(order.totalPrice), color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FoodDiaryRecordCard(order: OrderRecord, onClick: () -> Unit) {
    val dishSummary = order.items
        .take(3)
        .joinToString("、") { "${it.menuItemName} ×${it.quantity}" }
        .ifBlank { order.buyerNote.ifBlank { "这顿饭已经完成啦" } }

    OrderCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = Color(0xFFFFEEF2), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color(0xFFE46C83))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("这顿饭已记录", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(dishSummary, color = Muted, fontSize = 13.sp)
                Text(order.createdAt, color = Muted, fontSize = 12.sp)
            }
            Text(yuanText(order.totalPrice), color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(take(10)) }.getOrNull()
}

private fun DayOfWeek.weekIndexFromSunday(): Int = when (this) {
    DayOfWeek.SUNDAY -> 0
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
}

private fun LocalDate.toChineseDateText(): String {
    return "${year}年${monthValue.toString().padStart(2, '0')}月${dayOfMonth.toString().padStart(2, '0')}日"
}

private fun YearMonth.toChineseMonthText(): String {
    return "${year}年${monthValue}月"
}

private fun String.toOrderStatusText(): String = when (this) {
    "submitted" -> "已提交"
    "confirmed" -> "已接单"
    "delivering" -> "准备中"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    else -> this
}
