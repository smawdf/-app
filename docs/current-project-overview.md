# OrderDisk 当前项目画像

这份文档用于统一后续 Figma、UI 改造、代码实现和任务计划的事实基础。当前项目已经不是旧的“二人随机选菜工具”，也不是完整的“多商家外卖平台”，而是一个情侣点菜 App。

## 一句话定位

两个人打开 App，先登录或注册，然后进入情侣首页。首页有“饲养员”和“吃货”两个角色选择，并带纪念日功能；点菜页采用类美团的高效菜单体验；发现页只做搜菜，并把合适的菜加入“我的店铺”；我的页保存用户账号信息，并提供“我的店铺”编辑入口。

## 当前用户路径

1. 新用户进入 `OnboardingScreen` 注册，已有用户进入 `AuthScreen` 登录。
2. 登录后进入 `CoupleMenuScreen`，这是当前首页，包含“饲养员/吃货”角色选择和纪念日功能。
3. 进入 `OrderingScreen`，使用类美团菜单布局在“我的店铺”里点菜。
4. 在点餐页选择菜品、看详情、调整数量，购物车浮层实时汇总。
5. 进入 `CheckoutScreen`，选择/填写地址和备注后提交订单。
6. 在 `OrdersScreen` 和 `OrderDetailScreen` 查看历史订单。
7. 需要扩充店铺菜品时进入 `DiscoverScreen` 搜索菜品并加入“我的店铺”，或从 `ProfileScreen` 进入 `MenuManagementScreen` 编辑“我的店铺”。

## 当前页面地图

```text
MainActivity
  NavGraph
    HOME            -> CoupleMenuScreen
    ORDERING        -> OrderingScreen
    DISCOVER        -> DiscoverScreen
    ORDERS          -> OrdersScreen
    PROFILE         -> ProfileScreen
    ONBOARDING      -> OnboardingScreen
    AUTH            -> AuthScreen
    ANNIVERSARY     -> AnniversaryScreen
    SHOP_SETTINGS   -> MenuManagementScreen
    MENU_MANAGEMENT -> MenuManagementScreen
    CART            -> CartScreen
    CHECKOUT        -> CheckoutScreen
    ORDER_DETAIL    -> OrderDetailScreen
```

底部导航只出现在五个主 Tab：`HOME`、`ORDERING`、`DISCOVER`、`ORDERS`、`PROFILE`。

## 页面设计基准

- 首页应该延续上次画的情侣首页方向，核心是“饲养员/吃货”角色选择、纪念日和两个人的小饭桌氛围，而不是平台首页。
- 首页顶部关系区不展示 App 图标或未确定 App 名称，应展示左侧“当前用户”头像位、中间爱心、右侧“伴侣/邀请对方”头像位；当前用户头像下方显示当前角色。未绑定伴侣时右侧头像位是空心加号。
- 首页只应该保留一套当前情侣首页 Tab/内容结构，不允许同时出现旧首页 Tab 和新首页 Tab 两套入口或标签。
- 角色选择不是普通跳转按钮：选择“饲养员/吃货”后应保存当前用户身份，并更新当前用户头像下方的角色文本；当前用户固定在左侧，另一侧保留伴侣空位或伴侣头像。选择身份后不应直接跳到点菜页，除非已经完成身份保存并由明确按钮触发点菜。
- 点菜页可以类美团，强调分类、菜品、加购、购物车的效率，但语义应是“我的店铺”，不是附近商家。
- 发现页当前只做一件事：搜索菜品，并加入“我的店铺”。
- 我的页重点是用户账号信息，同时提供进入“我的店铺”编辑的入口。
- 我的店铺编辑页重点是分类、菜品、价格、库存、上下架。
- 登录注册页不要强行展示未确定的 App 名称，可以使用图标、欢迎语、双人饭桌氛围作为识别。
- 图标方向：小金毛 + 马尔济斯，小金毛戴厨师帽，整体简约，餐盘里可以是食物，但不要堆太多元素。

## 可复用功能参考

来自“熊家小灶”同类情侣点餐系统中，值得复用但需要按当前 App 方向改造的点：

- 下单通知：吃货点菜或提交订单后，饲养员可以收到提醒；优先做成 App 内通知/状态提醒，不急着做复杂推送。
- 伴侣账号/用户管理：围绕“邀请对方 / 绑定伴侣 / 解除绑定 / 显示双方身份”落地，不做传统后台用户管理。
- 余额/熊熊币：可以作为情侣趣味积分或投喂值，但不要先做真实充值体系；除非后续明确要收费或虚拟币闭环。
- 店铺与餐品管理：继续复用“我的店铺”概念，让饲养员维护分类、菜品、价格、库存和上下架。
- 美食记录：订单页里的“美食日记”方向可以保留，后续把已完成订单沉淀为共同吃饭记录。
- H5/扫码点餐：目前不是主线，只有在需要分享给伴侣或非 App 场景点菜时再考虑。

## 当前首页待修正问题

- 首页曾出现“旧 Tab + 新 Tab”两套标签/入口并存，后续实现必须删除旧入口，只保留当前情侣首页结构。
- “选择身份”当前语义不完整：点击后不应该直接跳转点菜页面，而应该完成身份选择、保存状态、更新左右头像位。
- 当前用户固定在左侧，不论选择“饲养员”还是“吃货”；右侧用于展示伴侣或邀请位。角色差异通过头像下方文本和对应功能入口表达。
- 没有伴侣或未绑定用户时，另一侧头像位保持空心加号，不能用假头像或旧图标顶替。

## 架构事实

- `MainActivity.kt`：单 Activity，Compose NavHost，启动页由 `SessionManager.isLoggedIn.value` 决定。
- `NavGraph.kt`：当前路由事实源。
- `BottomNavItem.kt`：当前主 Tab 事实源。
- `MyOrderApp.kt`：初始化 Koin 和 Coil 3 ImageLoader。
- `AppModule.kt`：Koin 业务注入模块。
- `NetworkModule.kt`：Retrofit、Moshi、SessionManager、外部图源和菜谱 API 注入模块。
- `AppDatabase.kt`：Room 数据库，当前版本 6。

## 数据层事实

- `SingleShopRepository`：当前“我的店铺”的单店抽象，同时绑定 `ShopRepository` 和 `MenuRepository`。
- `RoomMenuRepository`：“我的店铺”菜品持久化。
- `RoomCartRepository`：本地购物车持久化。
- `RoomAddressRepository`：本地地址持久化。
- `SupabaseOrderRepository`：订单保存与 Supabase 同步。
- `HybridDishRepository`：旧版菜品库的本地 + 云端混合仓储。
- `DiscoverViewModel`：聚合本地菜品、菜单菜品、Tian 菜谱搜索和 Spoonacular/TheMealDB 图源。

## 不能再误用的旧假设

- 不要把当前 App 画成多商家外卖市场。
- 不要默认存在附近商家、推荐商家、商家排行榜、骑手配送、平台补贴等外卖平台概念。
- 不要把 `home2/shop/merchant` 目录名直接理解成真实多商家业务，必须看当前路由和仓储绑定。
- 不要把历史重构计划中的“美团式”目标当成当前落地事实。
- Jisu/Juhe 旧菜谱接入已从源码移除，不要再基于这些旧入口新增功能。
- 不要在用户未确认 App 名称前，把 `OrderDisk` 做成用户可见主品牌。

## 已知技术债

- 部分源码可见文案存在乱码，需要后续统一修复为 UTF-8 中文。
- `SHOP_SETTINGS` 当前实际打开 `MenuManagementScreen`，命名和落地页面不完全一致。
- 旧版菜品库、心愿单、随机推荐、餐次等兼容模块仍在，后续需要决定保留、收口或重新整合。
- 历史文档中存在多商家/美团式描述，已经标为历史参考，不应作为新设计依据。
