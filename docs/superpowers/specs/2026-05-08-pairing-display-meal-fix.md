# 配对展示 + 点菜修复 — 需求文档

> 版本：v1.0 | 状态：设计中 | 日期：2026-05-08

## 1. 当前问题分析

### 1.1 配对展示

```
现在：ProfileScreen 仅显示文字 "已配对 · XXX"
     无对方头像、无配对动画、无爱心设计
```

### 1.2 点菜 Bug

```
现在：提交菜品后 → removeMyDish() 可删除
     → mySubmitted 仍为 true
     → 我方区域显示"已提交"但菜品已空
     → 矛盾状态
```

### 1.3 点菜布局

```
现在：垂直排列（我的选择 → 对方选择 → 菜品列表）
     不是左右并排双人布局
```

### 1.4 解除配对

```
现在：unpair() 仅清除本地 pairId
     对方仍显示已配对
     没有双向同步
```

---

## 2. 优化设计

### 2.1 配对完成爱心动画

```
配对确认 → 全屏爱心包裹动画（2秒）
         → 左右滑入双方头像 + 昵称
         → 爱心包裹旋转一周
         → 帧动画结束后显示主页
```

### 2.2 个人中心配对展示

```
┌──────────────────────────────────┐
│        👤 我的头像         👤 对方头像   │
│       我的昵称           对方昵称    │
│              💚 已配对              │
│          ┌──────────────┐          │
│          │   解除配对    │          │
│          └──────────────┘          │
└──────────────────────────────────┘
```

双方头像用爱心形状包裹（Clip 为心形），中间绿色连线或爱心图标。

### 2.3 点菜双人布局

```
┌──────────┬──────────┐
│   我      │   对方    │
│  (左侧)   │  (右侧)   │
│          │          │
│ 已选菜品  │  已选菜品  │
│ 🍗 宫保鸡丁│ 🍜 牛肉面  │
│ 🥬 炒白菜 │ 🍚 白米饭  │
│          │          │
│ 已提交 ✓  │ 未提交    │
└──────────┴──────────┘
┌──────────────────────┐
│    菜品库（可选）    │
└──────────────────────┘
```

### 2.4 点菜删除后重置提交状态

```
用户提交了 3 道菜 → "已提交 ✓"
用户又删了 1 道 → 我的选择变了
                → mySubmitted 应重置为 false
                → 用户可以重新提交
```

修复：
```kotlin
fun removeMyDish(dishId: String) {
    // 正常删除
    _uiState.value = _uiState.value.copy(
        mySelections = _uiState.value.mySelections.filter { it.id != dishId },
        mySubmitted = false  // 删除后重置提交状态
    )
}

### 2.5 解除配对双向同步

```
A 点击解除 → A 的 profile: pairId = "00000000-...", pairStatus = "none"
          → A 本地清除
          → 云端更新（如果在线）
          → B 下次拉取 profile 时发现 pairId 不匹配
          → B 也自动解除
          → B 的 pairId 也清除
```

---

## 3. 技术方案

### 3.1 爱心动画

```kotlin
// 配对完成时显示爱心包裹动画
@Composable
fun PairingHeartAnimation(
    myAvatar: String?,
    partnerAvatar: String?,
    myName: String,
    partnerName: String,
    onAnimationEnd: () -> Unit
) {
    // 使用 Animatable 实现：
    // 1. 爱心从小到大缩放 + 旋转（1.5s）
    // 2. 两个头像从两侧滑入（0.5s）
    // 3. 昵称渐入
    // 4. 停留 1s 后回调 onAnimationEnd
}
```

### 3.2 心形裁剪

```kotlin
// 头像用爱心形状裁剪
Box(
    modifier = Modifier
        .size(80.dp)
        .clip(HeartShape)  // 自定义 Shape
        .background(Color.Red)
) {
    AsyncImage(...)
}
```

### 3.3 配对状态同步

profiles 表新增字段：
```sql
ALTER TABLE profiles ADD COLUMN pair_status VARCHAR DEFAULT 'none';
-- 'none' | 'paired'
```

配对时：
- B 接受 → B.pairId = code, B.pairStatus = 'paired'
- A 接受 → A.pairId = code, A.pairStatus = 'paired'

解除时：
- 任一方解除 → 自己的 pairId = sentinel, pairStatus = 'none'
- 对方检测到 pairId 不匹配 → 自动解除

---

## 4. 涉及文件

| 文件 | 改动 |
|------|------|
| `ProfileScreen.kt` | 配对展示改为双人头像 + 爱心设计；解除配对确认弹窗 |
| `ProfileViewModel.kt` | 新增 partnerProfile 获取、解除同步逻辑 |
| `StartMealScreen.kt` | 双人左右布局；已提交后删除按钮隐藏 |
| `MealViewModel.kt` | `removeMyDish` 加已提交守卫 |
| `OnboardingScreen.kt` | 配对完成时爱心动画 |
| `SupabaseProfileRepository.kt` | `unpair` 改为双向同步 |
| `SupabaseApi.kt` | 新增查询对方 profile API |

---

## 5. 实现优先级

| 优先级 | 内容 |
|--------|------|
| P0 | 点菜删除后重置提交状态 Bug 修复 |
| P0 | 点菜双人左右布局 |
| P1 | 配对爱心展示（个人中心） |
| P1 | 解除配对双向同步 |
| P2 | 配对完成爱心动画 |
