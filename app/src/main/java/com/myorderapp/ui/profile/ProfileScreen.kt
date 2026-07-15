package com.myorderapp.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.BuildConfig
import com.myorderapp.ui.components.CandyCoinIcon
import com.myorderapp.ui.auth.AuthViewModel
import com.myorderapp.ui.components.CozyCherry
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyMotionVisibility
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyPage
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.components.CozySurface
import com.myorderapp.ui.components.ImageSourcePickerDialog
import com.myorderapp.ui.components.cozyTextFieldColors
import com.myorderapp.domain.model.ROLE_CARETAKER
import com.myorderapp.domain.model.ROLE_EATER
import com.myorderapp.data.sync.CloudSyncPhase
import com.myorderapp.ui.theme.Error
import com.myorderapp.ui.theme.OnSurface
import com.myorderapp.ui.theme.OnSurfaceVariant
import com.myorderapp.ui.theme.OutlineVariant
import com.myorderapp.ui.theme.Primary
import com.myorderapp.ui.theme.PrimaryContainer
import com.myorderapp.ui.theme.Secondary
import com.myorderapp.ui.theme.SecondaryContainer
import com.myorderapp.ui.theme.SurfaceVariant
import org.koin.androidx.compose.koinViewModel

private const val COUPLE_HOME_PREFS = "couple_home_prefs"
private const val KEY_SELECTED_ROLE = "selected_role"

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    onLoginClick: () -> Unit = {},
    onDishManageClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onCandyCoinsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile
    val savedName = profile?.nickname?.takeIf { it.isNotBlank() }
    val displayName = savedName ?: "糯米小狗"
    val context = LocalContext.current
    val rolePrefs = remember(context) { context.getSharedPreferences(COUPLE_HOME_PREFS, Context.MODE_PRIVATE) }
    val selectedRoleKey = profile?.selectedRole
        ?.takeIf { it == ROLE_CARETAKER || it == ROLE_EATER }
        ?: rolePrefs.getString(KEY_SELECTED_ROLE, null)
    val selectedRoleText = when (selectedRoleKey) {
        "caretaker" -> "饲养员"
        "eater" -> "吃货"
        else -> "待选择"
    }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showPairDialog by remember { mutableStateOf(false) }
    var showUnpairConfirm by remember { mutableStateOf(false) }

    if (showProfileEditor) {
        ProfileEditDialog(
            name = savedName.orEmpty(),
            avatarUrl = profile?.avatarUrl.orEmpty(),
            message = uiState.saveMessage,
            onDismiss = { showProfileEditor = false },
            onDismissMessage = viewModel::dismissMessage,
            onSave = { name, avatarUri -> viewModel.saveProfileEdits(context, name, avatarUri) }
        )
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showVersionDialog) {
        VersionInfoDialog(onDismiss = { showVersionDialog = false })
    }

    if (showPairDialog) {
        PairManagementDialog(
            uiState = uiState,
            onDismiss = {
                showPairDialog = false
                viewModel.dismissMessage()
            },
            onGeneratePairCode = {
                viewModel.generatePairCode(selectedRoleKey)
            },
            onJoinPairCodeChanged = viewModel::onJoinPairCodeChanged,
            onPreviewPairInvite = {
                viewModel.previewPairInvite(uiState.joinPairCode)
            },
            onConfirmPairInvite = {
                viewModel.joinPair(uiState.joinPairCode, uiState.invitePreview?.inviteeRole) { role ->
                    rolePrefs.edit().putString(KEY_SELECTED_ROLE, role).apply()
                }
            },
            onUnpair = {
                showUnpairConfirm = true
            }
        )
    }

    if (showUnpairConfirm) {
        AlertDialog(
            onDismissRequest = { showUnpairConfirm = false },
            title = { Text("确认解除绑定？", fontWeight = FontWeight.Black) },
            text = { Text("解除后双方将停止同步新内容，共同订单、店铺和纪念日会各自保留。对方会收到解绑通知。") },
            confirmButton = {
                TextButton(onClick = {
                    showUnpairConfirm = false
                    showPairDialog = false
                    rolePrefs.edit().remove(KEY_SELECTED_ROLE).apply()
                    viewModel.unpair()
                }) { Text("确认解除", color = Error, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showUnpairConfirm = false }) { Text("暂不解除") } }
        )
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                authViewModel.logout(onLoggedOut = onLoginClick)
            }
        )
    }

    val navigationBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CozyPage(decorative = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
                contentPadding = PaddingValues(bottom = navigationBottomPadding + 132.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    CozyMotionVisibility {
                        ImmersiveProfileHeader(
                            name = displayName,
                            avatarUrl = profile?.avatarUrl,
                            userId = profile?.userId.orEmpty(),
                            isSynced = uiState.isSynced,
                            syncPhase = uiState.cloudSyncState.phase,
                            failedSyncSteps = uiState.cloudSyncState.failedSteps,
                            isPaired = uiState.pairInfo.isPaired,
                            selectedRoleText = selectedRoleText,
                            onSettingsClick = { showProfileEditor = true },
                            onDishManageClick = onDishManageClick,
                            onOrdersClick = onOrdersClick
                        )
                    }
                }
                item {
                    CozyMotionVisibility(delayMillis = 40) {
                        SimulatedCurrencyBalanceCard(
                            balance = uiState.walletBalance,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        ProfileActionRow(
                            icon = Icons.Filled.Settings,
                            title = "账号设置",
                            onClick = { showProfileEditor = true }
                        )
                        if (selectedRoleKey == "caretaker") {
                            ProfileActionRow(
                                icon = Icons.Filled.Pets,
                                title = "糖糖币专属管理",
                                trailingText = "吃货 ${uiState.walletBalance} 枚",
                                onClick = onCandyCoinsClick
                            )
                        } else {
                            ProfileActionRow(
                                icon = Icons.Filled.Pets,
                                title = "糖糖币明细",
                                trailingText = "${uiState.walletBalance} 枚",
                                onClick = onCandyCoinsClick
                            )
                        }
                        ProfileActionRow(
                            icon = Icons.Filled.PersonAdd,
                            title = if (uiState.pairInfo.isPaired) "伴侣已绑定" else "邀请对方",
                            trailingText = uiState.pairInfo.partnerName.takeIf { it.isNotBlank() },
                            onClick = {
                                viewModel.refreshPairState()
                                showPairDialog = true
                            }
                        )
                        ProfileActionRow(
                            icon = Icons.Filled.Info,
                            title = "版本与更新",
                            trailingText = BuildConfig.VERSION_NAME,
                            onClick = { showVersionDialog = true }
                        )
                        ProfileActionRow(
                            icon = Icons.Outlined.SupportAgent,
                            title = "帮助与客服",
                            onClick = { showHelpDialog = true }
                        )
                    }
                }
                item { LogoutButton(onClick = { showLogoutConfirm = true }, modifier = Modifier.padding(horizontal = 20.dp)) }
            }
        }
    }
}

@Composable
private fun ImmersiveProfileHeader(
    name: String,
    avatarUrl: String?,
    userId: String,
    isSynced: Boolean,
    syncPhase: CloudSyncPhase,
    failedSyncSteps: List<String>,
    isPaired: Boolean,
    selectedRoleText: String,
    onSettingsClick: () -> Unit,
    onDishManageClick: () -> Unit,
    onOrdersClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileHeader(
                name = name,
                avatarUrl = avatarUrl,
                userId = userId,
                isSynced = isSynced,
                syncPhase = syncPhase,
                failedSyncSteps = failedSyncSteps,
                isPaired = isPaired,
                selectedRoleText = selectedRoleText,
                onSettingsClick = onSettingsClick
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileFeatureTile(
                    Icons.Filled.Storefront,
                    "我的店铺",
                    Modifier.weight(1f),
                    onClick = onDishManageClick
                )
                ProfileFeatureTile(
                    Icons.AutoMirrored.Outlined.ReceiptLong,
                    "订单记录",
                    Modifier.weight(1f),
                    onClick = onOrdersClick
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    avatarUrl: String?,
    userId: String,
    isSynced: Boolean,
    syncPhase: CloudSyncPhase,
    failedSyncSteps: List<String>,
    isPaired: Boolean,
    selectedRoleText: String,
    onSettingsClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val displayUserId = userId.takeIf { it.isNotBlank() }?.takeLast(7) ?: "未同步"
    val roleText = selectedRoleText

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) com.myorderapp.ui.components.CozyMotion.SoftPressedScale else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onSettingsClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF8FA).copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.62f))
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-36).dp, y = (-38).dp)
                    .background(SecondaryContainer.copy(alpha = 0.16f), RoundedCornerShape(48.dp))
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 44.dp, y = 42.dp)
                    .background(PrimaryContainer.copy(alpha = 0.10f), CircleShape)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Box(modifier = Modifier.size(102.dp), contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(3.dp, Color(0xFFFFFCF8)),
                    modifier = Modifier.size(96.dp)
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(model = avatarUrl, contentDescription = "头像", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                    } else {
                        Box(modifier = Modifier.background(Color(0xFFFFFCF8)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Pets, contentDescription = null, tint = Primary, modifier = Modifier.size(42.dp))
                        }
                    }
                }
                Surface(shape = CircleShape, color = Primary, border = BorderStroke(1.dp, OutlineVariant), modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑资料", tint = Color.White, modifier = Modifier.padding(7.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = name,
                color = OnSurface,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: $displayUserId",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = when (syncPhase) {
                    CloudSyncPhase.SYNCING -> "正在同步云端数据"
                    CloudSyncPhase.PARTIAL_FAILURE -> failedSyncSteps.toSyncFailureText()
                    CloudSyncPhase.SUCCESS -> "云端数据已同步"
                    CloudSyncPhase.IDLE -> if (isSynced) "资料已同步" else "等待云端同步"
                },
                color = if (syncPhase == CloudSyncPhase.PARTIAL_FAILURE) MaterialTheme.colorScheme.error else OnSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.62f),
                border = BorderStroke(1.dp, OutlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Pets, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "当前身份：$roleText",
                        color = OnSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            }
        }
    }
}

private fun List<String>.toSyncFailureText(): String {
    val labels = mapOf(
        "session" to "登录会话",
        "profile" to "个人资料",
        "shop" to "店铺",
        "menu" to "菜单",
        "orders" to "订单",
        "candy_coins" to "糖糖币记录",
        "preferences" to "偏好设置",
        "dishes" to "菜品库"
    )
    val failed = distinct().map { labels[it] ?: it }
    return if (failed.isEmpty()) "部分数据同步失败" else "${failed.joinToString("、")}同步失败"
}

@Composable
private fun SimulatedCurrencyBalanceCard(
    balance: Int,
    modifier: Modifier = Modifier
) {
    SquishySurface(modifier = modifier.fillMaxWidth(), onClick = {}) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "模拟货币余额",
                        color = OnSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "糖糖币不是金币，但点菜时会真实扣减",
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(shape = RoundedCornerShape(999.dp), color = PrimaryContainer.copy(alpha = 0.72f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        CandyCoinIcon(modifier = Modifier.size(24.dp))
                        Text(
                            text = "$balance 枚",
                            color = Primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Text(
                text = "余额不足时，需要饲养员增加糖糖币后才能提交点菜",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CandyCoinsRechargeDialog(
    balance: Int,
    onDismiss: () -> Unit,
    onRecharge: (Int) -> Unit
) {
    var customAmountText by remember { mutableStateOf("") }
    val customAmount = customAmountText.toIntOrNull()
    val fixedAmounts = listOf(10, 50, 100, 150)
    val canRechargeCustom = customAmount != null && customAmount > 0

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = Color(0xFFFFFCF8),
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFD1DC).copy(alpha = 0.76f),
                border = BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.size(62.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CandyCoinIcon(modifier = Modifier.size(54.dp))
                }
            }
        },
        title = {
            Text(
                text = "糖糖币专属管理",
                color = CozyCocoa,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFFFD1DC).copy(alpha = 0.34f),
                    border = BorderStroke(1.dp, OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("吃货当前余额", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
                            Text("$balance 枚", color = Primary, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.72f)) {
                            Text(
                                text = "真实额度",
                                color = Primary,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Text("快捷充值", color = CozyCocoa, fontWeight = FontWeight.Black)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    fixedAmounts.chunked(2).forEach { rowAmounts ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            rowAmounts.forEach { amount ->
                                RechargeAmountButton(
                                    amount = amount,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onRecharge(amount) }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("自定义金额", color = CozyCocoa, fontWeight = FontWeight.Black)
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { value ->
                            customAmountText = value.filter { it.isDigit() }.take(4)
                        },
                        placeholder = { Text("输入糖糖币数量") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = cozyTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "充值会增加吃货余额，不会增加饲养员自己的余额。",
                        color = CozyMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { customAmount?.let(onRecharge) },
                enabled = canRechargeCustom,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("充值自定义金额", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = CozyMuted)
            }
        }
    )
}

@Composable
private fun RechargeAmountButton(
    amount: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF3F6),
        border = BorderStroke(1.dp, Color(0xFFD6C1C5)),
        modifier = modifier
            .height(58.dp)
            .scale(if (pressed) com.myorderapp.ui.components.CozyMotion.ButtonPressedScale else 1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("+$amount", color = Primary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("糖糖币", color = CozyMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProfileFeatureTile(
    icon: ImageVector,
    title: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    SquishySurface(
        modifier = modifier
            .aspectRatio(1f),
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Surface(shape = CircleShape, color = PrimaryContainer.copy(alpha = 0.72f), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = OnSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    SquishySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = SurfaceVariant, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (leadingContent != null) {
                        leadingContent()
                    } else {
                        Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (trailingText != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFFFE6A7).copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, Color(0xFFE8BE63).copy(alpha = 0.38f))
                ) {
                    Text(
                        trailingText,
                        color = Color(0xFF7A5320),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SquishySurface(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(modifier = modifier.scale(if (pressed) com.myorderapp.ui.components.CozyMotion.SoftPressedScale else 1f)) {
        Surface(
            modifier = Modifier
                .matchParentSize()
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFFCF8),
            border = BorderStroke(1.dp, OutlineVariant)
        ) {
            Box {
                content()
            }
        }
    }
}

@Composable
private fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(modifier = modifier.fillMaxWidth().height(58.dp).scale(if (pressed) com.myorderapp.ui.components.CozyMotion.SoftPressedScale else 1f)) {
        Button(
            onClick = onClick,
            interactionSource = interaction,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Error.copy(alpha = 0.08f),
                contentColor = Error
            ),
            border = BorderStroke(1.dp, Error.copy(alpha = 0.34f))
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("退出登录", fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PairManagementDialog(
    uiState: ProfileUiState,
    onDismiss: () -> Unit,
    onGeneratePairCode: () -> Unit,
    onJoinPairCodeChanged: (String) -> Unit,
    onPreviewPairInvite: () -> Unit,
    onConfirmPairInvite: () -> Unit,
    onUnpair: () -> Unit
) {
    val context = LocalContext.current
    val invitePreview = uiState.invitePreview
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CozySurface,
        title = {
            Text(
                text = if (uiState.pairInfo.isPaired) "伴侣已绑定" else "邀请对方",
                color = CozyCocoa,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (uiState.pairInfo.isPaired) {
                        "已和 ${uiState.pairInfo.partnerName.ifBlank { "对方" }} 绑定。你们正在共享情侣资料、店铺、菜单和订单。"
                    } else {
                        "请先在首页选择身份。饲养员邀请对方去点餐；吃货邀请对方去做饭，确认后才会绑定。"
                    },
                    color = CozyMuted,
                    textAlign = TextAlign.Center
                )
                if (!uiState.pairInfo.isPaired) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = PrimaryContainer.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (uiState.pairCode.isBlank()) "我的邀请码" else "把这个邀请码发给对方",
                                color = CozyCocoa,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = uiState.pairCode.ifBlank { "点击生成" },
                                color = Primary,
                                fontSize = if (uiState.pairCode.isBlank()) 26.sp else 38.sp,
                                lineHeight = if (uiState.pairCode.isBlank()) 32.sp else 44.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.pairCode.isNotBlank()) {
                                Text(
                                    text = "等待对方输入后才会完成绑定",
                                    color = CozyMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = onGeneratePairCode,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text(if (uiState.pairCode.isBlank()) "生成邀请码" else "重新生成", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    TextButton(
                        onClick = { copyPairCode(context, uiState.pairCode) },
                        enabled = uiState.pairCode.isNotBlank(),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("复制邀请码", fontWeight = FontWeight.Black)
                    }
                    OutlinedTextField(
                        value = uiState.joinPairCode,
                        onValueChange = onJoinPairCodeChanged,
                        label = { Text("输入对方邀请码") },
                        singleLine = true,
                        colors = cozyTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (invitePreview != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, OutlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = invitePreview.promptText,
                                    color = CozyCocoa,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "确认后你的身份会自动设置为${invitePreview.inviteeRole.roleDisplayText()}，绑定后需解绑才能更改。",
                                    color = CozyMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                uiState.saveMessage?.let {
                    Text(it, color = Primary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (uiState.pairInfo.isPaired) {
                TextButton(onClick = onUnpair) {
                    Text("解除绑定", color = Error, fontWeight = FontWeight.Black)
                }
            } else {
                Button(
                    onClick = if (invitePreview == null) onPreviewPairInvite else onConfirmPairInvite,
                    enabled = uiState.joinPairCode.length == 6,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(invitePreview?.confirmText ?: "查看邀请", fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = CozyMuted) }
        }
    )
}

private fun String.roleDisplayText(): String = when (this) {
    ROLE_CARETAKER -> "饲养员"
    ROLE_EATER -> "吃货"
    else -> "待选择"
}

private fun copyPairCode(context: Context, code: String) {
    if (code.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("邀请码", code))
    Toast.makeText(context, "已复制邀请码", Toast.LENGTH_SHORT).show()
}

@Composable
private fun VersionInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CozySurface,
        title = {
            Text(
                text = "高糖小食 ${BuildConfig.VERSION_NAME}",
                color = CozyCocoa,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1.0.1 体验优化", color = CozyCocoa, fontWeight = FontWeight.Black)
                Text("• 优化小屏、大字体和横屏平板布局", color = CozyMuted)
                Text("• 修复列表滑动、文字遮挡和弹窗键盘避让", color = CozyMuted)
                Text("• 首页订单通知自适应内容高度，减少多余空白", color = CozyMuted)
                Text("• 解绑后仍可查看共同订单、店铺与纪念日", color = CozyMuted)
                Text("• 完善订单图片、通知跳转和每日推荐", color = CozyMuted)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = CozyRose, fontWeight = FontWeight.Black)
            }
        }
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CozySurface,
        title = {
            Text(
                text = "帮助与客服",
                color = CozyCocoa,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "请联系最帅的管理员。",
                color = CozyMuted,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了", color = Primary, fontWeight = FontWeight.Black)
            }
        }
    )
}

@Composable
private fun LogoutConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CozySurface,
        icon = {
            Surface(
                shape = CircleShape,
                color = Error.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, OutlineVariant),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Error, modifier = Modifier.size(34.dp))
                }
            }
        },
        title = { Text("确认要离开吗？", color = CozyCocoa, fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
        text = { Text("小狗会想念你的哦...", color = CozyMuted, textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = Error)) {
                Text("狠心退出", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("再留一会", color = CozyMuted) } }
    )
}

@Composable
private fun ProfileEditDialog(
    name: String,
    avatarUrl: String,
    message: String?,
    onDismiss: () -> Unit,
    onDismissMessage: () -> Unit,
    onSave: (String, Uri?) -> Unit
) {
    var nameDraft by remember(name) { mutableStateOf(name) }
    var previewAvatar by remember(avatarUrl) { mutableStateOf(avatarUrl) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourcePicker by remember { mutableStateOf(false) }
    val trimmedName = nameDraft.trim()
    val nameError = when {
        trimmedName.isBlank() -> "请输入昵称"
        trimmedName.length > 12 -> "昵称最多 12 个字"
        else -> null
    }
    ImageSourcePickerDialog(
        visible = showImageSourcePicker,
        title = "选择头像",
        onDismiss = { showImageSourcePicker = false },
        onImageSelected = {
            previewAvatar = it.toString()
            selectedAvatarUri = it
            onDismissMessage()
        }
    )

    if (!showImageSourcePicker) {
        AlertDialog(
            modifier = Modifier.imePadding(),
            onDismissRequest = { onDismissMessage(); onDismiss() },
            shape = RoundedCornerShape(28.dp),
            containerColor = CozySurface,
        title = {
            Text(
                "编辑个人资料",
                color = CozyCocoa,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.size(104.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = CozyCherry,
                        border = BorderStroke(2.dp, CozyRose.copy(alpha = 0.26f))
                    ) {
                        if (previewAvatar.isNotBlank()) {
                            AsyncImage(
                                model = previewAvatar,
                                contentDescription = "头像预览",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LocalDining, contentDescription = null, tint = CozyRose, modifier = Modifier.size(42.dp))
                            }
                        }
                    }
                    Surface(
                        onClick = { showImageSourcePicker = true },
                        shape = CircleShape,
                        color = CozyRose,
                        border = BorderStroke(3.dp, CozySurface),
                        modifier = Modifier.align(Alignment.BottomEnd).size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Edit, contentDescription = "更换头像", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                TextButton(onClick = { showImageSourcePicker = true }) {
                    Text("更换头像", color = CozyRose, fontWeight = FontWeight.Black)
                }
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = {
                        if (it.length <= 12) {
                            nameDraft = it
                            onDismissMessage()
                        }
                    },
                    label = { Text("昵称") },
                    isError = nameError != null,
                    supportingText = { Text(nameError ?: "${trimmedName.length}/12") },
                    singleLine = true,
                    colors = cozyTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                message?.let {
                    Text(
                        it,
                        color = if (it.contains("失败") || it.contains("请输入")) Error else CozyRose,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(trimmedName, selectedAvatarUri) }, enabled = nameError == null, shape = RoundedCornerShape(999.dp)) {
                Text("保存资料")
            }
        },
            dismissButton = { TextButton(onClick = { onDismissMessage(); onDismiss() }) { Text("取消", color = CozyMuted) } }
        )
    }
}
