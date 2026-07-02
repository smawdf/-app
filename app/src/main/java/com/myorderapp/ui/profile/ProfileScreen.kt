package com.myorderapp.ui.profile

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.ui.components.OrderCard
import com.myorderapp.ui.theme.Background
import com.myorderapp.ui.theme.OnBackground
import com.myorderapp.ui.theme.OnSurfaceVariant
import com.myorderapp.ui.theme.OrderDiskSpacing
import com.myorderapp.ui.theme.Primary
import com.myorderapp.ui.theme.PrimaryContainer
import com.myorderapp.ui.theme.Surface
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onLoginClick: () -> Unit = {},
    onDishManageClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile
    val displayName = profile?.nickname?.takeIf { it.isNotBlank() } ?: "未设置昵称"
    var showProfileEditor by remember { mutableStateOf(false) }

    if (showProfileEditor) {
        ProfileEditDialog(
            name = displayName,
            avatarUrl = profile?.avatarUrl.orEmpty(),
            onDismiss = { showProfileEditor = false },
            onSave = { name, avatar ->
                viewModel.updateNickname(name)
                if (avatar.isNotBlank()) {
                    viewModel.updateAvatar(avatar)
                }
                showProfileEditor = false
            }
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
                onDishManageClick = onDishManageClick,
                onLoginClick = onLoginClick
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
    onDishManageClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    OrderCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            ProfileActionRow(
                icon = Icons.Outlined.NotificationsNone,
                title = "订单通知设置",
                trailingText = "未开启",
                onClick = {}
            )
            ProfileActionRow(
                icon = Icons.Outlined.PersonAdd,
                title = "邀请小伙伴",
                buttonText = "点击分享",
                onClick = onLoginClick
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
    onSave: (String, String) -> Unit
) {
    var nameDraft by remember(name) { mutableStateOf(name) }
    var avatarDraft by remember(avatarUrl) { mutableStateOf(avatarUrl) }

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
                OutlinedTextField(
                    value = avatarDraft,
                    onValueChange = { avatarDraft = it },
                    label = { Text("头像图片链接") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(nameDraft, avatarDraft) }, shape = RoundedCornerShape(10.dp)) {
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
