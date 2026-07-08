package com.myorderapp.ui.auth

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRememberCredentialsSourceTest {

    @Test
    fun `login screen can remember account and password`() {
        val screen = readMainSource("ui/auth/AuthScreen.kt")
        val authVisuals = readMainSource("ui/auth/AuthVisuals.kt")
        val viewModel = readMainSource("ui/auth/AuthViewModel.kt")
        val sessionManager = readMainSource("data/remote/supabase/SessionManager.kt")

        listOf(
            "记住账号密码",
            "Checkbox",
            "uiState.rememberCredentials",
            "viewModel.onRememberCredentialsChanged",
            "账号 / 邮箱 / 手机号",
            "placeholder = \"账号 / 邮箱 / 手机号\"",
            "placeholder = \"密码\"",
            "floatingLabel = false",
            "Spacer(modifier = Modifier.height(20.dp))",
            "Spacer(modifier = Modifier.height(18.dp))",
            "AuthPrimaryButton(",
            "忘记密码？",
            "还没有账号？",
            "去注册"
        ).forEach { expected ->
            assertTrue("登录页缺少记住账号密码入口：$expected", screen.contains(expected))
        }

        listOf(
            "AuthGlassCard",
            "AuthLogo",
            "height(if (supportingText == null) 60.dp else 82.dp)",
            "Color(0xFFFEF8F2)",
            "border = BorderStroke(1.dp, AuthFieldStroke.copy(alpha = 0.72f))",
            "containerColor = Color(0xFFFF9FB7)",
            "Icons.Outlined.Visibility",
            "Icons.Outlined.VisibilityOff",
            "floatingLabel: Boolean = true",
            "label = if (floatingLabel)",
            "ic_launcher_orderdisk_dogs_cropped"
        ).forEach { expected ->
            assertTrue("登录注册页缺少 Stitch 认证视觉：$expected", authVisuals.contains(expected))
        }

        listOf(
            "rememberCredentials: Boolean = false",
            "session.isRememberCredentialsEnabled()",
            "session.getSavedPassword()",
            "onRememberCredentialsChanged",
            "session.saveRememberedCredentials("
        ).forEach { expected ->
            assertTrue("登录状态缺少记住账号密码逻辑：$expected", viewModel.contains(expected))
        }

        listOf(
            "saveRememberedCredentials",
            "remember_credentials",
            "saved_password",
            "getSavedPassword",
            "isRememberCredentialsEnabled"
        ).forEach { expected ->
            assertTrue("SessionManager 缺少本地凭据持久化：$expected", sessionManager.contains(expected))
        }
    }

    @Test
    fun `register flow tells user when account already exists`() {
        val authViewModel = readMainSource("ui/auth/AuthViewModel.kt")
        val onboardingViewModel = readMainSource("ui/onboarding/OnboardingViewModel.kt")
        val onboardingScreen = readMainSource("ui/onboarding/OnboardingScreen.kt")
        val authVisuals = readMainSource("ui/auth/AuthVisuals.kt")

        listOf(
            "账号已存在，请直接登录",
            "isAccountAlreadyExistsMessage",
            "user_already_exists",
            "email_exists"
        ).forEach { expected ->
            assertTrue("登录注册流程缺少账号已存在提示映射：$expected", authViewModel.contains(expected))
            assertTrue("新用户注册流程缺少账号已存在提示映射：$expected", onboardingViewModel.contains(expected))
        }

        listOf(
            "创建你们的小饭桌",
            "一起记录每一次想吃什么",
            "账号/邮箱/手机号",
            "从相册选择头像",
            "开启甜蜜点菜之旅",
            "DashedAvatarPlaceholder",
            "PathEffect.dashPathEffect",
            "Icons.Filled.Add",
            "AuthPrimaryButton(",
            "text = if (uiState.isLoading) \"请稍候...\" else \"下一步\"",
            "verticalArrangement = Arrangement.spacedBy(26.dp)",
            "MaterialTheme.typography.titleSmall"
        ).forEach { expected ->
            assertTrue("注册页缺少 Stitch 认证文案：$expected", onboardingScreen.contains(expected))
        }

        val registerBody = functionBody(onboardingScreen, "RegisterAccountScreen")
        val step2Body = functionBody(onboardingScreen, "Step2Profile")
        assertTrue("旧的步骤胶囊组件不应继续保留在认证视觉代码里", !authVisuals.contains("fun AuthStepPill"))
        assertTrue("注册第 1 步主按钮应放在玻璃表单卡内部", registerBody.indexOf("AuthPrimaryButton(") < registerBody.indexOf("AuthBottomLink("))
        assertTrue("注册第 1 步不应显示原型没有的步骤胶囊卡", !registerBody.contains("AuthStepPill"))
        assertTrue("注册第 1 步应保留原型右上角淡狗狗插画", registerBody.contains("ic_launcher_orderdisk_dogs_cropped"))
        assertTrue("注册第 2 步应保持 page_9 的无卡片任务流", !step2Body.contains("AuthGlassCard"))
        assertTrue("注册第 2 步应保留 page_9 的淡心形装饰", onboardingScreen.contains("Icons.Outlined.FavoriteBorder"))
        assertTrue("注册第 2 步不应再叠加额外 32dp 内边距", !step2Body.contains(".padding(32.dp)"))

        val goToStep2Body = functionBody(onboardingViewModel, "goToStep2")
        val createAccountBody = functionBody(onboardingViewModel, "createAccountBeforeProfile")
        val completeRegistrationBody = functionBody(onboardingViewModel, "completeRegistration")
        assertTrue("注册第一步进入资料页前就应该创建账号", goToStep2Body.contains("createAccountBeforeProfile(state)"))
        assertTrue("账号创建应发生在资料页之前", createAccountBody.contains("client.auth.signUpWith(Email)"))
        assertTrue("账号创建应读取 signUp 返回值辅助判断", createAccountBody.contains("val signUpResult = client.auth.signUpWith(Email)"))
        assertTrue("账号创建应使用 signUpResult.id 作为兜底 userId", createAccountBody.contains("user?.id ?: signUpResult?.id.orEmpty()"))
        assertTrue("账号重复提示应发生在资料页之前", createAccountBody.contains("账号已存在，请直接登录"))
        assertTrue("无可用 token 的注册返回不应再提示注册失败", !createAccountBody.contains("注册失败：请稍后重试"))
        assertTrue("账号创建成功后才进入资料步骤", createAccountBody.contains("step = 2"))
        assertTrue("已创建账号返回第一步后不应重复注册", onboardingViewModel.contains("accountCreated"))
        assertTrue("资料步骤只应保存资料并完成注册", completeRegistrationBody.contains("saveProfileAndFinishRegistration()"))
        assertTrue("资料保存阶段不应再次创建账号", !functionBody(onboardingViewModel, "saveProfileAndFinishRegistration").contains("signUpWith"))
    }

    @Test
    fun `forgot password flow sends reset email and opens reset page by deep link`() {
        val screen = readMainSource("ui/auth/AuthScreen.kt")
        val viewModel = readMainSource("ui/auth/AuthViewModel.kt")
        val resetScreen = readMainSource("ui/auth/ResetPasswordScreen.kt")
        val navGraph = readMainSource("ui/navigation/NavGraph.kt")
        val manifest = readMainResource("AndroidManifest.xml")

        listOf(
            "忘记密码？",
            "onForgotPasswordClick",
            "onClick = onForgotPasswordClick"
        ).forEach { expected ->
            assertTrue("登录页缺少忘记密码入口：$expected", screen.contains(expected))
        }

        listOf(
            "resetPasswordForEmail",
            "PASSWORD_RESET_REDIRECT_URL",
            "orderdisk://auth/reset-password",
            "resetPasswordFromDeepLink",
            "parseSessionFromUrl",
            "modifyUser",
            "密码已修改，请重新登录"
        ).forEach { expected ->
            assertTrue("AuthViewModel 缺少邮箱重置密码能力：$expected", viewModel.contains(expected))
        }

        listOf(
            "邮箱地址",
            "验证码",
            "邮箱链接已验证",
            "获取验证码",
            "用邮箱验证码找回账号",
            "新密码",
            "确认新密码",
            "重置密码",
            "正在重置...",
            "ResetPasswordScreen",
            "ResetPasswordLogo",
            "shape = CircleShape",
            "Icons.Outlined.Mail",
            "Icons.Outlined.Password",
            "ButtonDefaults.buttonColors"
        ).forEach { expected ->
            assertTrue("重置密码页缺少必要 UI：$expected", resetScreen.contains(expected))
        }
        val resetBody = functionBody(resetScreen, "ResetPasswordScreen")
        assertTrue("重置密码主按钮应放在玻璃表单卡内部", resetBody.indexOf("AuthPrimaryButton(") < resetBody.indexOf("AuthBottomLink("))
        assertTrue("重置密码页底部应只保留返回登录", resetScreen.contains("prefix = \"\""))

        assertTrue("导航缺少重置密码路由", navGraph.contains("RESET_PASSWORD"))
        assertTrue("导航缺少 resetPassword deep link route builder", navGraph.contains("fun resetPassword(deepLink: String)"))
        assertTrue("登录页忘记密码应进入重置密码页", navGraph.contains("onForgotPasswordClick = {\n                    navController.navigate(Routes.resetPassword(\"\"))"))
        assertTrue("Manifest 缺少重置密码 Deep Link scheme", manifest.contains("android:scheme=\"orderdisk\""))
        assertTrue("Manifest 缺少重置密码 Deep Link host", manifest.contains("android:host=\"auth\""))
        assertTrue("Manifest 缺少重置密码 Deep Link path", manifest.contains("android:path=\"/reset-password\""))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main/java/com/myorderapp").resolve(relativePath),
            Paths.get("app/src/main/java/com/myorderapp").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found: $relativePath")
        return Files.readString(sourcePath)
    }

    private fun readMainResource(relativePath: String): String {
        val candidates = listOf(
            Paths.get("src/main").resolve(relativePath),
            Paths.get("app/src/main").resolve(relativePath)
        )
        val sourcePath = candidates.firstOrNull { Files.exists(it) }
            ?: error("Main resource not found: $relativePath")
        return Files.readString(sourcePath)
    }

    private fun functionBody(source: String, functionName: String): String {
        val start = source.indexOf("fun $functionName")
        require(start >= 0) { "Function not found: $functionName" }
        val bodyStart = functionBodyStart(source, start, functionName)
        require(bodyStart >= 0) { "Function body not found: $functionName" }
        var depth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(bodyStart + 1, index)
                }
            }
        }
        error("Function body not closed: $functionName")
    }

    private fun functionBodyStart(source: String, start: Int, functionName: String): Int {
        val paramsStart = source.indexOf('(', start)
        require(paramsStart >= 0) { "Function params not found: $functionName" }
        var depth = 0
        for (index in paramsStart until source.length) {
            when (source[index]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) {
                        return source.indexOf('{', index)
                    }
                }
            }
        }
        return -1
    }
}
