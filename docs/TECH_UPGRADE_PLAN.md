# OrderDisk 技术栈升级计划

> 版本：v1.0 | 日期：2026-06-25 | 当前版本：v1.4.7

## Current Status As Of 2026-06-28

Several items from this plan are already partially implemented in the working tree:

- Supabase app data uses the official Kotlin SDK through `SupabaseClientProvider`.
- Room is active for dish/menu/cart/address/order/profile entities. Legacy wishlist and meal tables may still exist in the schema for old-data compatibility, but their runtime repositories are no longer part of the main app graph.
- Dish library paging is wired through `RoomPagingDishRepository` and Paging Compose.
- Coil 3 is active through the application-level `SingletonImageLoader`.
- Add/edit dish queues local image uploads through WorkManager; `ImageUploadWorker` uploads, retries, and writes the public image URL back to the dish repository.
- Release hardening added the `meal_items.mealId` Room index migration, tightened local image upload queue detection, validated worker input before upload, and migrated low-risk Compose/Koin warning sites.
- Remaining release warnings are tracked explicitly: Android Gradle's `android.disallowKotlinSourceSets=false` message is an experimental option warning, and native library strip warnings are packaging-level dependency output rather than app-source behavior.

Treat the rest of this document as historical upgrade context, not an exact description of the current source tree.

## 概述

OrderDisk 当前技术栈存在多个版本滞后和架构冗余问题，最突出的是 Supabase 双通道（Retrofit + 官方 SDK）导致的性能问题。本文档规划分阶段升级路线。

---

## 第一阶段：Supabase 统一 + 性能治理（P0）

### 1.1 砍掉 Retrofit Supabase 通道

**现状：**
- `SupabaseApi.kt` — 通过 Retrofit 手动调 Supabase REST API（PostgREST）
- `SupabaseAuthApi.kt` — 通过 Retrofit 手动调 Supabase Auth API
- `SupabaseStorageUploader.kt` — 手动管理上传
- `SessionManager.kt` — 手动管理 Bearer Token

**同时 build.gradle 已有官方 SDK 依赖：**
- `supabase.postgrest.kt`
- `supabase.realtime.kt`
- `supabase.storage.kt`
- `supabase.auth.kt`
- `supabase.compose.auth`

**问题：**
- 两套并存，代码重复，维护成本高
- Retrofit 那套没有连接池复用、Token 自动刷新
- 每次请求手动拼 Authorization header，Token 过期会 401

**改动：**

| 文件 | 操作 |
|------|------|
| `SupabaseApi.kt` | 删除 |
| `SupabaseAuthApi.kt` | 删除 |
| `SupabaseStorageUploader.kt` | 重写为官方 SDK 调用 |
| `SupabaseDishPayload.kt` | 删除（官方 SDK 直接用 Domain Model） |
| `SupabaseQuery.kt` | 删除 |
| `SessionManager.kt` | 简化，Token 交给官方 SDK 管理 |
| `SupabaseDishRepository.kt` | 重写，用 `supabase.postgrest` |
| 旧餐次仓储 | 已从当前运行时移除；不要再按旧随机选菜主流程重写 |
| `SupabaseProfileRepository.kt` | 重写，用 `supabase.postgrest` |
| `NetworkModule.kt` | 移除 supabase Retrofit 实例，添加 SupabaseClient 初始化 |
| `AppModule.kt` | 更新 DI 注入 |

**新增：**
- `SupabaseClientProvider.kt` — 单例管理 SupabaseClient 初始化

**官方 SDK 用法示例：**
```kotlin
// 初始化
val supabase = createSupabaseClient(
    supabaseUrl = ApiConfig.SUPABASE_URL,
    supabaseKey = ApiConfig.SUPABASE_ANON_KEY
) {
    install(Postgrest)
    install(Auth)
    install(Storage)
    install(Realtime)
}

// 查询菜品（替代 Retrofit SupabaseApi.getDishes）
val dishes = supabase.from("dishes")
    .select { eq("pair_id", pairId) }
    .decodeList<Dish>()

// 认证（替代手动拼 Bearer）
supabase.auth.signInWith(Email) {
    email = "user@example.com"
    password = "password"
}
```

**收益：**
- Token 自动刷新，不再 401
- 连接池复用，减少延迟
- Realtime 订阅可替代 3 秒轮询（对方选菜实时可见）
- 代码量减少 ~40%

### 1.2 清理日志

**现状：** `NetworkModule.kt` 中 `HttpLoggingInterceptor.Level.BODY`

**改动：** 生产环境改为 `Level.NONE`，Debug 构建改为 `Level.BASIC`

```kotlin
val logging = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
}
```

### 1.3 OkHttp 连接池优化

**改动：**
```kotlin
OkHttpClient.Builder()
    .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
    .addInterceptor(logging)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()
```

---

## 第二阶段：序列化 + 依赖升级（P1）

### 2.1 Moshi Reflect → Code Gen

**现状：** `Moshi.Builder().addLast(KotlinJsonAdapterFactory())` — 运行时反射

**改动：**
1. build.gradle 移除 `moshi-kotlin`，添加 `moshi-kotlin-codegen`
2. 所有 data class 添加 `@JsonClass(generateAdapter = true)`
3. 涉及文件：`Dish.kt`, `Meal.kt`, `Profile.kt`, `WishlistItem.kt`, `MealItem.kt` 及所有 Response 类

**收益：** 零反射、启动更快、APK 更小、ProGuard 兼容

### 2.2 OkHttp 4.12 → 5.0 + Retrofit 2.9 → 2.11

**改动：** `libs.versions.toml` 更新版本号，处理 API 变更（OkHttp 5 包名变化）

### 2.3 Coil 2.5 → 3.x

**改动：**
1. `coil-compose` → `coil3-compose`
2. `ImageLoader` 初始化改为 Coil 3 API
3. 启用磁盘缓存

**收益：** 三级缓存（内存/磁盘/网络）、渐进式加载、HEIF 支持

### 2.4 Koin 3.5 → 4.x

**改动：**
1. `koin-bom` 升级到 4.x
2. `koin-android` → `koin-androidx`（如需要）
3. `koin-compose` 替代手动 `getViewModel()`

### 2.5 移除 Accompanist

**改动：** `accompanist-permissions` → Compose Foundation 内置权限 API

### 2.6 Navigation 2.7 → 2.9+

**改动：** 迁移到 Type-safe routes（Kotlin Serialization），替代字符串路由

---

## 第三阶段：架构增强（P2）

### 3.1 引入 Room 本地数据库

**现状：** JSON 文件持久化（`dishes_cloud.json`），SharedPreferences 存状态

**改动：**
- 新增 `AppDatabase.kt`（Room）
- `DishEntity` / `MealEntity` / `ProfileEntity`
- `DishDao` / `MealDao` / `ProfileDao`
- `HybridDishRepository` 改为 Room + Supabase 双源

**收益：** 查询性能、索引、分页、Flow 自动更新

### 3.2 统一错误处理

**现状：** 直接 try-catch，无统一类型

**改动：**
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

### 3.3 图片上传队列

**现状：** 后台异步上传，失败就失败了

**改动：** WorkManager 管理上传队列，失败自动重试，支持离线排队

### 3.4 分页加载

**现状：** `getAllDishes()` 一次拉全量

**改动：** Supabase + Room 分页查询，Compose `LazyPagingItems`

---

## 执行顺序

```
第一阶段（1-2天）
├── 1.1 Supabase 统一 ← 最影响性能
├── 1.2 清理日志
└── 1.3 OkHttp 连接池

第二阶段（1-2天）
├── 2.1 Moshi Code Gen
├── 2.2 OkHttp/Retrofit 升级
├── 2.3 Coil 3
├── 2.4 Koin 4
├── 2.5 移除 Accompanist
└── 2.6 Navigation 升级

第三阶段（2-3天）
├── 3.1 Room 数据库
├── 3.2 错误处理
├── 3.3 上传队列
└── 3.4 分页加载
```

---

## 风险与回退

| 风险 | 缓解 |
|------|------|
| Supabase SDK API 不兼容 | 逐个 Repository 替换，保留旧代码注释 |
| Moshi Code Gen 遗漏类 | 编译期会报错，逐个修复 |
| Coil 3 迁移图片加载异常 | 灰度测试，保留回退分支 |
| Room 迁移数据丢失 | 提供 Migration 从 JSON 导入 |

---

## 不动的部分

- Kotlin 2.2.10 ✅ 够新
- Compose BOM 2026.02.01 ✅ 够新
- Lottie 6.3.0 ✅ 够新
- 整体 Clean Architecture + MVVM 架构 ✅ 没问题
- 离线优先策略 ✅ 方向正确
