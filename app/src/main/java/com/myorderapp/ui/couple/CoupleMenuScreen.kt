package com.myorderapp.ui.couple

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.ui.notifications.notifyActiveOrderIfAllowed
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val ToolBackground = Color(0xFFFFF6E8)
private val ToolSurface = Color(0xFFFFFBF5)
private val ToolSurfaceAlt = Color(0xFFFFE9C9)
private val ToolBorder = Color(0xFFF2DAC7)
private val ToolInk = Color(0xFF2B2019)
private val ToolMuted = Color(0xFF8C6650)
private val ToolAccent = Color(0xFF119482)
private val ToolAccentSoft = Color(0xFFE0F6EF)
private val ToolRose = Color(0xFFFF7088)
private val ToolGold = Color(0xFFFFCF6F)
private val ToolPeach = Color(0xFFFFB6AC)

private enum class CoupleRole {
    Caretaker,
    Eater;

    val storageKey: String
        get() = when (this) {
            Caretaker -> "caretaker"
            Eater -> "eater"
        }
}

private data class RoleToastState(
    val id: Int,
    val role: CoupleRole
)

private const val COUPLE_HOME_PREFS = "couple_home_prefs"
private const val KEY_SELECTED_ROLE = "selected_role"
private const val PROFILE_PREFS = "profile_screen_prefs"
private const val KEY_ORDER_NOTIFICATIONS_ENABLED = "order_notifications_enabled"
private const val KEY_LAST_NOTIFIED_ORDER_ID = "last_notified_order_id"

private fun String?.toCoupleRole(): CoupleRole? = when (this) {
    CoupleRole.Caretaker.storageKey -> CoupleRole.Caretaker
    CoupleRole.Eater.storageKey -> CoupleRole.Eater
    else -> null
}

@Composable
fun CoupleMenuScreen(
    profileRepository: ProfileRepository = koinInject(),
    orderRepository: OrderRepository = koinInject(),
    onCustomizeMenuClick: () -> Unit = {},
    onGoOrderingClick: () -> Unit = {},
    onAnniversaryClick: () -> Unit = {},
    onDiscoverClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val profile by profileRepository.getProfile().collectAsState(initial = null)
    val orders by orderRepository.observeOrders().collectAsState(initial = emptyList())
    var pairInfo by remember { mutableStateOf(PairInfo()) }
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(COUPLE_HOME_PREFS, Context.MODE_PRIVATE)
    }
    val profilePrefs = remember(context) {
        context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
    }
    var selectedRole by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_SELECTED_ROLE, null).toCoupleRole())
    }
    var toastState by remember { mutableStateOf<RoleToastState?>(null) }
    var toastId by remember { mutableIntStateOf(0) }

    fun selectRole(role: CoupleRole) {
        selectedRole = role
        prefs.edit().putString(KEY_SELECTED_ROLE, role.storageKey).apply()
        toastId += 1
        toastState = RoleToastState(id = toastId, role = role)
    }

    LaunchedEffect(profile?.pairId) {
        pairInfo = profileRepository.getPairInfo()
    }

    LaunchedEffect(profile?.userId, profile?.pairId) {
        while (true) {
            profileRepository.touchPresence()
            pairInfo = profileRepository.getPairInfo()
            delay(60_000)
        }
    }

    val activeOrder = orders.firstOrNull { it.status in activeOrderStatuses }
    LaunchedEffect(activeOrder?.id, activeOrder?.status, selectedRole) {
        val order = activeOrder ?: return@LaunchedEffect
        val notificationsEnabled = profilePrefs.getBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, false)
        val notifiedKey = "${order.id}:${order.status}"
        val lastNotifiedKey = profilePrefs.getString(KEY_LAST_NOTIFIED_ORDER_ID, "")
        if (notificationsEnabled && lastNotifiedKey != notifiedKey) {
            notifyActiveOrderIfAllowed(
                context = context,
                order = order,
                isCaretaker = selectedRole == CoupleRole.Caretaker
            )
            profilePrefs.edit().putString(KEY_LAST_NOTIFIED_ORDER_ID, notifiedKey).apply()
        }
    }

    LaunchedEffect(toastState?.id) {
        if (toastState != null) {
            delay(1500)
            toastState = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ToolBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 92.dp)
        ) {
            HomeHeader()
            WorkbenchSummary(
                days = daysEatingTogether(profile),
                selectedRole = selectedRole,
                profile = profile,
                pairInfo = pairInfo,
                onPartnerClick = onProfileClick
            )
            PrimaryActionGroup(
                selectedRole = selectedRole,
                onCaretakerClick = { selectRole(CoupleRole.Caretaker) },
                onEaterClick = { selectRole(CoupleRole.Eater) }
            )
            RoleFunctionCard(
                selectedRole = selectedRole,
                onCustomizeMenuClick = onCustomizeMenuClick,
                onGoOrderingClick = onGoOrderingClick
            )
            LatestOrderNudge(
                order = activeOrder,
                selectedRole = selectedRole,
                onClick = onOrdersClick
            )
            AnniversaryCard(days = daysEatingTogether(profile), onClick = onAnniversaryClick)
        }

        AnimatedVisibility(
            visible = toastState != null,
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.Center),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = toastState != null,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 5 },
                    exit = fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 6 }
                ) {
                    toastState?.let { IdentitySwitchToast(role = it.role) }
                }
            }
        }
    }
}

@Composable
private fun IdentitySwitchToast(role: CoupleRole) {
    val title = when (role) {
        CoupleRole.Caretaker -> "已切换为饲养员"
        CoupleRole.Eater -> "已切换为吃货"
    }
    val subtitle = when (role) {
        CoupleRole.Caretaker -> "现在可以上传菜单啦"
        CoupleRole.Eater -> "去点菜入口已开启"
    }
    val icon = if (role == CoupleRole.Eater) Icons.Filled.Favorite else Icons.Filled.CheckCircle
    val iconColor = if (role == CoupleRole.Eater) ToolRose else ToolAccent

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = ToolInk.copy(alpha = 0.68f),
        shadowElevation = 18.dp
    ) {
        Row(
            modifier = Modifier
                .width(356.dp)
                .height(96.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFF1C9),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title,
                    color = Color(0xFFFFFDF2),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFF8DCC6),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "今天也要好好吃饭",
            color = ToolInk,
            style = MaterialTheme.typography.displayLarge,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = todayText(),
            color = ToolMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun WorkbenchSummary(
    days: Long,
    selectedRole: CoupleRole?,
    profile: Profile?,
    pairInfo: PairInfo,
    onPartnerClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFF1C8), Color(0xFFFFD8D3), Color(0xFFFFC6D8))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RelationshipSlots(
                selectedRole = selectedRole,
                profile = profile,
                pairInfo = pairInfo,
                onPartnerClick = onPartnerClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "一起吃饭",
                color = ToolInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = days.toString(),
                    color = ToolInk,
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "天",
                    color = ToolInk,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun RelationshipSlots(
    selectedRole: CoupleRole?,
    profile: Profile?,
    pairInfo: PairInfo,
    onPartnerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CurrentUserSlot(selectedRole = selectedRole, profile = profile)
        HeartConnector()
        PartnerSlot(
            pairInfo = pairInfo,
            accent = Color(0xFFC87482),
            onClick = onPartnerClick
        )
    }
}

@Composable
private fun CurrentUserSlot(
    selectedRole: CoupleRole?,
    profile: Profile?
) {
    val accent = if (selectedRole == CoupleRole.Eater) Color(0xFFC87482) else Color(0xFFC98A57)
    val roleText = when (selectedRole) {
        CoupleRole.Caretaker -> "饲养员"
        CoupleRole.Eater -> "吃货"
        null -> "选择身份"
    }

    Column(
        modifier = Modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF8EA))
                .drawBehind {
                    drawCircle(
                        color = accent,
                        radius = size.minDimension / 2f - 4.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(62.dp)
            ) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatarUrl,
                        contentDescription = "当前用户头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "当前用户头像",
                            tint = Color.White,
                            modifier = Modifier.size(27.dp)
                        )
                        Text(
                            text = profile?.nickname?.takeIf { it.isNotBlank() }?.take(1) ?: "我",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f))
        ) {
            Text(
                text = roleText,
                color = ToolInk,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PartnerSlot(
    pairInfo: PairInfo,
    accent: Color,
    onClick: () -> Unit
) {
    val label = if (pairInfo.isPaired) pairInfo.partnerName.ifBlank { "我的小伙伴" } else "邀请对方"
    val statusText = when {
        !pairInfo.isPaired -> "等待绑定"
        pairInfo.isOnline -> "在线"
        else -> "已绑定"
    }
    val statusColor = when {
        !pairInfo.isPaired -> ToolMuted
        pairInfo.isOnline -> ToolAccent
        else -> accent
    }
    Column(
        modifier = Modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .background(Color(0xFFFFF8EA))
                .drawBehind {
                    drawCircle(
                        color = accent,
                        radius = size.minDimension / 2f - 4.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            pathEffect = if (pairInfo.isPaired) null else PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (pairInfo.isPaired) {
                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "伴侣头像",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "邀请对方",
                    tint = accent,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = ToolMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(5.dp))
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.28f))
        ) {
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HeartConnector() {
    Box(
        modifier = Modifier.width(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.86f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                }
        )
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.96f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = ToolRose,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun AnniversaryCard(days: Long, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = ToolSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFF1C9),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = ToolRose,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "纪念日小日历",
                    color = ToolInk,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "一起吃饭 $days 天，今天也要好好吃",
                    color = ToolMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF4D7BD))
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.63f)
                            .height(8.dp)
                            .background(ToolAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionGroup(
    selectedRole: CoupleRole?,
    onCaretakerClick: () -> Unit,
    onEaterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            title = "饲养员",
            subtitle = "负责上传菜单",
            icon = Icons.Filled.Storefront,
            selected = selectedRole == CoupleRole.Caretaker,
            accent = ToolGold,
            modifier = Modifier.weight(1f),
            onClick = onCaretakerClick
        )
        ActionButton(
            title = "吃货",
            subtitle = "负责去点菜",
            icon = Icons.Filled.Restaurant,
            selected = selectedRole == CoupleRole.Eater,
            accent = ToolPeach,
            modifier = Modifier.weight(1f),
            onClick = onEaterClick
        )
    }
}

@Composable
private fun RoleFunctionCard(
    selectedRole: CoupleRole?,
    onCustomizeMenuClick: () -> Unit,
    onGoOrderingClick: () -> Unit
) {
    val title = when (selectedRole) {
        CoupleRole.Caretaker -> "饲养员功能"
        CoupleRole.Eater -> "吃货功能"
        null -> "先选择你的身份"
    }
    val actionText = when (selectedRole) {
        CoupleRole.Caretaker -> "上传菜单"
        CoupleRole.Eater -> "去点菜"
        null -> "选择身份后开启"
    }
    val subtitle = when (selectedRole) {
        CoupleRole.Caretaker -> "把想做的菜、价格和库存放进我的店铺"
        CoupleRole.Eater -> "进入我的店铺，开始挑今天想吃的菜"
        null -> "切换成功后，这里会显示对应功能入口"
    }
    val onClick = when (selectedRole) {
        CoupleRole.Caretaker -> onCustomizeMenuClick
        CoupleRole.Eater -> onGoOrderingClick
        null -> ({})
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = selectedRole != null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = ToolSurface,
        border = BorderStroke(1.dp, ToolBorder),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = ToolAccentSoft, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (selectedRole == CoupleRole.Eater) Icons.Filled.Restaurant else Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = ToolAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = ToolInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = ToolMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selectedRole == null) ToolSurfaceAlt else ToolAccent
            ) {
                Text(
                    text = actionText,
                    color = if (selectedRole == null) ToolMuted else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private val activeOrderStatuses = setOf("submitted", "confirmed", "delivering")

@Composable
private fun LatestOrderNudge(
    order: OrderRecord?,
    selectedRole: CoupleRole?,
    onClick: () -> Unit
) {
    if (order == null) return

    val isCaretaker = selectedRole == CoupleRole.Caretaker
    val title = when (order.status) {
        "submitted" -> if (isCaretaker) "有新的点菜单等你接单" else "订单已提交，等饲养员接单"
        "confirmed" -> "饲养员已接单"
        "delivering" -> "这顿饭正在准备中"
        else -> "订单有新进展"
    }
    val subtitle = order.items.take(2).joinToString("、") { it.menuItemName }
        .ifBlank { order.shopName.ifBlank { "我的店铺" } }
    val buttonText = if (isCaretaker && order.status == "submitted") "去接单" else "查看订单"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = ToolInk,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = Color(0xFFFFF1C9), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = ToolRose,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFF8DCC6),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.16f)) {
                Text(
                    text = buttonText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val container = if (selected) {
        Brush.linearGradient(listOf(Color(0xFFFFE4EA), accent))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFDF2), Color(0xFFFFF4C8)))
    }
    Surface(
        modifier = modifier
            .height(158.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .background(container)
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = ToolInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(shape = CircleShape, color = Color(0xFFFFFDF2), modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) ToolRose else ToolAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
            Column {
                Text(subtitle, color = ToolInk, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.76f)) {
                    Text(
                        text = if (selected) "已选择" else "选择身份",
                        color = ToolMuted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun daysEatingTogether(profile: Profile?): Long {
    val start = resolveAnniversaryStartDate(profile)
    return ChronoUnit.DAYS.between(start, LocalDate.now()).coerceAtLeast(0)
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

private fun todayText(): String {
    val calendar = Calendar.getInstance()
    val date = SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(calendar.time)
    val week = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
    return "$date ${week[calendar.get(Calendar.DAY_OF_WEEK) - 1]}"
}
