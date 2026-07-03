# Realtime Setup 状态说明

> 当前状态：历史文档，旧餐次实时同步已经从运行时移除。

旧版项目曾计划通过 `RealtimeService` 订阅 `meal_items` 表，为配对用户实时同步餐次菜品变化。当前产品主线已经调整为：

```text
情侣首页 -> 我的店铺点菜 -> 购物车 -> 结算 -> 订单 / 订单详情
```

因此，旧的 `RealtimeService`、`MealRepository`、`SupabaseMealRepository`、`InMemoryMealRepository` 和心愿单仓储已经从运行时依赖中删除。当前订单协作依赖：

- 首页未完成订单提醒。
- 订单列表与订单详情。
- 饲养员角色推进订单状态。
- 已完成订单沉淀到美食日记。

## 后续如果重新做实时能力

不要恢复旧 `meal_items` 餐次实时方案。应围绕当前主线重新设计：

- 订单状态实时更新：监听 `orders` 表。
- 订单项实时更新：监听 `order_items` 表。
- 伴侣在线状态：独立 presence 或 profile/session 状态，不复用旧餐次同步。
- 通知：优先 App 内提醒，再扩展系统通知或推送。

恢复实时能力前，需要先更新 PRD、数据库事件模型、Koin 注入和 ViewModel 数据流。
