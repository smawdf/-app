# 注册/登录流程优化 · 需求文档

**日期**：2026-05-07
**状态**：待确认

---

## 1. 当前流程分析

### 1.1 注册流程（现状）

```
OnboardingScreen                    ProfileSetupScreen
┌─────────────────────┐            ┌────────────────────┐
│ 🍽️ 今天吃什么？      │  注册成功   │ 🎉 完善你的资料     │
│                     │ ────────→  │                    │
│ 邮箱: [________]    │            │ [头像] 点击设置     │
│ 密码: [________]    │            │ [昵称] 输入框      │
│ 确认: [________]    │            │                    │
│ [注册]              │            │ [完成] [稍后设置]   │
│ 已有账号？去登录     │            └────────────────────┘
└─────────────────────┘
```

**问题**：
- 注册只收集邮箱+密码，头像和昵称在第二页
- 没有配对选项
- ProfileSetupScreen 的头像不能输 URL（仅有拍照/相册）

### 1.2 登录流程（现状）

```
AuthScreen                          HomeScreen
┌─────────────────────┐            ┌────────────────────┐
│ 🔑 登录/注册         │  登录成功   │ 首页                │
│                     │ ────────→  │                    │
│ 邮箱: [________]    │            │                    │
│ 密码: [________]    │            │                    │
│ [登录] / [注册]      │            │                    │
└─────────────────────┘            └────────────────────┘
```

**问题**：
- 邮箱/密码不记忆，每次要重新输入
- 未注册邮箱点登录时，Supabase 返回错误被 catch-all 吃掉，只显示通用错误（如"请求失败"），用户不知道是"未注册"还是"密码错误"

### 1.3 会话保持（现状）

`SessionManager` 使用 `SharedPreferences` 持久化：
- JWT token → 用于 API 鉴权
- userId → 用户标识
- pairId → 配对关系

**未存储**：邮箱、密码、昵称、头像 URL

---

## 2. 需求

### 需求 A：注册流程整合头像 + 昵称 + 配对选项

**描述**：将 OnboardingScreen 和 ProfileSetupScreen 合并为一个流程，在注册时一次性完成：

**页面 1 — 创建账号（OnboardingScreen 改造）**：
- 邮箱输入框
- 密码输入框
- 确认密码输入框
- [下一步] 按钮 → 校验输入 → 进入页面 2

**页面 2 — 设置个人资料（新增）**：
- 头像设置：拍照 / 相册 / 输入 URL 三选一
- 昵称输入框（必填，最多12字）
- [上一步] [完成注册] 按钮
- 点击"完成注册" → 调用 Supabase signUp → 创建 Profile → 进入页面 3

**页面 3 — 配对选项（新增）**：
- "现在配对" or "稍后配对" 选项
- 选择"现在配对"：
  - 展示生成的配对码（6位大写）
  - 或输入对方的配对码
  - [完成] 按钮
- 选择"稍后配对"：直接进入首页

**影响文件**：
- `OnboardingScreen.kt` — 拆分为多步骤
- `OnboardingViewModel.kt` — 增加个人资料和配对状态
- `ProfileSetupScreen.kt` — 合并进来或废弃
- `NavGraph.kt` — 调整路由

### 需求 B：老用户记住账号

**描述**：登录成功后，用 `SharedPreferences` 保存邮箱。下次打开登录页自动填入。

**实现**：
- `SessionManager` 增加 `saveEmail(email)` / `getSavedEmail()` 方法
- `AuthScreen` 初始化时调用 `sessionManager.getSavedEmail()` 预填邮箱
- 登录成功时调用 `sessionManager.saveEmail(email)`
- **不存储密码**（安全考虑：明文密码不安全，应使用 token 自动登录）

**影响文件**：
- `SessionManager.kt` — 新增 email 存取
- `AuthScreen.kt` / `AuthViewModel.kt` — 初始化时预填

### 需求 C：未注册登录提示

**描述**：用未注册邮箱登录时，显示明确的"该邮箱尚未注册，请先创建账号"。

**现状问题**：`AuthViewModel.submit()` 的 catch 块统一返回 `e.message?.take(100) ?: "请求失败"`，用户无法区分错误类型。

**实现**：
```kotlin
// 在 AuthViewModel.submit() 的 catch 块中
catch (e: Exception) {
    val msg = e.message ?: ""
    val errorMsg = when {
        msg.contains("Invalid login credentials", ignoreCase = true) ||
        msg.contains("User not found", ignoreCase = true) ->
            "该邮箱尚未注册，请先创建账号"
        msg.contains("Invalid email or password", ignoreCase = true) ||
        msg.contains("email not confirmed", ignoreCase = true) ->
            "邮箱未验证，请检查邮箱确认链接"
        msg.contains("already registered", ignoreCase = true) ->
            "该邮箱已注册，请直接登录"
        else -> msg.ifBlank { "请求失败，请稍后重试" }.take(100)
    }
    _uiState.value = _uiState.value.copy(
        isLoading = false,
        errorMessage = errorMsg
    )
}
```

**影响文件**：
- `AuthViewModel.kt` — 细化错误处理
- `OnboardingViewModel.kt` — 同样细化（已有部分处理）

### 需求 D：Auto-Login（附加优化）

**描述**：App 启动时如果已保存有效 session，直接进入首页，不显示登录/注册页。当前已实现（`MainScreen` 中 `startDestination` 根据 `sessionManager.isLoggedIn` 判断）。

**无需改动**。

---

## 3. 实现优先级

| 优先级 | 需求 | 复杂度 | 说明 |
|--------|------|--------|------|
| P0 | C：未注册登录提示 | 低 | 改几行 catch 逻辑即可 |
| P0 | B：记住账号 | 低 | SharedPreferences 加一个 key |
| P1 | A：注册流程整合 | 中 | 拆 OnboardingScreen 为多步 |
| P2 | A中配对选项 | 中 | 复用 ProfileScreen 的配对码 UI |

## 4. 不改变

- Supabase 鉴权方式（signUp/signIn API）
- 数据模型（Profile, PairInfo 等）
- Koin DI 配置
- 首页和底部导航
