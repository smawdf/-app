# CLAUDE.md

本项目为 Claude Code 在 OrderDisk 仓库中工作提供指引。

## 构建与测试命令

```bash
./gradlew assembleDebug          # 构建 Debug APK
./gradlew assembleRelease        # 构建 Release APK
./gradlew test                   # 运行单元测试
./gradlew lint                   # Lint 检查
```

## 项目概述

**今天吃什么？**（包名 `com.myorderapp`，原 OrderDisk）—— 二人专属点菜与菜谱管理 Android 应用。搜索菜谱 → 发起点餐 → 双方各自选菜 → 结果实时合并。

### 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 + Coil |
| 架构 | MVVM（ViewModel → Repository） |
| DI | Koin |
| 网络 | Retrofit + OkHttp + Moshi |
| 云数据库 & 认证 | Supabase REST API（直调，非 supabase-kt SDK） |
| 本地缓存 | InMemoryRepository（内存）+ SharedPreferences（Session） |
| 异步 | Kotlin Coroutines + Flow |
| 导航 | Navigation Compose |

### 包结构 (`com.myorderapp/`)

```
com.myorderapp/
├── ApiConfig.kt                   # Juhe + Spoonacular + Supabase 密钥
├── MyOrderApp.kt                  # Application，Koin 启动入口
├── di/
│   ├── AppModule.kt               # Repository + ViewModel DI
│   └── NetworkModule.kt           # Retrofit/OkHttp/Moshi + SessionManager
├── data/
│   ├── local/
│   │   └── RecipeAssetLoader.kt   # assets/recipes.json 内置 90 道菜谱
│   ├── remote/
│   │   ├── recipe/
│   │   │   ├── FoodTranslator.kt           # 中→英 180+ 词条翻译词典
│   │   │   ├── JuheRecipeApi.kt            # 聚合数据 Retrofit 接口
│   │   │   ├── JuheRecipeResponse.kt       # 响应 DTO + 错误码中文映射
│   │   │   ├── JuheRecipeMapper.kt         # API 数据 → Dish（保留原始分类）
│   │   │   ├── JuheRecipeRemoteDataSource.kt
│   │   │   ├── SpoonacularApi.kt           # Spoonacular Retrofit 接口
│   │   │   ├── SpoonacularResponse.kt      # 响应 DTO（14 个数据类）
│   │   │   ├── SpoonacularMapper.kt        # 映射 + 图片尺寸升级 + HTML 清洗
│   │   │   └── SpoonacularRemoteDataSource.kt
│   │   └── supabase/
│   │       ├── SupabaseApi.kt              # REST API（CRUD profiles/dishes/meals）
│   │       ├── SupabaseAuthApi.kt          # Auth API（signup/login/logout）
│   │       └── SessionManager.kt           # JWT 管理 + SharedPreferences 持久化
│   └── repository/
│       ├── InMemoryDishRepository.kt       # 本地菜品仓库
│       ├── SupabaseDishRepository.kt       # 云端菜品仓库
│       ├── HybridDishRepository.kt         # 在线/离线自动切换
│       ├── InMemoryProfileRepository.kt
│       ├── SupabaseProfileRepository.kt    # 云端 profile（昵称/头像/口味）
│       ├── InMemoryMealRepository.kt
│       ├── SupabaseMealRepository.kt       # 云端点餐仓库
│       └── InMemoryWishlistRepository.kt
├── domain/
│   ├── model/           # Dish, CookStep, Meal, MealItem, Profile, DietaryPreference
│   ├── repository/      # DishRepository, MealRepository, ProfileRepository 接口
│   └── usecase/
│       └── DualRecipeSearchUseCase.kt  # Juhe + Spoonacular 并行搜索 + 去重
└── ui/
    ├── theme/           # Color, Type, Theme, CategoryDisplay
    ├── navigation/      # NavGraph, BottomNavItem, Routes
    ├── auth/            # AuthScreen + AuthViewModel（登录/注册）
    ├── home/            # HomeScreen（渐变 Hero + 时段问候）
    ├── search/          # SearchScreen（本地+双API 统一搜索）
    ├── dishdetail/      # DishDetailScreen（Coil 图片 + cook steps）
    ├── dishlibrary/     # DishLibraryScreen（动态分类筛选）
    ├── adddish/         # AddDishScreen + AddDishViewModel
    ├── meal/            # StartMealScreen（搜索+双人面板）+ MealResultScreen
    ├── random/          # RandomScreen（旋转动画 + API随机抽取）
    ├── wishlist/        # WishlistScreen
    ├── history/         # HistoryScreen
    └── profile/         # ProfileScreen（昵称编辑 + 头像URL + 在线同步）
```

## 菜谱数据源

### 菜谱数据源

| 来源 | source 值 | 位置 |
|------|----------|------|
| 内置菜谱 | `"builtin"` | `assets/recipes.json` 90 道 |
| Juhe API | `"external"` / `externalSource="juhe"` | 在线，100次/天 |
| TheMealDB | `"external"` / `externalSource="themealdb"` | 在线，免费无限 |
| 用户自建 | `"custom"` | 本地/Supabase |

### 搜索流程

```
用户输入关键词 → 300ms 防抖 → 三路并行
  ├── dishRepository.searchDishes()    → 本地内置
  └── dualSearch.search(query)
        ├── Juhe API（中文原词）
        └── TheMealDB（FoodTranslator 中→英翻译后查询）
  → 结果英→中回译 → 合并去重 → 展示 → 缓存
```

## 在线/离线架构

### Session 管理

`SessionManager` 用 `SharedPreferences` 持久化 JWT token/userId/pairId，App 重启自动恢复。

### 仓库切换

| 仓库 | 在线（已登录） | 离线 |
|------|--------------|------|
| Dish | `SupabaseDishRepository` → Supabase | `InMemoryDishRepository` → 内存 |
| Profile | `SupabaseProfileRepository` → PATCH 云端 | 本地 Flow |
| Meal | `SupabaseMealRepository` → Supabase | `InMemoryMealRepository` |

### 登录后同步

`AuthViewModel.submit()` 成功后自动触发：
```kotlin
dishRepo.syncFromCloud()
profileRepo.loadFromCloud()
mealRepo.syncFromCloud()
```

## Supabase 数据库

- **Project URL**：`https://dwncdcwsbgbouoemfvwt.supabase.co`
- **表结构 SQL**：`table/` 文件夹（6 张核心表 + RLS + Realtime + Storage）
- **认证**：已注册测试用户 `odtest_online@outlook.com`（auto-confirm 已启用）
- **RLS 关键**：profiles 只用 `user_id = auth.uid()` 单策略，避免递归

## 外部 API

| API | Key | 用途 |
|-----|-----|------|
| Juhe | `7fc3e0bdf2061a5c38781c2e82908d31` | 菜谱搜索，100次/天 |
| TheMealDB | `1`（test key） | 全球菜谱，免费无日配额 |
| Supabase | `sb_publishable_8N_jUSyhvKOmWAPXGRAIhA__q96dF7a` | 数据库 + Auth + Storage |

**注意**：Spoonacular 已移除。Juhe 使用 `/fapigx/caipu/query` 端点（POST + FormUrlEncoded）。

## 关键设计决策

- **DI 使用 Koin**：`MyOrderApp.kt` 中 `startKoin { modules(appModule, networkModule) }`
- **Supabase 用 Retrofit 直调**：非 supabase-kt SDK，与现有 Moshi 体系兼容
- **@JsonClass(generateAdapter) 已移除**：AGP 9.x built-in Kotlin 不支持 kapt，改用 `KotlinJsonAdapterFactory` 反射
- **领域模型 @Json(name=snake_case)**：Dish/Profile/Meal 字段映射 Supabase 列名
- **分类用 API 原始值**：不强行映射为标准分类，保留 Juhe `type_name` / Spoonacular `cuisines`
- **搜索自动全量缓存**：`onlineResult.dishes.forEach { dishRepository.cacheSearchResult(it) }`

## 2026-05-07 重设计 & 优化

### UI 重设计（清简日常风格）
- **色系**：暖橙 → 米白 `#F5F0E8` + 深棕 `#5C4B3A` + 淡绿 `#A8C5A0`
- **字体**：Serif 标题 + SansSerif 正文
- **动态颜色**：已禁用（`dynamicColor = false`）
- **App 图标**：「食」字图标（用户提供 1254x1254 图 → 5 密度 webp）
- 所有 14 个页面统一翻新，Card 去阴影

### 注册流程改造
- OnboardingScreen 合并为三步流程：账号 → 个人资料 → 配对
- 头像支持拍照/相册/URL 三种方式
- 配对码生成/输入/跳过选项
- ProfileSetupScreen 已废弃

### 登录优化
- `SessionManager` 记住邮箱，登录页自动填入
- 错误信息细化：区分「未注册」「密码错误」「邮箱未验证」
- 退出登录按钮（仅在在线时显示），确认后清 session

### 搜索
- Spoonacular 从 `DualRecipeSearchUseCase` 移除，仅用聚合数据
- FoodTranslator 新增英→中反向翻译（`toChinese()`），Spoonacular 代码保留备用

### 口味偏好
- 6 个固定 Boolean → 用户自定义标签（`DietaryPreference.custom: List<String>`）
- 自由添加/删除，最多 6 字/标签，云端同步

### 菜品编辑与删除
- 详情页右上角 ✏️ 图标 → 编辑页（复用 AddDishScreen，预填数据）
- 菜品库**长按**自建菜 → DropdownMenu → 删除确认 → 删 Supabase + 本地文件
- `createdBy` 使用实际昵称（`"${myName}创建"`）
- AddDishScreen 移除份量字段（Dish 模型无用）

### TheMealDB 免费 API
- `https://www.themealdb.com/api/json/v1/1/`，key=`1`，无日配额
- `DualRecipeSearchUseCase`：Juhe + TheMealDB 并行搜索
- `RandomViewModel`：50% 概率用 TheMealDB 随机全球菜谱
- 结果通过 `FoodTranslator.toChinese()` 英→中自动翻译
- 新增文件：`TheMealDBApi.kt`、`TheMealDBResponse.kt`、`TheMealDBMapper.kt`、`TheMealDBRemoteDataSource.kt`

### 图片云端存储
- 拍照/相册选图 → `SupabaseStorageUploader` 压缩（800px/JPEG85%）→ 上传 Supabase Storage `dish-images` bucket
- 成功后 `dish.imageUrl` 更新为公开 URL，永久可访问
- 离线时保留本地 URI 兜底
- 需执行 `table/08_storage_global_upload.sql` 更新 RLS

### 本地持久化
- `InMemoryDishRepository`：Moshi JSON → `filesDir/dishes.json`
- `SupabaseDishRepository`：Moshi JSON → `filesDir/dishes_cloud.json`
- `SupabaseProfileRepository`：SharedPreferences 存 nickname/avatar
- 离线数据不丢，重启恢复

### 导航动画
- Push 页面：右滑 + 淡入 + 微缩放 (300ms)
- 底部 Tab：交叉淡入淡出
- 列表项：`Modifier.animateItem()` 平滑位移

### 在线/离线判断
- `ProfileViewModel` 直接观察 `SessionManager.isLoggedIn`，不依赖 `isSynced`
- 我的页显示：`在线模式 · 数据云端同步中` / `本地模式 · 点击登录云端保存`

### 文档
- `docs/superpowers/specs/` — 设计规范 & 需求文档
- `docs/superpowers/plans/` — 实施计划
- `docs/demo.html` — UI Demo（可直接浏览器打开）
