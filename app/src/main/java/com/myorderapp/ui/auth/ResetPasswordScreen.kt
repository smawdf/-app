package com.myorderapp.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myorderapp.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun ResetPasswordScreen(
    deepLink: String,
    initialEmail: String = "",
    viewModel: AuthViewModel = koinViewModel(),
    onBackToLogin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(initialEmail) {
        if (initialEmail.isNotBlank()) {
            viewModel.onEmailChanged(initialEmail)
        }
    }

    LaunchedEffect(uiState.isPasswordResetComplete) {
        if (uiState.isPasswordResetComplete) {
            onBackToLogin()
        }
    }

    AuthDecoratedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ResetPasswordLogo()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "重置密码",
                style = MaterialTheme.typography.displayLarge,
                color = AuthPrimaryEnd,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "用邮箱验证码找回账号",
                style = MaterialTheme.typography.bodyMedium,
                color = AuthMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            AuthGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    AuthInputField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = "邮箱地址",
                        placeholder = "邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Outlined.Mail,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuthInputField(
                            value = if (deepLink.isBlank()) "" else "邮箱链接已验证",
                            onValueChange = {},
                            label = "验证码",
                            placeholder = "验证码",
                            modifier = Modifier.weight(1f),
                            leadingIcon = Icons.Outlined.Password,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Button(
                            onClick = { viewModel.sendPasswordResetEmail() },
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC7D6),
                                contentColor = AuthPrimaryEnd
                            ),
                            border = BorderStroke(2.dp, AuthPrimaryEnd.copy(alpha = 0.72f)),
                            modifier = Modifier.height(60.dp)
                        ) {
                            Text("获取验证码", fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthInputField(
                        value = password,
                        onValueChange = { password = it },
                        label = "新密码",
                        placeholder = "新密码",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
                        leadingIcon = Icons.Outlined.Lock,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthInputField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "确认新密码",
                        placeholder = "确认新密码",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
                        leadingIcon = Icons.Outlined.Lock,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            color = if (uiState.isPasswordResetComplete) AuthPrimaryEnd else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    AuthPrimaryButton(
                        text = if (uiState.isLoading) "正在重置..." else "重置密码",
                        onClick = { viewModel.resetPasswordFromDeepLink(deepLink, password, confirmPassword) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AuthBottomLink(
                prefix = "",
                actionText = "返回登录",
                onClick = onBackToLogin,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ResetPasswordLogo() {
    Surface(
        modifier = Modifier.size(112.dp),
        shape = CircleShape,
        color = Color(0xFFFFFCF8),
        border = BorderStroke(2.dp, AuthPrimaryEnd.copy(alpha = 0.62f))
    ) {
        Box(modifier = Modifier.padding(5.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.auth_dogs_artwork),
                contentDescription = "应用图标",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
