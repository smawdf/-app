# OrderDisk

OrderDisk 当前是一个 Android 情侣点菜 App。当前实现重点是让两个人围绕“今天吃什么、我的店铺里有什么、要点哪些菜、形成订单记录”完成一条轻量点菜链路，而不是多商家外卖平台。

## 当前主线

```text
登录/注册
  -> 情侣首页
  -> 类美团点菜页
  -> 加入购物车
  -> 购物车
  -> 结算
  -> 订单/订单详情
```

发现页当前只有一个核心功能：搜索菜品，并把合适的结果加入“我的店铺”。“我的”页面保存/展示用户账号信息，也可以进入“我的店铺”进行编辑。旧版菜品库、随机推荐、心愿单、餐次等能力仍保留在源码中，但现在不是主要信息架构。

## 主要页面

- 登录/注册：账号登录、新用户注册，注册包含两次密码确认。
- 首页：上次确定的情侣首页版本，包含“饲养员”和“吃货”两个角色选择，并包含纪念日功能。
- 点菜：类美团点菜视图，包含店铺横幅、分类、菜品列表、详情弹层和购物车浮层。
- 发现：搜索菜品，并加入“我的店铺”。
- 我的店铺：维护店铺菜品、分类、价格和库存。
- 购物车与结算：管理已选菜品、地址、备注和提交订单。
- 订单：查看订单列表与订单详情。
- 我的：保存/展示用户账号信息，并提供进入“我的店铺”编辑的入口。

## 技术栈

Jetpack Compose + Material 3 + Navigation Compose + Koin + Room + Retrofit + Moshi + official Supabase Kotlin SDK + Coil 3 + Paging 3。

项目是单 Android 模块，包名为 `com.myorderapp`。

```text
app/src/main/java/com/myorderapp/
  data/      Room、本地 assets、Retrofit、Supabase、Repository 实现
  domain/    领域模型与仓储接口
  ui/        Compose 页面、ViewModel、导航、主题、组件
  di/        Koin 注入模块
docs/        当前说明、历史重构计划、预览与设计资产
table/       Supabase SQL
tools/       本地调试与数据脚本
```

## 常用命令

Windows/Codex 本地执行时使用：

```powershell
rtk .\gradlew.bat compileDebugKotlin
rtk .\gradlew.bat testDebugUnitTest
rtk .\gradlew.bat assembleDebug
rtk .\gradlew.bat assembleRelease
```

普通终端可对应执行：

```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

Debug APK 输出到 `app/build/outputs/apk/debug/`，Release APK 输出到 `app/build/outputs/apk/release/`。

## 当前事实源

- 设计和开发前先看 `AGENTS.md` 与 `docs/current-project-overview.md`。
- 旧的“美团式多商家/附近商家/商家 Feed”重构文档属于历史参考，不代表当前落地状态。
- 当前源码中 `SingleShopRepository` 是“我的店铺”的单店抽象，不能直接推断为多商家市场。
- App 名称尚未最终确认，用户界面设计不要强行展示 `OrderDisk` 品牌字样。
