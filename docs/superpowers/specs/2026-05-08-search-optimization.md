# 搜索 + 随机 + 多项修复 · 设计规范

**日期**：2026-05-08
**状态**：待确认

---

## 一、搜索架构：Supabase 优先 → API 兜底

```
搜索"牛肉"
  ├── 1. Supabase dishes 表 ILIKE 模糊搜索
  │     └── 有 ≥ 5 条 → 返回，不调 API
  │     └── < 5 条 → Juhe API → 结果 INSERT 到 Supabase
  └── 返回
```

---

## 二、随机选菜流程

```
用户触发"转一转"
  │
  ├── 1. 查 Supabase 菜品总数
  │     ├── < 20 道 → 从关键词池随机抽 3 个 → 调 Juhe API ×3
  │     │              → 全部 INSERT 到 Supabase
  │     └── ≥ 20 道 → 跳过 API
  │
  ├── 2. 应用筛选条件
  │     ├── 分类筛选（用户可自定义输入）
  │     ├── 烹饪时间范围
  │     ├── 难度等级
  │     └── 排除已展示过的菜品（不重复机制）
  │
  ├── 3. 从筛选结果中随机抽取 1 道（Kotlin Random）
  │
  ├── 4. 展示：菜名 + 图片 + 分类 + 时间 + 难度
  │
  └── 5. 记录已展示 (dishId → SharedPreferences)
         全部轮完一圈后自动重置
```

### 筛选条件 UI（随机页）

```
┌─────────────────────────────┐
│  🎲 随机推荐                │
│                             │
│  分类: [__自定义输入__]      │  ← 用户可打字，如"中餐""川菜"
│  时间: [__分钟以内__]       │  ← 可选
│  难度: ⭐ ~ ⭐⭐⭐⭐⭐      │  ← 可选
│                             │
│  ┌─── 转盘 ───┐             │
│  │            │             │
│  │    🍳     │             │
│  │            │             │
│  └────────────┘             │
│                             │
│  [🎯 转一转！]              │
│                             │
│  已随机 12 次，未重复 12 道  │
└─────────────────────────────┘
```

### 不重复机制

- `SharedPreferences` 存 `shown_dish_ids`（逗号分隔的 ID 列表）
- 每次随机排除已展示的
- 全部轮完 → 清空列表 → 重新开始一轮
- 用户可手动"重置已展示列表"

---

## 三、其他改动

| # | 改动 | 说明 |
|---|------|------|
| 移除 TheMealDB | 删除 4 文件 + 清理 DI | 只保留聚合数据 |
| 卡片展示图片 | Home/Library/Search | 有 imageUrl 用 AsyncImage |
| 关于页面 | 新建 AboutScreen | 版本号 + 更新日志 |
| 移除种类标签 | RandomScreen | 筛选改成用户自定义 |

---

## 四、改动文件

| 文件 | 操作 |
|------|------|
| `SupabaseApi.kt` | +searchDishes(keyword), +batchInsert |
| `SupabaseDishRepository.kt` | searchFromCloud, batchInsert |
| `DualRecipeSearchUseCase.kt` | 移除 TheMealDB, Supabase 优先 |
| `RandomViewModel.kt` | 新随机逻辑 + 筛选 + 不重复 |
| `RandomScreen.kt` | 自定义筛选 UI + 移除标签 |
| `HomeScreen.kt` | 卡片图片 |
| `DishLibraryScreen.kt` | 网格卡片图片 |
| `SearchScreen.kt` | 搜索结果卡片图片 |
| `AboutScreen.kt` | 新建 |
| `NavGraph.kt` | About 路由 |
| `ProfileScreen.kt` | 关于 onClick |
| TheMealDB* (4 files) | 删除 |
| `NetworkModule.kt` | 清理 |
| `AppModule.kt` | 清理 |
