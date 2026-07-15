package com.myorderapp.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myorderapp.R
import coil3.compose.AsyncImage
import com.myorderapp.ui.auth.AuthBottomLink
import com.myorderapp.ui.auth.AuthGlassCard
import com.myorderapp.ui.auth.AuthInputField
import com.myorderapp.ui.auth.AuthInk
import com.myorderapp.ui.auth.AuthMuted
import com.myorderapp.ui.auth.AuthPrimaryButton
import com.myorderapp.ui.auth.AuthPrimaryEnd
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
    onRegisterComplete: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.registrationComplete) {
        LaunchedEffect(uiState.requiresLoginAfterRegistration) {
            if (uiState.requiresLoginAfterRegistration) onLoginClick() else onRegisterComplete()
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFCF8))
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = AuthPrimaryEnd,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 178.dp, end = 24.dp)
                .size(82.dp)
                .alpha(0.18f)
        )
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
            Text(
                text = "完善个人资料",
                style = MaterialTheme.typography.displayLarge,
                color = AuthPrimaryEnd,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "让对方一眼认出你",
                style = MaterialTheme.typography.bodyMedium,
                color = AuthMuted,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Step2Profile(viewModel, uiState)
        }
    }
}

@Composable
private fun RegisterAccountScreen(
    uiState: OnboardingUiState,
    viewModel: OnboardingViewModel,
    onLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFCF8))
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_orderdisk_dogs_cropped),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 20.dp)
                .size(138.dp)
                .alpha(0.10f)
        )
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
            Spacer(modifier = Modifier.height(92.dp))

            Text(
                text = "创建你们的小饭桌",
                style = MaterialTheme.typography.displayLarge,
                color = AuthInk,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "一起记录每一次想吃什么",
                style = MaterialTheme.typography.bodyMedium,
                color = AuthMuted,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            AuthGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AuthInputField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = "账号/邮箱",
                        placeholder = "账号/邮箱",
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
                        placeholder = "密码",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
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
                        placeholder = "确认密码",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
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

                    Spacer(modifier = Modifier.height(18.dp))

                    AuthPrimaryButton(
                        text = if (uiState.isLoading) "请稍候..." else "下一步",
                        onClick = viewModel::goToStep2,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it
            avatarLocalPath = it.toString()
            viewModel.onAvatarUrlChanged(it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(AuthPrimaryEnd.copy(alpha = 0.06f))
                .clickable { galleryLauncher.launch("image/*") },
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
                DashedAvatarPlaceholder()
            }
        }
        Text(
            "从相册选择头像",
            color = AuthPrimaryEnd,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
        )

        AuthInputField(
            value = uiState.nickname,
            onValueChange = viewModel::onNicknameChanged,
            label = "昵称",
            placeholder = "昵称",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        if (uiState.errorMessage != null) {
            Text(
                uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        AuthPrimaryButton(
            text = if (uiState.isLoading) "注册中..." else "开启甜蜜点菜之旅",
            onClick = { viewModel.completeRegistration(context, avatarUri) },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        AuthBottomLink(
            prefix = "",
            actionText = "返回上一步",
            onClick = { viewModel.goBackToStep1() }
        )
    }
}

@Composable
private fun DashedAvatarPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = AuthPrimaryEnd.copy(alpha = 0.82f),
                radius = size.minDimension / 2f - 3.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
                )
            )
        }
        Icon(
            Icons.Filled.Add,
            contentDescription = "添加头像",
            tint = AuthPrimaryEnd,
            modifier = Modifier.size(42.dp)
        )
    }
}
