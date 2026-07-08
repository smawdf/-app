# OrderDisk

OrderDisk 当前是一个 Android Compose 情侣点菜 App。主流程是：

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

## 开发约定

- 以当前源码和 `AGENTS.md` 为准。
- UI 继续贴近 Stitch 原型，整体偏明亮温暖，不乱加阴影。
- 不把 `OrderDisk` 当成未确认的用户界面品牌文案。
- 不恢复旧多商家/外卖市场假设，除非先明确更新产品方向。
