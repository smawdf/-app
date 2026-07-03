package com.myorderapp.ui.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.myorderapp.domain.model.PairInfo
import coil3.compose.AsyncImage
import com.myorderapp.ui.auth.AuthViewModel
import com.myorderapp.ui.components.OrderCard
import com.myorderapp.ui.theme.Background
import com.myorderapp.ui.theme.OnBackground
import com.myorderapp.ui.theme.OnSurfaceVariant
import com.myorderapp.ui.theme.OrderDiskSpacing
import com.myorderapp.ui.theme.Primary
import com.myorderapp.ui.theme.PrimaryContainer
import com.myorderapp.ui.theme.Surface
import org.koin.androidx.compose.koinViewModel

private const val PROFILE_PREFS = "profile_screen_prefs"
private const val KEY_ORDER_NOTIFICATIONS_ENABLED = "order_notifications_enabled"

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    onLoginClick: () -> Unit = {},
    onDishManageClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val displayName = profile?.nickname?.takeIf { it.isNotBlank() } ?: "未设置昵称"
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
    }
    var notificationsEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, false))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            notificationsEnabled = true
            prefs.edit().putBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, true).apply()
        }
    }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showPairDialog by remember { mutableStateOf(false) }

    if (showProfileEditor) {
        ProfileEditDialog(
            name = displayName,
            avatarUrl = profile?.avatarUrl.orEmpty(),
            onDismiss = { showProfileEditor = false },
            onAvatarSelected = { uri ->
                viewModel.saveAvatarUri(context, uri)
            },
            onSave = { name ->
                viewModel.updateNickname(name)
                showProfileEditor = false
            }
        )
    }

    if (showPairDialog) {
        PairManagementDialog(
            pairInfo = uiState.pairInfo,
            pairCode = uiState.pairCode,
            joinPairCode = uiState.joinPairCode,
            message = uiState.saveMessage,
            onGenerateCode = viewModel::generatePairCode,
            onJoinCodeChanged = viewModel::onJoinPairCodeChanged,
            onJoin = { viewModel.joinPair(uiState.joinPairCode) },
            onUnpair = viewModel::unpair,
            onDismissMessage = viewModel::dismissMessage,
            onDismiss = { showPairDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(OrderDiskSpacing.lg)
    ) {
        item {
            ProfileHeader(
                name = displayName,
                avatarUrl = profile?.avatarUrl,
                onSettingsClick = { showProfileEditor = true }
            )
        }

        item {
            ProfileActionList(
                pairInfo = uiState.pairInfo,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = {
                    val shouldEnable = !notificationsEnabled
                    if (shouldEnable &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationsEnabled = shouldEnable
                        prefs.edit().putBoolean(KEY_ORDER_NOTIFICATIONS_ENABLED, shouldEnable).apply()
                    }
                },
                onPairClick = { showPairDialog = true },
                onDishManageClick = onDishManageClick,
                onLoginClick = onLoginClick
            )
        }

        item {
            LogoutButton(
                onClick = {
                    authViewModel.logout(onLoggedOut = onLoginClick)
                }
            )
        }
    }
}

@Composable
private fun ProfileHeader(name: String, avatarUrl: String?, onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(142.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = 54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = CircleShape,
                color = Surface,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.LocalDining, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = OnBackground,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = PrimaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "个人中心",
                        color = OnBackground,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onSettingsClick),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "设置头像和名称", tint = Primary, modifier = Modifier.padding(8.dp).size(28.dp))
        }
    }
}

@Composable
private fun ProfileActionList(
    pairInfo: PairInfo,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onPairClick: () -> Unit,
    onDishManageClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    OrderCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            ProfileActionRow(
                icon = Icons.Outlined.NotificationsNone,
                title = "订单通知设置",
                trailingText = if (notificationsEnabled) "已开启" else "未开启",
                onClick = onToggleNotifications
            )
            ProfileActionRow(
                icon = Icons.Outlined.PersonAdd,
                title = if (pairInfo.isPaired) "伴侣已绑定" else "邀请小伙伴",
                buttonText = if (pairInfo.isPaired) "管理" else "绑定",
                onClick = onPairClick
            )
            ProfileActionRow(
                icon = Icons.Outlined.Storefront,
                title = "厨房设置",
                onClick = onDishManageClick
            )
        }
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE54848),
                contentColor = Color.White
            )
        ) {
            Text("退出登录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "退出后会回到登录页，本地菜单和订单记录仍会保留。",
            color = OnSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun PairManagementDialog(
    pairInfo: PairInfo,
    pairCode: String,
    joinPairCode: String,
    message: String?,
    onGenerateCode: () -> Unit,
    onJoinCodeChanged: (String) -> Unit,
    onJoin: () -> Unit,
    onUnpair: () -> Unit,
    onDismissMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onDismissMessage()
            onDismiss()
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Surface,
        title = {
            Text(
                text = if (pairInfo.isPaired) "管理伴侣绑定" else "邀请小伙伴",
                color = OnBackground,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pairInfo.isPaired) {
                    Text(
                        text = "当前已绑定：${pairInfo.partnerName.ifBlank { "我的小伙伴" }}",
                        color = OnBackground
                    )
                    Text(
                        text = "绑定码：${pairCode.ifBlank { pairInfo.pairCode.ifBlank { "已保存" } }}",
                        color = OnSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    Text("生成邀请码给对方，或者输入对方发来的 6 位绑定码。", color = OnSurfaceVariant)
                    Surface(shape = RoundedCornerShape(12.dp), color = PrimaryContainer) {
                        Text(
                            text = pairCode.ifBlank { "还没有邀请码" },
                            color = OnBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                    OutlinedTextField(
                        value = joinPairCode,
                        onValueChange = onJoinCodeChanged,
                        label = { Text("输入 6 位绑定码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                message?.let {
                    Text(text = it, color = Primary, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            if (pairInfo.isPaired) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onGenerateCode) {
                        Text("重新生成邀请码", color = Primary)
                    }
                    TextButton(onClick = onUnpair) {
                        Text("解除绑定", color = Primary)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onGenerateCode) {
                        Text("生成邀请码", color = Primary)
                    }
                    Button(
                        onClick = onJoin,
                        enabled = joinPairCode.length == 6,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("绑定")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissMessage()
                    onDismiss()
                }
            ) {
                Text("关闭", color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    buttonText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(22.dp))
        Text(
            text = title,
            color = OnBackground,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        when {
            trailingText != null -> Text(text = trailingText, color = Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            buttonText != null -> TextButton(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                )
            ) {
                Text(buttonText, fontSize = 16.sp)
            }
            else -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun ProfileEditDialog(
    name: String,
    avatarUrl: String,
    onDismiss: () -> Unit,
    onAvatarSelected: (android.net.Uri) -> Unit,
    onSave: (String) -> Unit
) {
    var nameDraft by remember(name) { mutableStateOf(name) }
    var previewAvatar by remember(avatarUrl) { mutableStateOf(avatarUrl) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            previewAvatar = it.toString()
            onAvatarSelected(it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = Surface,
        title = { Text("设置头像和名称", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(58.dp),
                        shape = CircleShape,
                        color = PrimaryContainer
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
                                Icon(
                                    Icons.Outlined.LocalDining,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "头像从本地相册选择",
                            color = OnBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "不再手动填写图片链接",
                            color = OnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
                Button(
                    onClick = { avatarPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = OnBackground
                    )
                ) {
                    Text("从相册选择头像")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(nameDraft) }, shape = RoundedCornerShape(10.dp)) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = OnSurfaceVariant)
            }
        }
    )
}
