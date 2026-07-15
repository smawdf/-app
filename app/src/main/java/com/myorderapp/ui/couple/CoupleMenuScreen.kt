package com.myorderapp.ui.couple

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.domain.model.OrderRecord
import com.myorderapp.domain.model.PairInfo
import com.myorderapp.domain.model.Profile
import com.myorderapp.domain.repository.OrderRepository
import com.myorderapp.domain.repository.ProfileRepository
import com.myorderapp.data.repository.UserPreferencesRepository
import com.myorderapp.ui.components.CozyCard
import com.myorderapp.ui.components.CozyBorder
import com.myorderapp.ui.components.CozyCherry
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyIconBadge
import com.myorderapp.ui.components.CozyMainTopBar
import com.myorderapp.ui.components.CozyMotion
import com.myorderapp.ui.components.CozyMotionVisibility
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyPage
import com.myorderapp.ui.components.CozyPill
import com.myorderapp.ui.components.CozyPink
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.components.CozySurface
import com.myorderapp.ui.components.CozyTerracotta
import com.myorderapp.ui.notifications.notifyActiveOrderIfAllowed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

private enum class CoupleRole {
    Caretaker,
    Eater;

    val storageKey: String
        get() = when (this) {
            Caretaker -> "caretaker"
            Eater -> "eater"
        }

    val label: String
        get() = when (this) {
            Caretaker -> "饲养员"
            Eater -> "吃货"
        }
}

private data class RoleToastState(val id: Int, val role: CoupleRole)

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
    viewModel: CoupleMenuViewModel = koinViewModel(),
    profileRepository: ProfileRepository = koinInject(),
    orderRepository: OrderRepository = koinInject(),
    userPreferencesRepository: UserPreferencesRepository = koinInject(),
    onCustomizeMenuClick: () -> Unit = {},
    onGoOrderingClick: () -> Unit = {},
    onAnniversaryClick: () -> Unit = {},
    onDiscoverClick: () -> Unit = {},
    onOrderClick: (String) -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val profile by profileRepository.getProfile().collectAsStateWithLifecycle(initialValue = null)
    val orders by orderRepository.observeOrders().collectAsStateWithLifecycle(initialValue = emptyList())
    val pairInfo by viewModel.pairInfo.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(COUPLE_HOME_PREFS, Context.MODE_PRIVATE) }
    val profilePrefs = remember(context) { context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE) }
    val orderNotificationsEnabled by userPreferencesRepository.orderNotificationsEnabled.collectAsStateWithLifecycle()
    val rolePreferenceKey = remember(profile?.userId) { "$KEY_SELECTED_ROLE:${profile?.userId.orEmpty()}" }
    var selectedRole by rememberSaveable(profile?.userId) {
        mutableStateOf(
            profile?.selectedRole.toCoupleRole()
                ?: profile?.userId?.takeIf { it.isNotBlank() }
                    ?.let { prefs.getString(rolePreferenceKey, null).toCoupleRole() }
        )
    }
    var toastState by remember { mutableStateOf<RoleToastState?>(null) }
    var pairNotice by remember { mutableStateOf<String?>(null) }
    var toastId by remember { mutableIntStateOf(0) }

    fun selectRole(role: CoupleRole) {
        if (pairInfo.isPaired) return
        selectedRole = role
        if (!profile?.userId.isNullOrBlank()) {
            prefs.edit().putString(rolePreferenceKey, role.storageKey).apply()
        }
        toastId += 1
        toastState = RoleToastState(toastId, role)
    }

    LaunchedEffect(selectedRole?.storageKey, profile?.userId) {
        val role = selectedRole?.storageKey ?: return@LaunchedEffect
        if (!profile?.userId.isNullOrBlank() && profile?.selectedRole != role) {
            profileRepository.saveSelectedRole(role)
        }
    }

    LaunchedEffect(profile?.selectedRole) {
        val cloudRole = profile?.selectedRole.toCoupleRole()
        if (cloudRole != null && cloudRole != selectedRole) {
            selectedRole = cloudRole
            prefs.edit().putString(rolePreferenceKey, cloudRole.storageKey).apply()
        }
    }

    LaunchedEffect(lifecycleOwner, profile?.userId, profile?.pairId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshWhileActive()
        }
    }

    val activeOrder = orders.firstOrNull { it.status in activeOrderStatuses }
    LaunchedEffect(activeOrder?.id, activeOrder?.status, selectedRole) {
        val order = activeOrder ?: return@LaunchedEffect
        val notificationsEnabled = orderNotificationsEnabled || profilePrefs.getBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, false)
        val notifiedKey = "${order.id}:${order.status}"
        if (notificationsEnabled && profilePrefs.getString(KEY_LAST_NOTIFIED_ORDER_ID, "") != notifiedKey) {
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
            delay(1600)
            toastState = null
        }
    }

    LaunchedEffect(pairInfo.noticeId) {
        if (pairInfo.noticeId.isNotBlank() && pairInfo.noticeMessage.isNotBlank()) {
            pairNotice = pairInfo.noticeMessage
            delay(2600)
            pairNotice = null
        }
    }

    CozyPage(decorative = false) {
        HomeDecorativeBubbles()
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 188.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                CozyMotionVisibility {
                    RelationshipCard(
                        days = daysEatingTogether(profile),
                        selectedRole = selectedRole,
                        profile = profile,
                        pairInfo = pairInfo,
                        onPartnerClick = onProfileClick,
                        onAnniversaryClick = onAnniversaryClick
                    )
                }
                CozyMotionVisibility(delayMillis = 40) {
                    LatestOrderNudge(
                        order = activeOrder,
                        onClick = { activeOrder?.id?.let(onOrderClick) }
                    )
                }
                CozyMotionVisibility(delayMillis = 80) {
                    QuickActionGrid(
                        onAnniversaryClick = onAnniversaryClick,
                        onCustomizeMenuClick = onCustomizeMenuClick,
                        onGoOrderingClick = onGoOrderingClick
                    )
                }
                CozyMotionVisibility(delayMillis = 120) {
                    RoleSwitcher(
                        selectedRole = selectedRole,
                        rolesLocked = pairInfo.isPaired && selectedRole != null,
                        onCaretakerClick = { selectRole(CoupleRole.Caretaker) },
                        onEaterClick = { selectRole(CoupleRole.Eater) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = toastState != null,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(CozyMotion.Toast)),
            exit = fadeOut(tween(CozyMotion.Toast))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visible = toastState != null,
                    enter = fadeIn(tween(CozyMotion.Toast)) + slideInVertically(tween(CozyMotion.Standard)) { it / 8 },
                    exit = fadeOut(tween(CozyMotion.Exit)) + slideOutVertically(tween(CozyMotion.Exit)) { it / 8 }
                ) {
                    toastState?.let { IdentitySwitchToast(role = it.role) }
                }
            }
        }
        AnimatedVisibility(
            visible = pairNotice != null,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(CozyMotion.Toast)),
            exit = fadeOut(tween(CozyMotion.Toast))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CozyCocoa.copy(alpha = 0.92f),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = pairNotice.orEmpty(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDecorativeBubbles() {
    Box(modifier = Modifier.fillMaxSize()) {
        DecorativeBubble(
            icon = Icons.Filled.Favorite,
            tint = CozyRose.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 38.dp, top = 48.dp)
                .size(24.dp)
        )
        DecorativeBubble(
            icon = Icons.Filled.Pets,
            tint = CozyTerracotta.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 58.dp, top = 92.dp)
                .size(20.dp)
        )
        DecorativeBubble(
            icon = Icons.Filled.Restaurant,
            tint = CozyPink.copy(alpha = 0.14f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
                .size(28.dp)
        )
    }
}

@Composable
private fun DecorativeBubble(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}

@Composable
private fun HomeHeader() {
    CozyMainTopBar(title = "今天也要一起好好吃饭")
    return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .background(CozySurface.copy(alpha = 0.94f))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.CenterStart) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = CozyRose.copy(alpha = 0.82f), modifier = Modifier.size(26.dp))
        }
        Text(
            text = "今天也要一起好好吃饭",
            color = CozyRose,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Box(
            modifier = Modifier.width(44.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = "通知", tint = CozyMuted, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun RelationshipCard(
    days: Long,
    selectedRole: CoupleRole?,
    profile: Profile?,
    pairInfo: PairInfo,
    onPartnerClick: () -> Unit,
    onAnniversaryClick: () -> Unit
) {
    CozyCard(
        modifier = Modifier.padding(horizontal = 20.dp),
        containerColor = CozyPink.copy(alpha = 0.30f),
        borderColor = CozyPink.copy(alpha = 0.42f),
        radius = 26,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp)
                .background(CozyPink.copy(alpha = 0.18f))
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.09f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(210.dp)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrentUserSlot(
                        profile = profile,
                        selectedRole = selectedRole,
                        modifier = Modifier.weight(1f)
                    )
                    HeartConnector()
                    PartnerSlot(
                        pairInfo = pairInfo,
                        onClick = onPartnerClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                Surface(
                    onClick = onAnniversaryClick,
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, CozyRose.copy(alpha = 0.18f)),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("一起吃饭 $days 天", color = CozyRose, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Black)
                        Text("今天也想和你好好吃饭", color = CozyMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentUserSlot(
    profile: Profile?,
    selectedRole: CoupleRole?,
    modifier: Modifier = Modifier
) {
    val name = profile?.nickname?.takeIf { it.isNotBlank() } ?: "我"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        AvatarBubble(
            avatarUrl = profile?.avatarUrl,
            fallback = name.take(1),
            label = "当前用户",
            filled = true,
            showPetFallback = true,
            onClick = null
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, color = CozyCocoa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(6.dp))
        RoleLabelPill(text = "当前角色：${selectedRole?.label ?: "选择身份"}")
    }
}

@Composable
private fun PartnerSlot(
    pairInfo: PairInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = if (pairInfo.isPaired) pairInfo.partnerName.ifBlank { "对方" } else "邀请对方"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        AvatarBubble(
            avatarUrl = pairInfo.partnerAvatarUrl,
            fallback = name.take(1),
            label = name,
            filled = pairInfo.isPaired,
            onClick = onClick
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, color = CozyCocoa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(modifier = Modifier.height(6.dp))
        PartnerStatusSpacer(
            text = when {
                !pairInfo.isPaired -> ""
                pairInfo.isOnline -> "在线"
                else -> "已绑定"
            }
        )
    }
}

@Composable
private fun PartnerStatusSpacer(text: String) {
    Text(
        text = text,
        color = CozyMuted.copy(alpha = 0.0f),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.height(18.dp)
    )
}

@Composable
private fun RoleLabelPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, CozyRose.copy(alpha = 0.10f)),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            color = CozyMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun AvatarBubble(
    avatarUrl: String?,
    fallback: String,
    label: String,
    filled: Boolean,
    showPetFallback: Boolean = false,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .size(84.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(if (filled) 3.dp else 2.dp, if (filled) CozyRose.copy(alpha = 0.55f) else CozyMuted.copy(alpha = 0.35f)),
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else if (showPetFallback) {
                Icon(Icons.Filled.Pets, contentDescription = label, tint = CozyRose, modifier = Modifier.size(36.dp))
            } else if (filled) {
                Text(fallback, color = CozyRose, fontSize = 28.sp, fontWeight = FontWeight.Black)
            } else {
                Icon(Icons.Filled.Add, contentDescription = label, tint = CozyRose, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun HeartConnector() {
    val transition = rememberInfiniteTransition(label = "relationshipHeartBeat")
    val scale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "relationshipHeartScale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
        Surface(modifier = Modifier.scale(scale), shape = CircleShape, color = Color.White.copy(alpha = 0.92f), shadowElevation = 0.dp) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = CozyRose, modifier = Modifier.padding(8.dp).size(22.dp))
        }
    }
}

@Composable
private fun QuickActionGrid(
    onAnniversaryClick: () -> Unit,
    onCustomizeMenuClick: () -> Unit,
    onGoOrderingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            title = "纪念日",
            subtitle = "记录我们一起吃饭的日子",
            icon = Icons.Filled.Event,
            tint = CozyTerracotta,
            modifier = Modifier
                .weight(1f)
                .height(128.dp),
            vertical = true,
            onClick = onAnniversaryClick
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "我的店铺",
                subtitle = "上传菜单，整理菜品",
                icon = Icons.Filled.Storefront,
                tint = CozyRose,
                modifier = Modifier.height(58.dp),
                onClick = onCustomizeMenuClick
            )
            QuickActionCard(
                title = "去点菜",
                subtitle = "看看今天想吃什么",
                icon = Icons.Filled.RestaurantMenu,
                tint = CozyTerracotta,
                modifier = Modifier.height(58.dp),
                onClick = onGoOrderingClick
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    vertical: Boolean = false,
    onClick: () -> Unit
) {
    CozyCard(
        modifier = modifier,
        radius = 22,
        onClick = onClick,
        containerColor = Color.White.copy(alpha = 0.90f),
        borderColor = CozyRose.copy(alpha = 0.20f),
        contentPadding = PaddingValues(12.dp)
    ) {
        if (vertical) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                CozyIconBadge(icon = icon, background = tint.copy(alpha = 0.13f), tint = tint)
                Column {
                    Text(title, color = CozyCocoa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(subtitle, color = CozyMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CozyIconBadge(icon = icon, background = tint.copy(alpha = 0.13f), tint = tint)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = CozyCocoa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(subtitle, color = CozyMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RoleSwitcher(
    selectedRole: CoupleRole?,
    rolesLocked: Boolean,
    onCaretakerClick: () -> Unit,
    onEaterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoleCard(
            role = CoupleRole.Caretaker,
            subtitle = "上传菜单，照顾小饭桌",
            icon = Icons.Filled.SoupKitchen,
            selected = selectedRole == CoupleRole.Caretaker,
            locked = rolesLocked,
            accent = CozyRose,
            modifier = Modifier.weight(1f),
            onClick = onCaretakerClick
        )
        RoleCard(
            role = CoupleRole.Eater,
            subtitle = "浏览菜单，准备开饭",
            icon = Icons.Filled.Restaurant,
            selected = selectedRole == CoupleRole.Eater,
            locked = rolesLocked,
            accent = CozyTerracotta,
            modifier = Modifier.weight(1f),
            onClick = onEaterClick
        )
    }
}

@Composable
private fun RoleCard(
    role: CoupleRole,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    locked: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    CozyCard(
        modifier = modifier.aspectRatio(1f),
        containerColor = if (selected) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.82f),
        borderColor = if (selected) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f),
        radius = 22,
        onClick = { if (!locked) onClick() },
        contentPadding = PaddingValues(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                shape = RoundedCornerShape(bottomStart = 48.dp),
                color = accent.copy(alpha = if (selected) 0.12f else 0.05f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(72.dp)
            ) {}
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                CozyIconBadge(icon = icon, background = accent.copy(alpha = 0.14f), tint = accent, modifier = Modifier.size(42.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(role.label, color = CozyCocoa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    if (selected) {
                        Surface(shape = RoundedCornerShape(999.dp), color = accent.copy(alpha = 0.92f)) {
                            Text(
                                "当前角色",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        subtitle,
                        color = CozyMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        lineHeight = 15.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            locked && selected -> "已绑定，身份锁定"
                            locked -> "解绑后可更改"
                            selected -> "当前：${role.label}"
                            else -> "切换为${role.label}"
                        },
                        color = if (selected) accent else CozyMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentitySwitchToast(role: CoupleRole) {
    Surface(
        modifier = Modifier.size(188.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CozyPink.copy(alpha = 0.16f))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CozyIconBadge(
                icon = if (role == CoupleRole.Caretaker) Icons.Filled.CheckCircle else Icons.Filled.Favorite,
                background = Color.White.copy(alpha = 0.58f),
                tint = CozyRose,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("已切换为${role.label}", color = CozyCocoa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    if (role == CoupleRole.Caretaker) "现在可以上传菜单啦" else "去看看今天想吃什么吧",
                    color = CozyMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

private val activeOrderStatuses = setOf("submitted", "confirmed", "preparing", "delivering")

@Composable
private fun LatestOrderNudge(
    order: OrderRecord?,
    onClick: () -> Unit
) {
    if (order == null) return

    val title = when (order.status) {
        "submitted", "confirmed" -> "待饲养员确认接单"
        "preparing", "delivering" -> "订单正在准备中"
        else -> "订单有新进展"
    }
    val subtitle = order.items.take(2).joinToString("、") { "${it.menuItemName}×${it.quantity}" }
        .ifBlank { order.shopName.ifBlank { "我的小店" } }
    CozyCard(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 2.dp)
            .height(104.dp),
        containerColor = Color(0xFFFFFCF8),
        borderColor = CozyBorder.copy(alpha = 0.72f),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CozyIconBadge(Icons.Filled.Restaurant, background = Color(0xFFFFD1DC), tint = CozyRose)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = CozyCocoa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = CozyMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            CozyPill(text = "查看", color = CozyPink)
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
    return runCatching { Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}
