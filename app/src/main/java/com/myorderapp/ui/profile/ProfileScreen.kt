package com.myorderapp.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onHistoryClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val preferences = uiState.preferences
    val profile = uiState.profile
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(profile?.nickname ?: "") }
    var showAvatarSheet by remember { mutableStateOf(false) }
    var showAvatarUrlDialog by remember { mutableStateOf(false) }
    var editAvatarUrl by remember { mutableStateOf(profile?.avatarUrl ?: "") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.saveAvatarUri(context, it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            viewModel.saveAvatarUri(context, cameraUri!!)
        }
    }

    fun launchCamera() {
        val photoFile = File(
            context.externalCacheDir ?: context.cacheDir,
            "avatar_${System.currentTimeMillis()}.jpg"
        )
        photoFile.parentFile?.mkdirs()
        cameraUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraUri!!)
    }

    // Edit nickname dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("修改昵称", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { if (it.length <= 12) editName = it },
                    placeholder = { Text("输入新昵称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateNickname(editName)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("取消") }
            }
        )
    }

    // Avatar URL dialog
    if (showAvatarUrlDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarUrlDialog = false },
            title = { Text("设置头像URL", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("输入图片链接地址", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAvatarUrl,
                        onValueChange = { editAvatarUrl = it },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAvatar(editAvatarUrl)
                        showAvatarUrlDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAvatarUrlDialog = false }) { Text("取消") }
            }
        )
    }

    // Avatar source picker (photo / gallery / url)
    if (showAvatarSheet) {
        AlertDialog(
            onDismissRequest = { showAvatarSheet = false },
            title = { Text("设置头像", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButton(
                        onClick = { showAvatarSheet = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📷", fontSize = 20.sp)
                            Text("拍照", fontSize = 16.sp)
                        }
                    }
                    TextButton(
                        onClick = { showAvatarSheet = false; galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🖼️", fontSize = 20.sp)
                            Text("从相册选择", fontSize = 16.sp)
                        }
                    }
                    TextButton(
                        onClick = {
                            showAvatarSheet = false
                            editAvatarUrl = profile?.avatarUrl ?: ""
                            showAvatarUrlDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔗", fontSize = 20.sp)
                            Text("输入URL", fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarSheet = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero Header ──
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar — click to choose photo/gallery/url
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(Brush.linearGradient(
                            listOf(Color(0xFFFF6B35), Color(0xFFFF8A65))
                        ))
                        .clickable { showAvatarSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    val av = profile?.avatarUrl
                    if (!av.isNullOrBlank()) {
                        AsyncImage(
                            model = av,
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            if (profile?.nickname.isNullOrBlank()) "👤"
                            else profile!!.nickname.first().toString(),
                            fontSize = 32.sp, color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Name row with edit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        editName = profile?.nickname ?: ""
                        showEditNameDialog = true
                    }
                ) {
                    Text(
                        profile?.nickname?.ifBlank { "点击设置昵称" } ?: "点击设置昵称",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("✏️", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Pair status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.pairInfo.isPaired) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        if (uiState.pairInfo.isPaired) {
                            val name = uiState.pairInfo.partnerName
                            "💕 已配对" + if (name.isNotBlank()) " · $name" else ""
                        } else "🔗 尚未配对",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = if (uiState.pairInfo.isPaired) Color(0xFF4CAF50) else Color(0xFFFF6B35)
                    )
                }

                // Save status message
                if (uiState.saveMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uiState.saveMessage!!.contains("✓") || uiState.saveMessage!!.contains("💕"))
                            Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                        onClick = { viewModel.dismissMessage() }
                    ) {
                        Text(
                            uiState.saveMessage!!,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            color = if (uiState.saveMessage!!.contains("✓") || uiState.saveMessage!!.contains("💕"))
                                Color(0xFF4CAF50) else Color(0xFFFF6B35)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Taste Preferences ──
        Text("口味偏好", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp))
        Text("选择你的口味，推荐更精准", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    preferences.forEach { pref ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (pref.value) Color(0xFFFFF3E0) else Color(0xFFF5F5F5),
                            onClick = {
                                val idx = preferences.indexOf(pref)
                                if (idx >= 0) viewModel.togglePreference(idx)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(pref.emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(pref.label, fontSize = 11.sp,
                                    fontWeight = if (pref.value) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (pref.value) Color(0xFFFF6B35) else Color(0xFF999999))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Pairing ──
        Text("配对管理", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape, color = Color(0xFFFFF3E0),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("👥", fontSize = 20.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (uiState.pairInfo.isPaired) "已配对" else "生成配对码",
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                    Text(
                        if (uiState.pairInfo.isPaired) "对方可以通过配对码加入" else "让对方输入此码完成配对",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (uiState.pairInfo.isPaired) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFEBEE),
                        onClick = { viewModel.unpair() }
                    ) {
                        Text("解除", fontSize = 11.sp, color = Color(0xFFE53935),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                } else {
                    Button(
                        onClick = { viewModel.generatePairCode() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("生成", fontSize = 12.sp)
                    }
                }
            }

            // Generated pair code display
            if (uiState.pairCode.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text("配对码：", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.pairCode, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B35), letterSpacing = 3.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("将此码分享给对方，对方在下方输入即可配对",
                        fontSize = 10.sp, color = Color(0xFF999999))
                }
            }

            // Join pair input area (always show when not paired)
            if (!uiState.pairInfo.isPaired) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.joinPairCode,
                        onValueChange = viewModel::onJoinPairCodeChanged,
                        placeholder = { Text("输入对方配对码", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp, letterSpacing = 2.sp
                        )
                    )
                    Button(
                        onClick = { viewModel.joinPair(uiState.joinPairCode) },
                        enabled = uiState.joinPairCode.length == 6,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("加入", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── More ──
        Text("更多", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                SettingsRow("📊", "历史记录", "查看过往点餐", onClick = onHistoryClick)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow("📋", "菜品管理", "管理你的菜品库")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsRow("ℹ️", "关于", "今天吃什么？v1.0")
            }
        }

        // Sync / Login status
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onLoginClick() },
            shape = RoundedCornerShape(12.dp),
            color = if (uiState.isSynced) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (uiState.isSynced) Color(0xFF4CAF50) else Color(0xFFFFA726))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (uiState.isSynced) "在线模式 · 数据实时同步"
                    else "离线模式 · 点击登录同步数据",
                    fontSize = 12.sp,
                    color = if (uiState.isSynced) Color(0xFF4CAF50) else Color(0xFFFF6B35)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsRow(emoji: String, title: String, subtitle: String = "", onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
