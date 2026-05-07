# ProfileScreen 修复 · 需求文档

**日期**：2026-05-07
**状态**：待确认

---

## 问题分析

| # | 问题 | 根因 | 
|---|------|------|
| 1 | 无退出登录 | ProfileScreen 没有登出按钮。在线模式下底部只显示"在线模式"文本 |
| 2 | 菜品管理无响应 | `SettingsRow("📋", "菜品管理")` 没有传 `onClick` 参数，点了无反应 |
| 3 | 版本说明硬编码 | `"今天吃什么？v1.0"` 是写死的字符串 |

---

## 修改方案

### 1. 添加退出登录按钮
- 在"更多"区域增加一行 `SettingsRow("🚪", "退出登录")`
- 点击后弹出确认弹窗，确认后：调用 `AuthViewModel.logout()` 清除 session → 导航到 AuthScreen
- 仅在 `isSynced == true` 时显示

**改动**：
- `ProfileScreen.kt`：添加退出登录行 + 确认弹窗 + 新参数 `onLogoutClick`
- `NavGraph.kt`：ProfileScreen 路由增加 `onLogoutClick` 参数

### 2. 菜品管理跳转
- "菜品管理"点击后跳转到菜品库页面（`Routes.DISH_LIBRARY`）
- 新增 `onDishManageClick` 回调参数

**改动**：
- `ProfileScreen.kt`：SettingsRow 加 `onClick = onDishManageClick`
- `NavGraph.kt`：传入 `onDishManageClick = { navController.navigate(Routes.DISH_LIBRARY) }`

### 3. 版本号动态获取
- 从 `BuildConfig.VERSION_NAME` 或 `context.packageManager.getPackageInfo()` 获取真实版本号
- 显示格式：`"今天吃什么？v1.2.3"`

**改动**：
- `ProfileScreen.kt`：用 `LocalContext.current` 读取版本号

---

## 影响文件
- `ProfileScreen.kt` — 添加退出登录、菜品管理跳转、动态版本号
- `NavGraph.kt` — 传递新回调
- `ProfileViewModel.kt` — 可能不需要改动（logout 由 AuthViewModel 处理）

## 不改变
- 其他页面
- 数据模型
- 业务逻辑
