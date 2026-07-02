package com.myorderapp.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.myorderapp.ui.auth.AuthBottomLink
import com.myorderapp.ui.auth.AuthDecoratedBackground
import com.myorderapp.ui.auth.AuthGlassCard
import com.myorderapp.ui.auth.AuthInputField
import com.myorderapp.ui.auth.AuthPrimaryButton
import com.myorderapp.ui.auth.AuthStepPill
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
    onRegisterComplete: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.registrationComplete) {
        LaunchedEffect(Unit) { onRegisterComplete() }
        return
    }

    if (uiState.step == 1) {
        RegisterAccountScreen(
            uiState = uiState,
            viewModel = viewModel,
            onLoginClick = onLoginClick
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
    ) {
        // ── Step Indicator ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepDot(1, uiState.step, "账号")
            StepLine(uiState.step > 1)
            StepDot(2, uiState.step, "资料")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Hero ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "完善你的资料",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "设置头像和昵称，打造你的美食名片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Step Content ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step"
            ) { step ->
                when (step) {
                    2 -> Step2Profile(viewModel, uiState)
                    else -> Step2Profile(viewModel, uiState)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom link
        if (uiState.step == 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "已有账号？",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "点击登录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable { onLoginClick() }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RegisterAccountScreen(
    uiState: OnboardingUiState,
    viewModel: OnboardingViewModel,
    onLoginClick: () -> Unit
) {
    AuthDecoratedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthStepPill(currentStep = 1)

            Spacer(modifier = Modifier.height(54.dp))

            Text(
                text = "创建账号",
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFF173B44),
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "先设置登录信息",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6F858B),
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            AuthGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AuthInputField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = "邮箱",
                        placeholder = "输入邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthInputField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = "密码",
                        placeholder = "输入密码",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
                        supportingText = "至少 6 位",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthInputField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChanged,
                        label = "确认密码",
                        placeholder = "再次输入密码",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
                        supportingText = "两次密码一致后可继续",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            AuthPrimaryButton(
                text = if (uiState.isLoading) "请稍候..." else "下一步",
                onClick = viewModel::goToStep2,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            AuthBottomLink(
                prefix = "已有账号？",
                actionText = "去登录",
                onClick = onLoginClick,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

// ── Step 2: 个人资料 ──
@Composable
private fun Step2Profile(viewModel: OnboardingViewModel, uiState: OnboardingUiState) {
    val context = LocalContext.current
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarLocalPath by remember { mutableStateOf(uiState.avatarUrl) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf(uiState.avatarUrl) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it
            avatarLocalPath = it.toString()
            viewModel.onAvatarUrlChanged(it.toString())
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            avatarUri = cameraUri
            avatarLocalPath = cameraUri.toString()
            viewModel.onAvatarUrlChanged(cameraUri.toString())
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

    if (showPhotoSheet) {
        AlertDialog(
            onDismissRequest = { showPhotoSheet = false },
            title = { Text("设置头像", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButton(
                        onClick = { showPhotoSheet = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("拍照", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    TextButton(
                        onClick = {
                            showPhotoSheet = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("从相册选择", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    TextButton(
                        onClick = {
                            showPhotoSheet = false
                            urlInput = uiState.avatarUrl
                            showUrlDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("输入图片链接", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSheet = false }) { Text("取消") }
            }
        )
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("输入头像链接", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("输入图片链接") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onAvatarUrlChanged(urlInput)
                        avatarLocalPath = urlInput
                        showUrlDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showPhotoSheet = true },
            contentAlignment = Alignment.Center
        ) {
            if (avatarLocalPath.isNotBlank()) {
                AsyncImage(
                    model = avatarLocalPath,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        "点击设置",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nickname
        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = viewModel::onNicknameChanged,
            placeholder = { Text("输入你的昵称") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.goBackToStep1() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("上一步", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = { viewModel.completeRegistration() },
                enabled = !uiState.isLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (uiState.isLoading) "注册中..." else "完成注册",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Step indicator helpers ──
@Composable
private fun StepDot(step: Int, current: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (step == current) 32.dp else 24.dp)
                .clip(RoundedCornerShape(if (step == current) 10.dp else 8.dp))
                .background(
                    when {
                        step < current -> MaterialTheme.colorScheme.primary
                        step == current -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (step < current) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    "$step",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        step <= current -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (step <= current) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepLine(active: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}
