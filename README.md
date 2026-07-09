# 高糖小食

高糖小食当前是一个 Android Compose 情侣点菜 App。主流程是：

```text
登录/注册 -> 情侣首页 -> 点菜 -> 购物车 -> 结算 -> 订单
```

发现页用于搜索中文菜品，并把合适的结果加入“我的小店”。当前产品不是多商家外卖平台，也不默认包含附近商家、骑手配送、商家排行榜或平台补贴等外卖业务。

## 当前页面

- 登录/注册：`AuthScreen`、`OnboardingScreen`
- 情侣首页：`CoupleMenuScreen`
- 点菜：`OrderingScreen`
- 发现搜菜：`DiscoverScreen`
- 我的店铺编辑：`MenuManagementScreen`
- 购物车/结算/订单：`CartScreen`、`CheckoutScreen`、`OrdersScreen`、`OrderDetailScreen`
- 我的：`ProfileScreen`

## 技术栈

- Jetpack Compose + Material 3 + Navigation Compose
- Koin
- Room
- Retrofit + Moshi
- Supabase Kotlin SDK
- Coil 3
- Paging 3

源码主目录：

```text
app/src/main/java/com/myorderapp/
  data/      Room、本地 assets、Retrofit、Supabase、Repository 实现
  domain/    领域模型和仓储接口
  ui/        Compose 页面、ViewModel、导航、主题和组件
  di/        Koin 注入模块
table/       Supabase SQL
docs/        当前产品与设计文档
```

## 常用命令

在 Codex/Windows 本地执行 Gradle 命令必须带 `rtk` 前缀：

```powershell
rtk .\gradlew.bat compileDebugKotlin
rtk .\gradlew.bat testDebugUnitTest
rtk .\gradlew.bat assembleDebug
rtk .\gradlew.bat assembleRelease
```

不要重复打包；只有明确需要 APK 时再执行 `assembleDebug`。

## 当前数据源

- 中文菜谱本地库：`app/src/main/assets/bimissing_recipes.json`
- 实时搜索：下厨房搜索源
- 图片兜底：下厨房结果优先，缺图时走 Bing 图片兜底
- Tian 菜谱 API 仍用于菜谱数据补充

旧 Jisu/Juhe 接入、旧 Realtime 餐次同步、云端 debug 日志表和重复 Stitch 资源目录已经移除。`bimissing_recipes.json` 中保留旧菜谱数据，但运行时会过滤 `hoto.cn/res.hoto.cn` 图片，缺图时进入当前图片兜底链路。

## 登录与云端能力

- 邮箱/账号使用账号密码登录；邮箱账号可用于找回密码和验证切换设备。
- 邮箱注册后如果 Supabase 开启邮箱确认，需要先打开确认邮件再登录。
- 单设备登录使用 `profiles.session_id` 和 `profiles.session_updated_at`。旧设备主动退出会释放设备占用；新设备可通过注册邮箱验证后接管。
- 单设备会话超过 30 天未活跃会被视为过期。
- 相关 Supabase SQL：`table/16_single_device_session.sql`。

## UI 与动画

- 主要页面使用暖色、明亮、低阴影的 Cozy/Stitch 风格。
- 页面切换、提示、购物车飞入和按压反馈统一走 `CozyMotion` token，避免散落动画时长。
- 当前主源码已扫描清理可见乱码文案和外卖平台化文案；旧图源过滤逻辑仍保留用于图片兜底。

## 开发约定

- 以当前源码和 `AGENTS.md` 为准。
- UI 继续贴近 Stitch 原型，整体偏明亮温暖，不乱加阴影。
- UI 可见品牌以“高糖小食”为准；`OrderDisk` 仍可能作为历史项目名、包内主题名或文档上下文存在。
- 不恢复旧多商家/外卖市场假设，除非先明确更新产品方向。
