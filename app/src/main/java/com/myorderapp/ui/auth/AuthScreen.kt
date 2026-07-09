package com.myorderapp.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onLoggedIn: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = { viewModel.sendPasswordResetEmail() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoggedIn()
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
            AuthLogo()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "欢迎回来",
                style = MaterialTheme.typography.displayLarge,
                color = AuthInk,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "今天也一起好好吃饭吧",
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
                        label = "账号 / 邮箱",
                        placeholder = "账号 / 邮箱",
                        modifier = Modifier.fillMaxWidth(),
                        floatingLabel = false,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "邮箱账号可用于找回密码和验证切换设备。",
                        color = AuthMuted,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AuthInputField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = "密码",
                        placeholder = "密码",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
                        floatingLabel = false,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.onRememberCredentialsChanged(!uiState.rememberCredentials)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.rememberCredentials,
                                onCheckedChange = viewModel::onRememberCredentialsChanged,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AuthPrimaryEnd,
                                    uncheckedColor = AuthMuted.copy(alpha = 0.70f),
                                    checkmarkColor = Color(0xFFFFFBF5)
                                )
                            )
                            Text(
                                text = "记住账号密码",
                                color = AuthInk,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = onForgotPasswordClick,
                            enabled = !uiState.isLoading
                        ) {
                            Text(
                                text = "忘记密码？",
                                color = AuthPrimaryEnd,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    uiState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    AuthPrimaryButton(
                        text = if (uiState.isLoading) "登录中..." else "登录",
                        onClick = viewModel::submit,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.canSwitchDeviceByEmail) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = viewModel::sendDeviceSwitchEmail,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "发送邮箱验证，切换到当前设备",
                                color = AuthPrimaryEnd,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AuthBottomLink(
                prefix = "还没有账号？",
                actionText = "去注册",
                onClick = onRegisterClick,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
