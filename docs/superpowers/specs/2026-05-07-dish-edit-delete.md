# 菜品修改/删除 · 需求文档

**日期**：2026-05-07
**状态**：待确认

---

## 现状

- `DishRepository.updateDish()` / `deleteDish()` **已实现**，Supabase + 本地文件都支持
- `DishDetailScreen` 只展示，没有编辑/删除入口
- 用户只能看菜、加菜，没法改和删

## 需求

### 1. 详情页加操作按钮

点击菜品 → DishDetailScreen 底部出现两个按钮：

| 按钮 | 颜色 | 行为 |
|------|------|------|
| ✏️ 编辑 | Primary 淡绿 | 跳转到编辑页（复用 AddDishScreen） |
| 🗑 删除 | Error 红 | 弹确认框 → 确认后删除 → 返回上页 |

### 2. 编辑：复用 AddDishScreen

- `AddDishScreen` 新增可选参数 `editDishId: String?`
- 有 `editDishId` 时：
  - 标题显示"编辑菜品"
  - 预填现有菜品的所有字段（名称/分类/图片/食材/步骤/备注等）
  - 保存按钮调用 `updateDish()` 而非 `addDish()`
- 保存成功 → 返回上一页（详情页自动刷新）

### 3. 删除：确认 + 同步

- 点击删除 → AlertDialog 确认
- 确认后调用 `dishRepository.deleteDish(dishId)`
- HybridDishRepository 自动路由：在线删 Supabase + 本地，离线删本地文件
- 删除成功 → pop 回上一页

---

## 改动范围

| 文件 | 改动 |
|------|------|
| `DishDetailScreen.kt` | 底部加编辑/删除按钮 + 删除确认弹窗 |
| `DishDetailViewModel.kt` | 加 `deleteDish()` 方法 |
| `AddDishScreen.kt` | 加 `editDishId` 参数，编辑模式预填数据 |
| `AddDishViewModel.kt` | 加 `loadDishForEdit()` / `updateDish()` 逻辑 |
| `NavGraph.kt` | `ADD_DISH` 路由加可选 `dishId` 参数 |

## 不改变

- DishRepository 接口
- 数据模型
- 菜品库列表页（DishLibraryScreen）
