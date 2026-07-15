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

## APK 与发布

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/<versionName>.apk`
- 当前正式包：`1.0.0.apk`
- 正式版本通过 GitHub Releases 发布，Release 标签使用 `v<versionName>`，例如 `v1.0.0`。
- Release APK 使用本地 `keystore.properties` 和签名文件构建；密钥、签名文件和 APK 不提交到 Git 仓库。

## 当前数据源

- 中文菜谱本地库：`app/src/main/assets/bimissing_recipes.json`
- 实时搜索：下厨房搜索源
- 图片兜底：下厨房结果优先，缺图时走 Bing 图片兜底
- Tian 菜谱 API 仍用于菜谱数据补充

旧 Jisu/Juhe 接入、旧 Realtime 餐次同步、云端 debug 日志表和重复 Stitch 资源目录已经移除。`bimissing_recipes.json` 中保留旧菜谱数据，但运行时会过滤 `hoto.cn/res.hoto.cn` 图片，缺图时进入当前图片兜底链路。

## 登录与云端能力

- 邮箱或普通账号使用账号密码登录。
- 当前私人版允许同一账号在多台设备登录，不依赖邮箱或短信验证完成日常登录。
- 历史 `session_id` 字段保留兼容和错误日志用途，不再用于设备挤下线。
- 个人资料、情侣关系、店铺、菜单、订单、糖糖币、纪念日、偏好和图片 URL 尽量保存到 Supabase。
- 购物车只保存在本机，不上传云端。

## UI 与动画

- 主要页面使用暖色、明亮、低阴影的 Cozy/Stitch 风格。
- 页面切换、提示、购物车飞入和按压反馈统一走 `CozyMotion` token，避免散落动画时长。
- 当前主源码已扫描清理可见乱码文案和外卖平台化文案；旧图源过滤逻辑仍保留用于图片兜底。
- 当前产品与视觉事实源见根目录 `PRODUCT.md` 和 `DESIGN.md`。

## 开发约定

- 以当前源码和 `AGENTS.md` 为准。
- UI 继续贴近 Stitch 原型，整体偏明亮温暖，不乱加阴影。
- UI 可见品牌以“高糖小食”为准；`OrderDisk` 仍可能作为历史项目名、包内主题名或文档上下文存在。
- 不恢复旧多商家/外卖市场假设，除非先明确更新产品方向。
