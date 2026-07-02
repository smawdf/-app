# AGENTS.md

@C:\Users\Administrator\.codex\RTK.md

--- project-doc ---

# OrderDisk 当前项目说明

OrderDisk 当前是一个 Android 情侣点菜 App。当前源码的事实主线不是“多商家外卖市场”，而是“登录/注册 -> 情侣首页 -> 类美团点菜 -> 购物车 -> 结算 -> 订单”的点菜流程，并通过发现页搜索菜品，把结果加入“我的店铺”。

后续做 UI、Figma、代码或文档时，以当前源码为准，不要沿用旧文档中的“美团式多商家/附近商家/推荐商家流”假设，除非用户明确重新改变产品方向。

## 常用命令

```powershell
rtk .\gradlew.bat compileDebugKotlin
rtk .\gradlew.bat testDebugUnitTest
rtk .\gradlew.bat assembleDebug
rtk .\gradlew.bat assembleRelease
```

Debug APK 输出到 `app/build/outputs/apk/debug/`，Release APK 使用 `orderdisk.jks` 签名。Codex/Windows 本地执行命令时必须使用 `rtk` 前缀。

## 当前产品结构

- 登录/注册：`AuthScreen` 和 `OnboardingScreen`，注册包含密码与确认密码、资料/头像步骤。
- 首页：`CoupleMenuScreen`，是上次确定的情侣首页方向，包含两个人的角色选择：一方是“饲养员”，一方是“吃货”，并包含纪念日功能。
- 点菜：`OrderingScreen`，是类美团的点菜页面，但业务语义是进入自己的“我的店铺”点菜，不是浏览外部商家列表。主体是店铺信息、分类栏、菜品列表、菜品详情、购物车浮层。
- 发现：`DiscoverScreen`，当前只承担一个核心功能：搜索菜品，并把合适的菜品加入“我的店铺”。
- 我的店铺编辑：`MenuManagementScreen`，维护“我的店铺”的分类、菜品、价格、库存等。
- 我的：`ProfileScreen`，保存/展示用户账号信息，并提供进入“我的店铺”编辑的入口。
- 购物车/结算/订单：`CartScreen`、`CheckoutScreen`、`OrdersScreen`、`OrderDetailScreen`，本地优先保存，登录后可同步订单。
- 辅助页：`AnniversaryScreen`。
- 兼容模块：旧版菜品库、随机推荐、心愿单、餐次等能力仍在源码中，但不应作为新主流程或视觉设计的默认依据。

## 当前导航

主 Tab 来自 `BottomNavItem`：

- `HOME` -> `CoupleMenuScreen`
- `ORDERING` -> `OrderingScreen`
- `DISCOVER` -> `DiscoverScreen`
- `ORDERS` -> `OrdersScreen`
- `PROFILE` -> `ProfileScreen`

其他路由包括 `ONBOARDING`、`AUTH`、`ANNIVERSARY`、`SHOP_SETTINGS`、`MENU_MANAGEMENT`、`CART`、`CHECKOUT`、`ORDER_DETAIL`。当前 `SHOP_SETTINGS` 实际映射到 `MenuManagementScreen`。

## 技术栈与架构

单 Android 模块，包名 `com.myorderapp`。

```text
data/    Room、本地 assets、Retrofit API、Supabase SDK、Repository 实现
domain/  Dish、Meal、Profile、Wishlist、Shop、MenuItem、Cart、Address、OrderRecord 等模型和仓储接口
ui/      Compose 页面、ViewModel、导航、主题和组件
di/      Koin 注入模块：AppModule.kt、NetworkModule.kt
```

主要技术：

- Jetpack Compose + Material 3 + Navigation Compose
- Koin，非 Hilt
- Room，当前 `AppDatabase` 版本为 6
- Retrofit + Moshi
- official Supabase Kotlin SDK
- Coil 3
- Paging 3

## 数据与仓储事实

- `SingleShopRepository` 同时实现 `ShopRepository` 和 `MenuRepository`，当前是“我的店铺”的单店抽象，不是多商家市场。
- `RoomMenuRepository` 维护“我的店铺”的菜品。
- `RoomCartRepository`、`RoomAddressRepository` 维护本地购物车和地址。
- `SupabaseOrderRepository` 负责订单仓储：本地 Room 保存为主，登录态下同步/读取 Supabase。
- `HybridDishRepository` 合并 Room 本地菜品与 Supabase 云端菜品，主要服务旧版菜品库兼容能力。
- `SupabaseProfileRepository`、`SupabaseMealRepository`、`SupabaseDishRepository` 仍通过 `SupabaseClientProvider` 访问 Supabase。
- 当前网络模块接入 Tian 菜谱 API，以及 Spoonacular、TheMealDB 图源；Jisu/Juhe 旧菜谱接入已移除，不要再新增或恢复这些旧入口。

## 设计与实现注意事项

- 不要在未确认 App 名称前把 `OrderDisk` 作为用户界面主品牌文案。
- 视觉方向应围绕“两只小狗 + 情侣点菜 + 我的店铺 + 温暖点餐”，不要画成外卖平台、附近商家、骑手配送或商业店铺排行榜。
- App 图标方向是用户指定的两只小狗：小金毛和马尔济斯，小金毛戴厨师帽。图标应简约、重点清晰。
- 首页关系区应展示左侧“当前用户”头像位、中间爱心、右侧“伴侣/邀请对方”头像位；当前用户头像下方显示当前角色文本。未绑定伴侣时右侧使用空心加号，不在首页展示 App 图标。
- 首页只能保留当前情侣首页的一套 Tab/内容结构，不允许旧首页 Tab 和新首页 Tab 同时出现。
- “选择身份”不是直接跳转点菜页：必须先保存当前用户身份，并更新当前用户头像下方的角色文本。当前用户固定显示在左侧，不论选择“饲养员”还是“吃货”；右侧保留伴侣头像或邀请位。
- 可从同类“熊家小灶”复用的方向包括下单通知、伴侣账号/绑定、趣味余额或熊熊币、店铺/餐品管理、美食记录；但要按当前情侣点菜 App 改造，不做传统后台管理系统。
- 当前源码中仍有部分可见文案乱码，这是需要清理的技术债；新文档必须保持 UTF-8 中文。
- `local.properties` 注入 API Key，`orderdisk.jks` 和签名配置不能公开泄露。
- `settings.gradle.kts` 使用阿里云 Maven 镜像；海外环境可能需要代理。
- JDK 版本要求见 `gradle/gradle-daemon-jvm.properties`。
- `everything-*`、`.claude` 等目录不是 OrderDisk 主业务代码，开发时不要把它们当作 App 项目结构。

## 开发原则

- 先确认当前源码，再设计或修改，不要根据旧 Figma、旧 README、旧重构计划直接推断。
- 文档、Figma、代码三者冲突时，以当前可编译源码为准。
- 新增功能优先服务当前主线：登录/注册、情侣首页、角色选择、纪念日、类美团点菜、发现搜菜、加入我的店铺、我的页、店铺编辑、购物车、结算、订单。
- 如果要重新做多商家/美团式方向，必须先更新产品目标、导航、仓储模型和文档，再开始画图或写代码。
