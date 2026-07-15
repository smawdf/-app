package com.myorderapp.ui.candy

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.domain.model.CandyCoinRecord
import com.myorderapp.ui.components.CandyCoinIcon
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.components.CozySurface
import com.myorderapp.ui.components.cozyTextFieldColors
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

private const val COUPLE_HOME_PREFS = "couple_home_prefs"
private const val KEY_SELECTED_ROLE = "selected_role"
private val CandySurface = Color(0xFFFEF8F2)
private val CandyCard = Color(0xFFFFFCF8)
private val CandyLine = Color(0xFFD6C1C5)

@Composable
fun CandyCoinsScreen(
    viewModel: CandyCoinsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val rolePrefs = remember(context) { context.getSharedPreferences(COUPLE_HOME_PREFS, Context.MODE_PRIVATE) }
    val selectedRole = uiState.profile?.selectedRole
        ?.takeIf { it == "caretaker" || it == "eater" }
        ?: rolePrefs.getString(KEY_SELECTED_ROLE, null)
    val isCaretaker = selectedRole == "caretaker"
    var chartMode by remember { mutableStateOf(ChartMode.Bar) }
    var period by remember { mutableStateOf(ChartPeriod.Week) }
    var pendingRecharge by remember { mutableStateOf<Int?>(null) }
    var customAmount by remember { mutableStateOf("") }
    val balance = uiState.walletBalance
    val visibleRecords = remember(uiState.records, isCaretaker) {
        uiState.records.filter { if (isCaretaker) it.type == "recharge" else it.type != "recharge" }
    }
    val chartPoints = remember(visibleRecords, period) { visibleRecords.toChartPoints(period) }

    pendingRecharge?.let { amount ->
        RechargeConfirmDialog(
            amount = amount,
            onDismiss = { pendingRecharge = null },
            onConfirm = {
                pendingRecharge = null
                viewModel.recharge(amount)
            }
        )
    }

    uiState.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            shape = RoundedCornerShape(26.dp),
            containerColor = CandyCard,
            title = { Text("糖糖币提醒", color = CozyCocoa, fontWeight = FontWeight.Black) },
            text = { Text(message, color = CozyMuted) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text("知道了", color = CozyRose) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(CandySurface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CandyTopBar(onBack = onBack)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CandyHeroCard(balance = balance, isCaretaker = isCaretaker)
                }
                if (isCaretaker) {
                    item {
                        RechargePanel(
                            customAmount = customAmount,
                            onCustomAmountChange = { customAmount = it.filter(Char::isDigit).take(4) },
                            onSelectAmount = { pendingRecharge = it },
                            onCustomRecharge = { customAmount.toIntOrNull()?.takeIf { it > 0 }?.let { pendingRecharge = it } }
                        )
                    }
                }
                item {
                    ChartControlCard(
                        chartMode = chartMode,
                        period = period,
                        onChartModeChange = { chartMode = it },
                        onPeriodChange = { period = it },
                        chartPoints = chartPoints,
                        isCaretaker = isCaretaker
                    )
                }
                item {
                    LedgerListCard(records = visibleRecords, isCaretaker = isCaretaker)
                }
            }
        }
    }
}

@Composable
private fun CandyTopBar(onBack: () -> Unit) {
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
            text = "糖糖币管理",
            color = CozyRose,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun CandyHeroCard(balance: Int, isCaretaker: Boolean) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = CandyCard.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, CandyLine.copy(alpha = 0.58f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color(0xFFFFF3F6).copy(alpha = 0.80f), modifier = Modifier.size(62.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    CandyCoinIcon(modifier = Modifier.size(56.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isCaretaker) "吃货糖糖币余额" else "我的糖糖币余额", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
                Text("$balance 枚", color = CozyCocoa, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
                Text(
                    if (isCaretaker) "充值后吃货才能继续快乐点菜" else "点菜会真实消耗糖糖币",
                    color = CozyMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RechargePanel(
    customAmount: String,
    onCustomAmountChange: (String) -> Unit,
    onSelectAmount: (Int) -> Unit,
    onCustomRecharge: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = CandyCard.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, CandyLine.copy(alpha = 0.52f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("给吃货充值", color = CozyCocoa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            listOf(10, 50, 100, 150).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { amount ->
                        RechargeButton(amount = amount, modifier = Modifier.weight(1f), onClick = { onSelectAmount(amount) })
                    }
                }
            }
            OutlinedTextField(
                value = customAmount,
                onValueChange = onCustomAmountChange,
                placeholder = { Text("自定义金额") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = cozyTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onCustomRecharge,
                enabled = customAmount.toIntOrNull()?.let { it > 0 } == true,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CozyRose),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("确认自定义金额", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun RechargeButton(amount: Int, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Surface(
        modifier = modifier.height(58.dp).scale(if (pressed) 0.97f else 1f),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF3F6),
        border = BorderStroke(1.dp, CandyLine)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("+$amount", color = CozyRose, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CandyCoinIcon(modifier = Modifier.size(18.dp))
                Text("糖糖币", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChartControlCard(
    chartMode: ChartMode,
    period: ChartPeriod,
    onChartModeChange: (ChartMode) -> Unit,
    onPeriodChange: (ChartPeriod) -> Unit,
    chartPoints: List<ChartPoint>,
    isCaretaker: Boolean
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = CandyCard.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, CandyLine.copy(alpha = 0.52f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.SsidChart, contentDescription = null, tint = CozyRose, modifier = Modifier.size(22.dp))
                Text(if (isCaretaker) "充值趋势" else "消耗趋势", color = CozyCocoa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            SegmentedRow(
                labels = ChartMode.entries.map { it.label },
                selectedIndex = chartMode.ordinal,
                onSelected = { onChartModeChange(ChartMode.entries[it]) }
            )
            SegmentedRow(
                labels = ChartPeriod.entries.map { it.label },
                selectedIndex = period.ordinal,
                onSelected = { onPeriodChange(ChartPeriod.entries[it]) }
            )
            CandyChart(points = chartPoints, mode = chartMode)
        }
    }
}

@Composable
private fun SegmentedRow(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Surface(
                onClick = { onSelected(index) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) CozyRose else Color.White.copy(alpha = 0.70f),
                border = BorderStroke(1.dp, if (selected) CozyRose else CandyLine),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    label,
                    color = if (selected) Color(0xFFFFFCF8) else CozyCocoa,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun CandyChart(points: List<ChartPoint>, mode: ChartMode) {
    val maxValue = max(points.maxOfOrNull { it.value } ?: 0, 1)
    Canvas(modifier = Modifier.fillMaxWidth().height(168.dp)) {
        val chartHeight = size.height - 32.dp.toPx()
        val bottom = size.height - 18.dp.toPx()
        val slot = size.width / points.size.coerceAtLeast(1)
        points.forEachIndexed { index, point ->
            val x = slot * index + slot / 2f
            val y = bottom - (point.value / maxValue.toFloat()) * chartHeight
            if (mode == ChartMode.Bar) {
                drawRoundRect(
                    color = CozyRose.copy(alpha = 0.72f),
                    topLeft = Offset(x - slot * 0.20f, y),
                    size = androidx.compose.ui.geometry.Size(slot * 0.40f, bottom - y)
                )
            }
            drawCircle(Color(0xFF894C5C), 4.dp.toPx(), Offset(x, y))
        }
        if (mode == ChartMode.Line && points.isNotEmpty()) {
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = slot * index + slot / 2f
                val y = bottom - (point.value / maxValue.toFloat()) * chartHeight
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, CozyRose, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun LedgerListCard(records: List<CandyCoinRecord>, isCaretaker: Boolean) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = CandyCard.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, CandyLine.copy(alpha = 0.52f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isCaretaker) "充值明细" else "消耗明细", color = CozyCocoa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            if (records.isEmpty()) {
                Text("还没有糖糖币记录", color = CozyMuted)
            } else {
                records.take(12).forEach { record ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.note, color = CozyCocoa, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(record.createdAt.toFriendlySecondTime(), color = CozyMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(record.amount.toSignedText(), color = if (record.amount >= 0) CozyRose else Color(0xFF8B4E38), fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun RechargeConfirmDialog(amount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CandyCard,
        title = { Text("确认充值", color = CozyCocoa, fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
        text = { Text("确定给吃货充值 $amount 枚糖糖币吗？", color = CozyMuted, textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = CozyRose)) {
                Text("确认充值", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("再想想", color = CozyMuted) } }
    )
}

private enum class ChartMode(val label: String) { Bar("柱状图"), Line("折线图") }

private enum class ChartPeriod(val label: String) { Week("按周"), Month("按月") }

private data class ChartPoint(val label: String, val value: Int)

private fun List<CandyCoinRecord>.toChartPoints(period: ChartPeriod): List<ChartPoint> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = when (period) {
        ChartPeriod.Week -> (6 downTo 0).map { today.minusDays(it.toLong()) }
        ChartPeriod.Month -> (29 downTo 0).map { today.minusDays(it.toLong()) }
    }
    return days.map { date ->
        val total = filter { it.createdAt.toLocalDateOrNull(zone) == date }.sumOf { kotlin.math.abs(it.amount) }
        ChartPoint("${date.monthValue}/${date.dayOfMonth}", total)
    }
}

private fun String.toLocalDateOrNull(zone: ZoneId): LocalDate? {
    return runCatching { Instant.parse(this).atZone(zone).toLocalDate() }.getOrNull()
}

private fun String.toFriendlySecondTime(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return runCatching { Instant.parse(this).atZone(ZoneId.systemDefault()).format(formatter) }.getOrDefault(this)
}

private fun Int.toSignedText(): String = if (this >= 0) "+$this" else this.toString()
