package com.myorderapp.ui.couple

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.ProfileRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val AnniversaryBg = Color(0xFFF7F9FB)
private val AnniversarySurface = Color(0xFFFBFCFD)
private val AnniversaryInk = Color(0xFF1F2933)
private val AnniversaryMuted = Color(0xFF6B7885)
private val AnniversaryPrimary = Color(0xFF5F95B5)
private val AnniversarySoft = Color(0xFFE2EEF5)
private val AnniversaryBorder = Color(0xFFE7EEF3)

@Composable
fun AnniversaryScreen(
    profileRepository: ProfileRepository = koinInject(),
    onBack: () -> Unit
) {
    val profile by profileRepository.getProfile().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val state = remember(profile) { anniversaryState(profile) }
    var showEditor by remember { mutableStateOf(false) }

    if (showEditor) {
        AnniversaryEditorDialog(
            initialDate = state.startDate,
            onDismiss = { showEditor = false },
            onSave = { name, date, countMode ->
                scope.launch {
                    val current = profile ?: Profile()
                    profileRepository.saveProfile(
                        current.copy(
                            nickname = current.nickname,
                            pairedAt = date.toString()
                        )
                    )
                    showEditor = false
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnniversaryBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            AnniversaryHeader(onBack = onBack)
            HeroHeart(state = state, onAddClick = { showEditor = true })
            NextAnniversaryCard(state = state)
            EmptyAnniversaryHint()
        }

        FloatingActionButton(
            onClick = { showEditor = true },
            shape = CircleShape,
            containerColor = AnniversaryPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 34.dp)
                .size(64.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "设置纪念日", modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun AnniversaryHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 12.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = AnniversaryInk)
        }
        Text(
            text = "纪念日",
            color = AnniversaryInk,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun HeroHeart(state: AnniversaryPageUiState, onAddClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        shape = RoundedCornerShape(30.dp),
        color = AnniversarySoft,
        border = BorderStroke(1.dp, AnniversaryBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(AnniversarySoft, Color(0xFFF7FBFF))
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier.size(176.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Favorite, contentDescription = null, tint = AnniversaryPrimary.copy(alpha = 0.22f), modifier = Modifier.size(128.dp))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, AnniversaryBorder), modifier = Modifier.size(62.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Cake, contentDescription = null, tint = AnniversaryPrimary, modifier = Modifier.size(30.dp))
                        }
                    }
                    Surface(shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, AnniversaryBorder), modifier = Modifier.size(62.dp), onClick = onAddClick) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, contentDescription = "设置纪念日", tint = AnniversaryPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text("我们在一起已经", color = AnniversaryMuted, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.days.toString(),
                        color = AnniversaryInk,
                        fontSize = 62.sp,
                        lineHeight = 66.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text("天", color = AnniversaryMuted, fontSize = 20.sp, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
                }
                Text(state.startDateTextWithWeek, color = AnniversaryMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NextAnniversaryCard(state: AnniversaryPageUiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        shape = RoundedCornerShape(22.dp),
        color = AnniversarySurface,
        border = BorderStroke(1.dp, AnniversaryBorder)
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("下一个纪念日", color = AnniversaryInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(state.nextAnniversaryText, color = AnniversaryMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Surface(shape = RoundedCornerShape(18.dp), color = AnniversarySoft, modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = AnniversaryPrimary, modifier = Modifier.size(42.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyAnniversaryHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(shape = RoundedCornerShape(26.dp), color = AnniversarySoft, modifier = Modifier.size(112.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = AnniversaryPrimary, modifier = Modifier.size(54.dp))
            }
        }
        Text("暂无更多纪念日，创建一个试试吧~", color = AnniversaryMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AnniversaryEditorDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String) -> Unit
) {
    var name by remember { mutableStateOf("在一起的日子") }
    var dateText by remember(initialDate) { mutableStateOf(initialDate.toString()) }
    var countMode by remember { mutableStateOf("累计天数") }
    val parsedDate = remember(dateText) { runCatching { LocalDate.parse(dateText.trim()) }.getOrNull() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = AnniversarySurface,
        title = { Text("创建纪念日", color = AnniversaryInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("每一个重要时刻都值得被铭记", color = AnniversaryMuted, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("纪念日名称") },
                    supportingText = { Text("${name.length}/20") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = anniversaryFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it.take(10) },
                    label = { Text("日期，例如 2026-05-20") },
                    singleLine = true,
                    isError = parsedDate == null,
                    shape = RoundedCornerShape(14.dp),
                    colors = anniversaryFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("累计天数", "每年倒数").forEach { mode ->
                        val selected = countMode == mode
                        Surface(
                            onClick = { countMode = mode },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) AnniversaryPrimary else AnniversarySoft
                        ) {
                            Text(
                                mode,
                                color = if (selected) Color.White else AnniversaryInk,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && parsedDate != null,
                onClick = { parsedDate?.let { onSave(name, it, countMode) } },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnniversaryPrimary, contentColor = Color.White)
            ) {
                Text("保存纪念日")
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
    focusedContainerColor = AnniversarySurface,
    unfocusedContainerColor = AnniversarySurface
)

private data class AnniversaryPageUiState(
    val startDate: LocalDate,
    val days: Long,
    val startDateTextWithWeek: String,
    val nextAnniversaryText: String
)

private fun anniversaryState(profile: Profile?): AnniversaryPageUiState {
    val startDate = resolveAnniversaryStartDate(profile)
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(startDate, today).coerceAtLeast(0)
    val next = nextYearlyDate(startDate, today)
    val remaining = ChronoUnit.DAYS.between(today, next).coerceAtLeast(0)
    return AnniversaryPageUiState(
        startDate = startDate,
        days = days,
        startDateTextWithWeek = "${startDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.CHINA))} ${weekText(startDate)}",
        nextAnniversaryText = if (remaining == 0L) "今天就是纪念日" else "${next.format(DateTimeFormatter.ofPattern("MM月dd日", Locale.CHINA))} · 还有 $remaining 天"
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
    return source?.let(::parseDateSafely) ?: LocalDate.of(2026, 5, 20)
}

private fun parseDateSafely(value: String): LocalDate? {
    return runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }
        .getOrNull()
        ?: runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

private fun weekText(date: LocalDate): String {
    val week = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    return week[date.dayOfWeek.value - 1]
}
