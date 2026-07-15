package com.myorderapp.ui.couple

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack 
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft 
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val AnniversarySurface = Color(0xFFFEF8F2)
private val AnniversaryCard = Color(0xFFFFFCF8)
private val AnniversaryInk = Color(0xFF1D1B18)
private val AnniversaryMuted = Color(0xFF524346)
private val AnniversaryPrimary = Color(0xFF894C5C)
private val AnniversarySecondary = Color(0xFF78555E)
private val AnniversaryPink = Color(0xFFFFD1DC)
private val AnniversarySoftPink = Color(0xFFF4A7B9)
private val AnniversaryTerracotta = Color(0xFF8B4E38)
private val AnniversaryBorder = Color(0xFFD6C1C5)
private val AnniversaryCreamLine = Color(0xFFF0E5DC)
private val completedMomentStatuses = setOf("completed", "delivered", "finished")

@Composable
fun AnniversaryScreen(
    profileRepository: ProfileRepository = koinInject(),
    orderRepository: OrderRepository = koinInject(),
    onBack: () -> Unit
) {
    val profile by profileRepository.getProfile().collectAsStateWithLifecycle(initialValue = null)
    val orders by orderRepository.observeOrders().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val state = remember(profile) { anniversaryState(profile) }
    val sweetMomentOrders = remember(orders) {
        orders
            .filter { it.status in completedMomentStatuses }
            .sortedByDescending { it.createdAt }
            .take(5)
    }
    var showEditor by remember { mutableStateOf(false) }

    if (showEditor) {
        AnniversaryEditorDialog(
            initialDate = state.startDate,
            onDismiss = { showEditor = false },
            onSave = { date ->
                scope.launch {
                    val current = profile ?: Profile()
                    profileRepository.saveProfile(current.copy(pairedAt = date.toString()))
                    showEditor = false
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnniversarySurface)
    ) {
        AnniversaryBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            AnniversaryHeader(onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                AnniversaryHeroCard(state = state)
                NextAnniversaryCard(state = state, onClick = { showEditor = true })
                SweetMomentsTimeline(orders = sweetMomentOrders)
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun AnniversaryBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color(0xFFFFB1C3).copy(alpha = 0.10f),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.50f, -size.width * 0.15f)
        )
        drawNoodleBowl(
            topLeft = Offset(size.width * 0.42f, size.height * 0.82f),
            width = size.width * 0.50f,
            color = AnniversaryPrimary.copy(alpha = 0.08f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNoodleBowl(
    topLeft: Offset,
    width: Float,
    color: Color
) {
    val height = width * 0.52f
    val path = Path().apply {
        moveTo(topLeft.x + width * 0.18f, topLeft.y + height * 0.34f)
        cubicTo(topLeft.x + width * 0.20f, topLeft.y + height, topLeft.x + width * 0.80f, topLeft.y + height, topLeft.x + width * 0.82f, topLeft.y + height * 0.34f)
        close()
    }
    drawPath(path, color)
    drawCircle(color, width * 0.06f, Offset(topLeft.x + width * 0.28f, topLeft.y + height * 0.16f))
    drawCircle(color, width * 0.035f, Offset(topLeft.x + width * 0.70f, topLeft.y + height * 0.12f))
}

@Composable
private fun AnniversaryHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .background(AnniversarySurface.copy(alpha = 0.94f))
            .padding(horizontal = 22.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = AnniversaryMuted, modifier = Modifier.size(24.dp)) 
        }
        Text(
            text = "纪念日",
            color = AnniversaryPrimary,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(AnniversaryCreamLine.copy(alpha = 0.92f))
        )
    }
}

@Composable
private fun AnniversaryHeroCard(state: AnniversaryPageUiState) {
    StitchGlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color(0xFFFFEFF3).copy(alpha = 0.46f), 
        borderColor = AnniversaryTerracotta.copy(alpha = 0.32f),
        squishyShadow = true,
        radius = 18,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(126.dp), contentAlignment = Alignment.Center) {
            FloatingHeart(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 6.dp),
                size = 34.dp,
                color = AnniversaryPrimary.copy(alpha = 0.54f),
                outline = true
            )
            FloatingHeart(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 54.dp, top = 28.dp),
                size = 44.dp,
                color = AnniversaryPink.copy(alpha = 0.38f), 
                outline = true
            )
            FloatingHeart(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 18.dp),
                size = 20.dp,
                color = AnniversaryTerracotta.copy(alpha = 0.64f),
                outline = true
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "恋爱第 ${state.days} 天",
                    color = AnniversaryPrimary,
                    fontSize = 21.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "每一次心跳，都在为你倒数",
                    color = AnniversarySecondary,
                    fontSize = 19.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FloatingHeart(
    modifier: Modifier,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    outline: Boolean = false
) {
    if (outline) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.50f, h * 0.86f)
                cubicTo(w * 0.06f, h * 0.52f, w * 0.06f, h * 0.16f, w * 0.30f, h * 0.16f)
                cubicTo(w * 0.42f, h * 0.16f, w * 0.49f, h * 0.25f, w * 0.50f, h * 0.34f)
                cubicTo(w * 0.51f, h * 0.25f, w * 0.58f, h * 0.16f, w * 0.70f, h * 0.16f)
                cubicTo(w * 0.94f, h * 0.16f, w * 0.94f, h * 0.52f, w * 0.50f, h * 0.86f)
            }
            drawPath(path, color, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
        }
    } else {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = color,
            modifier = modifier.size(size)
        )
    }
}

@Composable
private fun NextAnniversaryCard(
    state: AnniversaryPageUiState,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFE7EC).copy(alpha = 0.54f), 
        border = BorderStroke(1.5.dp, AnniversarySoftPink.copy(alpha = 0.34f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 114.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(shape = CircleShape, color = AnniversaryPrimary.copy(alpha = 0.10f), modifier = Modifier.size(54.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Cake, contentDescription = null, tint = AnniversaryPrimary, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.nextAnniversaryTitle, color = AnniversaryMuted, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                Text("距离下一站浪漫", color = AnniversaryInk, fontSize = 18.sp, lineHeight = 25.sp)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.nextAnniversaryRemainingDays.toString(),
                    color = AnniversaryPrimary,
                    fontSize = 25.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black
                )
                Text("天", color = AnniversaryMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun CalendarCard(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    calendarType: AnniversaryCalendarType,
    onCalendarTypeChange: (AnniversaryCalendarType) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    visibleWeekCount: Int? = null,
    onDateSelected: (LocalDate) -> Unit
) {
    StitchGlassCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = AnniversaryCard.copy(alpha = 0.58f), 
        borderColor = AnniversaryBorder.copy(alpha = 0.48f),
        squishyShadow = true,
        radius = 18,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(25.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = monthName(visibleMonth.monthValue),
                    color = AnniversaryInk,
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Normal
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CalendarTypeToggle(calendarType = calendarType, onCalendarTypeChange = onCalendarTypeChange)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上月", tint = AnniversarySecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onNextMonth, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下月", tint = AnniversarySecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            CalendarGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                calendarType = calendarType,
                visibleWeekCount = visibleWeekCount,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun CalendarTypeToggle(
    calendarType: AnniversaryCalendarType,
    onCalendarTypeChange: (AnniversaryCalendarType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFE7E2DC).copy(alpha = 0.50f),
        border = BorderStroke(1.dp, AnniversaryBorder.copy(alpha = 0.46f))
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            AnniversaryCalendarType.entries.forEach { type ->
                val selected = calendarType == type
                Surface(
                    onClick = { onCalendarTypeChange(type) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) AnniversaryPrimary else Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = type.label,
                        color = if (selected) Color.White else AnniversaryMuted,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    calendarType: AnniversaryCalendarType,
    visibleWeekCount: Int? = null,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    color = AnniversaryMuted,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val cells = remember(visibleMonth, visibleWeekCount) {
            calendarCellsFor(visibleMonth).let { allCells ->
                visibleWeekCount?.let { allCells.take(it * 7) } ?: allCells
            }
        }
        cells.chunked(7).forEach { rowCells ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowCells.forEach { cell ->
                    CalendarDayCell(
                        cell = cell,
                        selectedDate = selectedDate,
                        calendarType = calendarType,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarCell,
    selectedDate: LocalDate,
    calendarType: AnniversaryCalendarType,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentMonth = YearMonth.from(cell.date) == cell.visibleMonth
    val isSelected = isCurrentMonth && cell.date == selectedDate
    val hasDot = false
    val hasHeart = false
    Surface(
        onClick = { if (isCurrentMonth) onDateSelected(cell.date) },
        shape = CircleShape,
        color = if (isSelected) AnniversaryPrimary else Color.Transparent,
        modifier = modifier
            .height(42.dp)
            .aspectRatio(1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                color = when {
                    isSelected -> Color.White
                    isCurrentMonth -> AnniversaryInk
                    else -> AnniversaryInk.copy(alpha = 0.13f)
                },
                fontSize = 19.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (calendarType == AnniversaryCalendarType.Lunar && isCurrentMonth && !isSelected) {
                Text(
                    text = solarToLunar(cell.date).shortLabel(),
                    color = AnniversaryMuted.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            if (hasDot && !isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(6.dp)
                        .background(AnniversaryPrimary, CircleShape)
                )
            }
            if (hasHeart && !isSelected) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (cell.date.dayOfMonth == 4) AnniversaryTerracotta else AnniversaryPrimary,
                    modifier = Modifier.align(Alignment.BottomCenter).size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SweetMomentsTimeline(orders: List<OrderRecord>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.HistoryEdu, contentDescription = null, tint = AnniversaryPrimary, modifier = Modifier.size(28.dp))
            Text("甜蜜时刻", color = AnniversaryInk, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.Normal)
        }
        if (orders.isEmpty()) {
            StitchGlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = AnniversaryCard.copy(alpha = 0.72f), 
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 22.dp),
                squishyShadow = true,
                borderColor = AnniversaryBorder.copy(alpha = 0.42f),
                radius = 16
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("还没有记录", color = AnniversaryInk, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
                    Text("完成点菜后，可以在这里沉淀你们自己的甜蜜时刻。", color = AnniversaryMuted, fontSize = 16.sp, lineHeight = 24.sp)
                }
            }
        } else {
            orders.forEachIndexed { index, order ->
                SweetMomentItem(
                    dotColor = if (index == 0) AnniversarySoftPink else AnniversaryPink,
                    dotBorder = AnniversaryPrimary.copy(alpha = 0.46f),
                    date = order.createdAt.toMomentDateText(),
                    text = order.toSweetMomentText(),
                    imageUrl = order.items.firstOrNull { it.menuItemImageUrl.isNotBlank() }?.menuItemImageUrl,
                    icon = Icons.Filled.Cake
                )
            }
        }
    }
}

private fun OrderRecord.toSweetMomentText(): String {
    val dishNames = items.take(2).joinToString("、") { it.menuItemName }.ifBlank { "一顿小饭" }
    val suffix = if (items.size > 2) "等 ${items.size} 道菜" else ""
    val buyer = buyerName.ifBlank { if (buyerRole == "caretaker" || buyerRole == "keeper") "饲养员" else "吃货" }
    return "$buyer 点了 $dishNames$suffix，一起完成了一次开饭记录。"
}

private fun String.toMomentDateText(): String {
    return parseDateSafely(this)?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: take(10).ifBlank { "最近一次" }
}

@Composable
private fun SweetMomentItem(
    dotColor: Color,
    dotBorder: Color,
    date: String,
    text: String,
    imageUrl: String? = null,
    icon: ImageVector? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = dotColor,
            border = BorderStroke(2.dp, dotBorder),
            modifier = Modifier.padding(top = 15.dp, start = 3.dp).size(18.dp)
        ) {}
        Spacer(modifier = Modifier.width(20.dp))
        StitchGlassCard(
            modifier = Modifier.weight(1f),
            containerColor = AnniversaryCard.copy(alpha = 0.86f), 
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 22.dp),
            squishyShadow = true,
            borderColor = AnniversaryBorder.copy(alpha = 0.42f),
            radius = 16
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AnniversaryPink,
                    modifier = Modifier.size(82.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            imageUrl != null -> AsyncImage(
                                model = imageUrl,
                                contentDescription = date,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            icon != null -> Icon(icon, contentDescription = null, tint = AnniversaryPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(date, color = AnniversaryInk, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
                    Text(text, color = AnniversaryInk, fontSize = 17.sp, lineHeight = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun StitchGlassCard(
    modifier: Modifier = Modifier,
    containerColor: Color = AnniversaryCard.copy(alpha = 0.70f),
    borderColor: Color = AnniversaryTerracotta,
    squishyShadow: Boolean = true,
    radius: Int = 16,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(radius.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor.copy(alpha = if (squishyShadow) 0.72f else 0.58f)),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AnniversaryEditorDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LocalDate) -> Unit
) {
    var dateText by remember(initialDate) { mutableStateOf(initialDate.toString()) }
    var calendarType by remember { mutableStateOf(AnniversaryCalendarType.Solar) }
    var visibleMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }
    val parsedDate = remember(dateText, calendarType) { parseAnniversaryInput(dateText, calendarType) }

    LaunchedEffect(parsedDate) {
        parsedDate?.let { visibleMonth = YearMonth.from(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = AnniversaryCard,
        title = { Text("设置纪念日", color = AnniversaryInk, fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("支持文字输入，也可以在日历里选择日期。", color = AnniversaryMuted)
                CalendarTypeToggle(calendarType = calendarType, onCalendarTypeChange = { type ->
                    calendarType = type
                    dateText = if (type == AnniversaryCalendarType.Solar) {
                        (parsedDate ?: initialDate).toString()
                    } else {
                        solarToLunar(parsedDate ?: initialDate).toInputText()
                    }
                })
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it.take(11) },
                    label = { Text(calendarType.inputLabel) },
                    singleLine = true,
                    isError = parsedDate == null,
                    supportingText = {
                        Text(
                            when {
                                parsedDate == null -> "请输入有效日期，例如 2026-05-20；农历闰月可写 闰04"
                                calendarType == AnniversaryCalendarType.Lunar -> "将按阳历 ${parsedDate} 保存"
                                else -> "也可以直接在下方日历点击选择"
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = anniversaryFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                CalendarCard(
                    visibleMonth = visibleMonth,
                    selectedDate = parsedDate ?: initialDate,
                    calendarType = calendarType,
                    onCalendarTypeChange = { type ->
                        calendarType = type
                        dateText = if (type == AnniversaryCalendarType.Solar) {
                            (parsedDate ?: initialDate).toString()
                        } else {
                            solarToLunar(parsedDate ?: initialDate).toInputText()
                        }
                    },
                    onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                    onDateSelected = { selected ->
                        dateText = if (calendarType == AnniversaryCalendarType.Solar) {
                            selected.toString()
                        } else {
                            solarToLunar(selected).toInputText()
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = parsedDate != null,
                onClick = { parsedDate?.let(onSave) },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnniversaryPrimary, contentColor = Color.White)
            ) {
                Text("保存纪念日", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = AnniversaryMuted) }
        }
    )
}

@Composable
private fun anniversaryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AnniversaryPrimary,
    unfocusedBorderColor = AnniversaryBorder,
    focusedContainerColor = AnniversaryCard,
    unfocusedContainerColor = AnniversaryCard
)

private data class CalendarCell(
    val date: LocalDate,
    val visibleMonth: YearMonth
)

private fun calendarCellsFor(visibleMonth: YearMonth): List<CalendarCell> {
    val firstDay = visibleMonth.atDay(1)
    val leadingDays = firstDay.dayOfWeek.value % 7
    val start = firstDay.minusDays(leadingDays.toLong())
    return List(35) { index -> CalendarCell(date = start.plusDays(index.toLong()), visibleMonth = visibleMonth) }
}

private fun LocalDate.isSupportedInVisibleMonth(visibleMonth: YearMonth): Boolean {
    return YearMonth.from(this) == visibleMonth
}

private fun monthName(month: Int): String = when (month) {
    1 -> "一月"
    2 -> "二月"
    3 -> "三月"
    4 -> "四月"
    5 -> "五月"
    6 -> "六月"
    7 -> "七月"
    8 -> "八月"
    9 -> "九月"
    10 -> "十月"
    11 -> "十一月"
    12 -> "十二月"
    else -> "${month}月"
}

private enum class AnniversaryCalendarType(val label: String, val inputLabel: String) {
    Solar("阳历", "阳历日期，例如 2026-05-20"),
    Lunar("农历", "农历日期，例如 2026-04-04")
}

private data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false
) {
    fun toInputText(): String = if (isLeapMonth) {
        listOf(year.toString().padStart(4, '0'), "闰" + month.toString().padStart(2, '0'), day.toString().padStart(2, '0')).joinToString("-")
    } else {
        listOf(year.toString().padStart(4, '0'), month.toString().padStart(2, '0'), day.toString().padStart(2, '0')).joinToString("-")
    }

    fun shortLabel(): String = if (day == 1) "${if (isLeapMonth) "闰" else ""}${month}月" else dayLabel()

    fun dayLabel(): String {
        val labels = listOf(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )
        return labels.getOrElse(day - 1) { day.toString() }
    }
}

private fun parseAnniversaryInput(text: String, calendarType: AnniversaryCalendarType): LocalDate? {
    val normalized = text.trim()
    return when (calendarType) {
        AnniversaryCalendarType.Solar -> runCatching { LocalDate.parse(normalized) }.getOrNull()
        AnniversaryCalendarType.Lunar -> parseLunarDate(normalized)?.let(::lunarToSolar)
    }
}

private fun parseLunarDate(text: String): LunarDate? {
    val parts = text.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val monthText = parts[1].trim()
    val isLeapMonth = monthText.startsWith("L", ignoreCase = true) || monthText.startsWith("闰")
    val normalizedMonth = monthText.removePrefix("闰").removePrefix("L").removePrefix("l")
    val month = normalizedMonth.toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (year !in 1901..2099 || month !in 1..12 || day !in 1..30) return null
    return LunarDate(year = year, month = month, day = day, isLeapMonth = isLeapMonth)
}

private fun solarToLunar(date: LocalDate): LunarDate {
    val gregorianCalendar = android.icu.util.GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
    val chineseCalendar = android.icu.util.ChineseCalendar().apply {
        timeInMillis = gregorianCalendar.timeInMillis
    }
    return LunarDate(
        year = chineseCalendar.get(android.icu.util.Calendar.EXTENDED_YEAR) - CHINESE_EXTENDED_YEAR_OFFSET,
        month = chineseCalendar.get(android.icu.util.Calendar.MONTH) + 1,
        day = chineseCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH),
        isLeapMonth = chineseCalendar.get(android.icu.util.ChineseCalendar.IS_LEAP_MONTH) == 1
    )
}

private fun lunarToSolar(lunarDate: LunarDate): LocalDate? {
    val searchStart = LocalDate.of(lunarDate.year, 1, 1).minusDays(45)
    val searchEnd = LocalDate.of(lunarDate.year, 12, 31).plusDays(45)
    var date = searchStart
    while (!date.isAfter(searchEnd)) {
        if (solarToLunar(date) == lunarDate) return date
        date = date.plusDays(1)
    }
    return null
}

private const val CHINESE_EXTENDED_YEAR_OFFSET = 2637

private data class AnniversaryPageUiState(
    val startDate: LocalDate,
    val days: Long,
    val nextAnniversaryTitle: String,
    val nextAnniversaryRemainingDays: Long
)

private fun anniversaryState(profile: Profile?): AnniversaryPageUiState {
    val startDate = resolveAnniversaryStartDate(profile)
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(startDate, today).coerceAtLeast(0)
    val next = nextYearlyDate(startDate, today)
    val remaining = ChronoUnit.DAYS.between(today, next).coerceAtLeast(0)
    val years = (next.year - startDate.year).coerceAtLeast(1)
    return AnniversaryPageUiState(
        startDate = startDate,
        days = days,
        nextAnniversaryTitle = "${years}周年纪念日",
        nextAnniversaryRemainingDays = remaining
    )
}

private fun nextYearlyDate(startDate: LocalDate, today: LocalDate): LocalDate {
    var next = startDate.withYear(today.year)
    if (next.isBefore(today)) next = next.plusYears(1)
    return next
}

private fun resolveAnniversaryStartDate(profile: Profile?): LocalDate {
    val source = listOfNotNull(
        profile?.pairedAt?.takeIf { it.isNotBlank() },
        profile?.createdAt?.takeIf { it.isNotBlank() }
    ).firstOrNull()
    return source?.let(::parseDateSafely) ?: LocalDate.now()
}

private fun parseDateSafely(value: String): LocalDate? {
    return runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}
