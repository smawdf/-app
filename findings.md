# Findings

## 项目结构与现状

- 当前是单模块 Compose App，入口为 `MainActivity + NavGraph + Koin`。
- 当前底部导航是 `home / dish_library / meal / wishlist / profile`，不是外卖型信息架构。
- 当前核心域模型是 `Dish / Meal / MealItem / Profile / WishlistItem`。
- 当前 Room 主表是 `dishes / meals / meal_items / profiles / wishlist_items`。
- 当前 Supabase 主表与仓储也围绕上述旧模型展开。
- 当前搜索由 `SearchViewModel` 聚合 `DishRepository` 与 `TianRecipeRemoteDataSource`。

## 明确痛点

- 领域模型偏菜谱与双人决策，不适合直接支撑商家、购物车、订单。
- 外部搜索结果会被缓存回 `dishes` 表，污染主数据。
- ViewModel 直接依赖多个具体仓储实现，迁移成本较高。
- `RealtimeService` 仅服务 `meal_items`，实时能力没有转化成订单链路能力。
- 项目文档中提到的 `DualRecipeSearchUseCase` 在当前代码中不存在，说明文档与实现已漂移。

## 外部数据源结论

- 当前 TianAPI 集成无图片字段，不适合美团式图片卡片搜索。
- 极速数据官方文档明确展示了 `pic` 与 `process[].pic`，适合做中文图片菜谱源。
- Spoonacular 官方文档明确展示了搜索结果 `image` 字段，适合做增强搜索与食材/菜单补充。
- TheMealDB 官方文档提供缩略图能力，适合原型与开发期兜底。
- 聚合数据当前公开页面能确认有查询/分类/详情能力，但图片字段需二次验证。

## 2026-06-30 重构完成度检查

- Phase 2-4 重构尚未完全收口。`compileDebugKotlin` 通过，但首次 `testDebugUnitTest` 有 2 个失败。
- `OrderingHomeShellTest` 根因是 `MainActivity.kt` 的底部导航仍在 `Routes.HOME` 上显示；测试要求首页点餐壳自己占据底部区域。
- `VisibleChineseTextTest` 根因是 `OrderingScreen.kt` 可见文案里仍含英文单位 `km`，违反点餐流中文可见文案约束。
- 第二次全量测试发现 `OrderingScreenLayoutTest` 仍期待旧文案 `距离1.2km`，与中文可见文案测试冲突。根因是测试期望未随中文化文案同步。
- 修复后 `testDebugUnitTest`、`assembleDebug`、`assembleRelease` 均通过。技术验证角度可以认为 Phase 2-4 重构已收口；剩余工作是代码审阅、真机/模拟器手动走单和分批提交。
