# 配对确认通知 — 需求文档

> 版本：v1.0 | 状态：设计中 | 日期：2026-05-08

## 1. 当前问题

```
现有配对（单向无感知）：
  A 生成码 "ABC123"
  B 输入码 → B 本地保存 pairId = "ABC123" → 完成
  A：完全不知道有人配对了自己
  B：不知道配对是否真正生效
```

## 2. 优化目标

- 配对过程双向确认
- 被配对方实时收到通知
- 配对码可长按复制
- 配对成功后双方都能看到对方信息

## 3. 优化后流程

### 3.1 注册时配对

```
Step 3（注册完成后，可选）：
  A 生成配对码 → 显示 "将配对码发给对方" + 长按复制提示
                                        ↓
  B 注册时输入 A 的码 → 调用 joinPair(code) → 
  B 的 profile 写入 { pairId: "ABC123", userId: "B的ID" }
                                        ↓
       Supabase Realtime 通知 A：
       ┌──────────────────────────────────┐
       │  "XXX 想和你配对，是否接受？"       │
       │  [接受]  [拒绝]                    │
       └──────────────────────────────────┘
                    ↓ 接受
       A 的 profile 更新 pairId → 双方配对成功
       双方跳转首页，显示配对状态
```

### 3.2 个人中心配对

```
ProfileScreen → 配对管理：
  A 生成新配对码 → 长按复制
  B 输入码 → 验证配对
     ↓
  A 收到通知 → 确认 → 双方配对
```

### 3.3 长按复制

配对码显示区域支持长按：
- 长按 → 复制到剪贴板
- 复制后弹出 Snackbar "配对码已复制"

## 4. 技术方案

### 4.1 数据表变动

在 Supabase `profiles` 表新增字段：

```sql
ALTER TABLE profiles ADD COLUMN pair_status VARCHAR DEFAULT 'none';
-- 'none' | 'pending' | 'paired'
```

配对流程：

```
B 输入 A 的码 → B 的 profile:
  { pairId: "ABC123", pairStatus: "pending" }

A 的 profile 保持: { pairId: "00000000-...", pairStatus: "none" }

A 查询: SELECT * FROM profiles WHERE pairId = A的码 AND pairStatus = 'pending'
  → 找到 B 的配对请求

A 确认 → API 更新:
  UPDATE profiles SET pairId = "ABC123", pairStatus = "paired" WHERE userId = A
  UPDATE profiles SET pairStatus = "paired" WHERE userId = B

现在 A.pairId = B.pairId = "ABC123"，配对完成
```

### 4.2 实时通知（Supabase Realtime）

```kotlin
// 订阅自己 profile 的变化
val channel = supabaseClient.realtime.createChannel("pairing-${myUserId}")

// 监听其他人以我的码发起配对
channel.onPostgresChange(
    schema = "public",
    table = "profiles",
    filter = "pair_id=eq.${myCode} AND pair_status=eq.pending"
) { payload ->
    // 收到新配对请求 → 弹窗确认
}
```

备选方案（不用 Realtime）：
- ProfileScreen 显示时轮询检查
- 或收到配对请求后存本地标记，下次打开 App 弹窗

### 4.3 长按复制

```kotlin
var showCopiedTip by remember { mutableStateOf(false) }

Text(
    text = pairCode,
    modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                clipboardManager.setText(AnnotatedString(pairCode))
                showCopiedTip = true
            }
        )
    }
)
if (showCopiedTip) {
    Snackbar { Text("配对码已复制") }
}
```

## 5. UI 交互

### 5.1 配对请求弹窗

```
┌──────────────────────────────────┐
│          👫                       │
│                                   │
│   "XXX 想和你配对"                  │
│                                   │
│   配对后可实时同步点餐数据          │
│                                   │
│  ┌─────────┐ ┌─────────┐         │
│  │  接受 →  │ │  拒绝    │         │
│  └─────────┘ └─────────┘         │
└──────────────────────────────────┘
```

### 5.2 配对码展示（含长按提示）

```
┌─────────────────────────────┐
│       你的配对码              │
│                             │
│       A B C 1 2 3           │
│                             │
│   将此码分享给对方            │
│   💡 长按可复制配对码         │
└─────────────────────────────┘
```

## 6. 涉及文件

| 文件 | 改动 |
|------|------|
| `SupabaseProfileRepository.kt` | `joinPair()` 改为请求模式；新增 `acceptPair()` / `declinePair()` / 轮询检测 |
| `SupabaseApi.kt` | 新增配对请求查询 API |
| `OnboardingScreen.kt` | 配对码长按复制；配对请求弹窗 |
| `ProfileScreen.kt` | 配对码长按复制；配对请求检测 |
| `ProfileViewModel.kt` | 订阅配对请求状态 |
| `SessionManager.kt` | 新增 `pairStatus` 字段 |
| `Supabase` | profiles 表新增 `pair_status` 列 |

## 7. 待确认

- 实时通知优先用 Supabase Realtime 还是轮询？
- 如果 A 不在线，B 的配对请求是否保留？保留多久？
- 一个人能否同时收到多个配对请求？
