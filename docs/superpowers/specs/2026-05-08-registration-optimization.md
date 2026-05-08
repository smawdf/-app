# 注册环节优化 — 需求文档

> 版本：v2.0 | 状态：设计中 | 日期：2026-05-08

## 1. 优化目标

简化注册流程，减少用户流失，提升注册成功率。

---

## 2. 当前流程（v1.2.0）

```
Step 1: 邮箱 + 密码 + 确认密码（仅客户端校验）
  ↓
Step 2: 头像(可选) + 昵称(必填) → "完成注册" 才调注册 API
  ① signUp API
  ② createProfile
  ③ setSession
  ④ 三路云端同步（串行阻塞）
  ↓
Step 3: 配对（必经，可跳过）
  ↓
首页
```

**问题**：
- 注册 API 在 Step 2 才调用，用户不知道邮箱是否已被注册
- 云端同步串行阻塞，注册完成后卡住
- 配对放在注册流程中，拖长流程
- URL 输入框冗余

---

## 3. 优化后流程

```
Step 1: 注册方式选择
  ┌────────────────┐  ┌──────────────────┐
  │  📧 邮箱注册     │  │  📱 手机号注册 ▸   │
  │                │  │  (待完成)         │
  └────────────────┘  └──────────────────┘
         ↓
Step 2: 邮箱 + 密码 + 确认密码
  校验：
  ① 邮箱/密码非空
  ② 邮箱格式合法（含 @ 和域名）
  ③ 密码 ≥ 6 位
  ④ 两次密码一致
         ↓
Step 3: 头像 + 昵称 + 完成注册
  - 头像：拍照 / 从相册选择（去掉 URL 输入）
  - 昵称：可跳过，默认 "食客"
  - 点击 "完成注册"：
    ① signUp API
    ② createProfile
    ③ setSession + saveEmail
    ④ 云端同步改为后台异步（不阻塞 UI）
    ⑤ registrationComplete = true
         ↓
首页（配对移除，移到个人中心）
```

---

## 4. 配对后置

- 注册完成后直接进入首页
- 配对入口移至 **个人中心 → 配对管理**
- 顶部 banner 提示未配对状态
- 可随时配对或跳过

---

## 5. 需要改动的文件

| 文件 | 改动 |
|------|------|
| `OnboardingScreen.kt` | 新增 Step 选择注册方式，Step 2 去掉 URL 输入，Step 3 昵称可跳过，删除配对步骤 |
| `OnboardingViewModel.kt` | 新增邮箱格式校验，昵称默认值，云端同步改后台 |
| `DishLibraryScreen.kt` | "上传展示图"中的"输入URL"也要删除（与 AddDishScreen 同步） |
| `ProfileScreen.kt` | 增强配对入口（从注册流程移入后需要更明显） |

---

## 6. 邮箱格式校验

```kotlin
private fun isValidEmail(email: String): Boolean {
    return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
}
```

---

## 7. 云端同步异步化

```kotlin
// 注册成功后，同步放后台
viewModelScope.launch(Dispatchers.IO) {
    dishRepo.syncFromCloud()
    profileRepo.loadFromCloud()
    mealRepo.syncFromCloud()
}
// 不等待，直接完成注册
```

---

## 8. 邮箱验证码

### 8.1 验证链接模式（推荐，Supabase 原生）

注册 → Supabase 自动发邮件 → 用户点链接验证 → 邮箱确认 → 可登录

**底层 API 流程：**

```
① 用户注册
  POST /auth/v1/signup
  Body: { email: "user@example.com", password: "123456" }
  Header: apikey: <anon_key>
  ↓
  Supabase 创建用户（confirmed_at = null）
  自动发送验证邮件到用户邮箱
  ↓
  返回: { user: { id, email, confirmed_at: null }, session: null }
  
② 邮件内容
  发件人: no-reply@supabase.co（可自定义）
  主题: Confirm your signup
  正文: 含验证链接
  https://<project>.supabase.co/auth/v1/verify?token=<one_time_token>&type=signup

③ 用户点击链接 → 浏览器打开 → Supabase 验证 token
  GET /auth/v1/verify?token=<token>&type=signup
  ↓
  Supabase 设置 confirmed_at = 当前时间
  重定向到配置的 redirect URL
  ↓
  邮箱验证完成

④ 用户登录
  POST /auth/v1/token?grant_type=password
  ↓
  返回: { access_token, user: { confirmed_at: "2026-05-08T..." } }
```

**Supabase Dashboard 配置：**

| 设置 | 值 |
|------|-----|
| Auth → Providers → Email | Confirm email = ✅ |
| Auth → Email Templates → Confirm signup | 自定义标题/正文/跳转链接 |
| Auth → URL Configuration → Site URL | `https://dwncdcwsbgbouoemfvwt.supabase.co` |
| Auth → URL Configuration → Redirect URLs | `myorderapp://auth/callback` |

**App 侧改动：**
- signUp 返回后提示"请查收验证邮件"
- 用户点邮件链接 → 浏览器验证 → 自动跳回 App（通过 deep link）
- 或用户手动返回 App 登录
- 登录时检查 `confirmed_at`，未验证则提示

### 8.2 6 位验证码模式（需自建）

> 状态：⛔ 待实现，优先级低

流程：注册 → 输入邮箱 → 收到 6 位码 → 输入 → 验证通过

需要：
1. Supabase Edge Function 生成随机码 + 存储
2. Resend / SendGrid 发邮件
3. Edge Function 验证码校验
4. 客户端 60s 倒计时

开发成本高，不推荐一期实现。

---

## 9. 手机号注册（未完成）

> 状态：⛔ 待实现

底层逻辑：
```
POST /auth/v1/otp  → 发送短信验证码（需 Twilio）
POST /auth/v1/verify → 验证码校验 + 自动注册/登录
```

前置条件：Supabase Dashboard 启用 Phone Auth + 配置 Twilio 账号。

待 Twilio 配置完成后实现。
