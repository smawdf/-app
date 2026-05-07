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

### 四层数据 + 动态分类

| 来源 | source 值 | 位置 |
|------|----------|------|
| 内置菜谱 | `"builtin"` | `assets/recipes.json` 90 道 |
| Juhe API | `"external"` / `externalSource="juhe"` | 在线查询 |
| Spoonacular | `"external"` / `externalSource="spoonacular"` | 在线查询（中→英自动翻译） |
| 用户自建 | `"custom"` | 本地/Supabase |

**分类**：直接使用 API 原始值（Juhe `type_name` 如"川菜""猪肉"，Spoonacular `cuisines` 如"Italian"），筛选标签随搜索结果动态变化。

### 搜索流程

```
用户输入关键词 → 300ms 防抖 → 三路并行
  ├── dishRepository.searchDishes()    → 本地内置
  └── dualSearch.search(query)
        ├── Juhe API（中文原词）
        └── Spoonacular（FoodTranslator 中→英）
  → 合并去重 → 统一展示 → 全量缓存到本地仓库
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
| Juhe | `7fc3e0bdf2061a5c38781c2e82908d31` | 中餐搜索（type_name 作为分类） |
| Spoonacular | `c4d077f93cbd4545ae6337c03b1c925a` | 西餐搜索 + 高质量图片（636x393） |
| Supabase | `sb_publishable_8N_jUSyhvKOmWAPXGRAIhA__q96dF7a` | 在线数据库 |

**注意**：Spoonacular 免费额度 150次/天，Juhe 100次/天。搜索结果自动缓存，避免重复调用。

## 关键设计决策

- **DI 使用 Koin**：`MyOrderApp.kt` 中 `startKoin { modules(appModule, networkModule) }`
- **Supabase 用 Retrofit 直调**：非 supabase-kt SDK，与现有 Moshi 体系兼容
- **@JsonClass(generateAdapter) 已移除**：AGP 9.x built-in Kotlin 不支持 kapt，改用 `KotlinJsonAdapterFactory` 反射
- **领域模型 @Json(name=snake_case)**：Dish/Profile/Meal 字段映射 Supabase 列名
- **分类用 API 原始值**：不强行映射为标准分类，保留 Juhe `type_name` / Spoonacular `cuisines`
- **搜索自动全量缓存**：`onlineResult.dishes.forEach { dishRepository.cacheSearchResult(it) }`
