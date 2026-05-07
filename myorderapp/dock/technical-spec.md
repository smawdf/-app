# myorderapp — 技术文档

> 二人专属点菜 & 做菜 Android 应用
> 版本：v1.0 | 更新日期：2026-05-03

---

## 一、项目概述

### 1.1 定位
myorderapp 是一款专为情侣/二人设计的点菜与菜谱管理 Android 应用。支持自定义菜品录入与全网菜谱查询，所有菜品均配备完整的制作方法与展示图，帮助用户解决"今天吃什么"的选择困难。

### 1.2 核心特性
- **双模式菜品库**：自定义菜品 + 全网菜谱搜索
- **完整菜谱信息**：每道菜都有展示图 + 分步骤制作方法
- **实时云同步**：基于 Supabase，双方手机操作即时同步
- **点餐流程**：发起点餐 → 各选各的 → 合并确认
- **智能推荐**：随机摇一摇、历史偏好推荐
- **心愿单 & 历史**：想吃的记下来，吃过的有记录

### 1.3 使用场景
- 两个人商量今天吃什么
- 记录好吃的菜，下次直接点
- 查看菜谱，决定是点外卖还是自己做
- 随机推荐，打破选择困难

---

## 二、技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────┐
│                 Android App                  │
│  ┌─────────┐  ┌──────────┐  ┌────────────┐  │
│  │   UI    │  │ ViewModel│  │ Repository │  │
│  │  Layer  │──│  Layer   │──│   Layer    │  │
│  │(Compose)│  │(MVVM)    │  │            │  │
│  └─────────┘  └──────────┘  └─────┬──────┘  │
└────────────────────────────────────┼────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
              ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼─────┐
              │  Supabase  │   │  External │   │   Local   │
              │  Cloud DB  │   │  Recipe   │   │   Cache   │
              │  (Postgres)│   │    API    │   │  (Room)   │
              └───────────┘   └───────────┘   └───────────┘
```

### 2.2 技术栈

| 层级 | 技术方案 | 说明 |
|------|----------|------|
| **开发语言** | Kotlin | Android 官方推荐语言 |
| **UI 框架** | Jetpack Compose | 声明式 UI，开发效率高 |
| **架构模式** | MVVM + Clean Architecture | ViewModel + UseCase + Repository |
| **依赖注入** | Hilt (Dagger) | Android 官方推荐 DI 框架 |
| **网络请求** | Retrofit + OkHttp + Moshi | REST API 调用 |
| **本地缓存** | Room Database | 离线数据缓存 |
| **图片加载** | Coil | Kotlin-first 图片加载库 |
| **异步处理** | Kotlin Coroutines + Flow | 响应式数据流 |
| **云数据库** | Supabase (PostgreSQL) | 实时同步、认证、存储 |
| **实时通信** | Supabase Realtime | Postgres Changes 推送 |
| **文件存储** | Supabase Storage | 菜品图片上传 |
| **导航** | Navigation Compose | 页面路由管理 |

### 2.3 项目结构

```
com.myorderapp/
├── di/                          # Hilt 依赖注入模块
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── NetworkModule.kt
├── data/
│   ├── local/
│   │   ├── dao/                 # Room DAO
│   │   ├── entity/              # Room Entity
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   ├── supabase/            # Supabase 客户端 & API
│   │   │   ├── SupabaseClient.kt
│   │   │   ├── DishRepository.kt
│   │   │   ├── MealRepository.kt
│   │   │   └── ProfileRepository.kt
│   │   └── recipe/              # 外部菜谱 API
│   │       ├── RecipeApi.kt
│   │       └── RecipeMapper.kt
│   └── repository/              # Repository 实现
│       ├── DishRepositoryImpl.kt
│       ├── MealRepositoryImpl.kt
│       └── SyncRepositoryImpl.kt
├── domain/
│   ├── model/                   # 领域模型
│   │   ├── Dish.kt
│   │   ├── Meal.kt
│   │   ├── Profile.kt
│   │   └── CookStep.kt
│   ├── usecase/                 # 业务逻辑
│   │   ├── SearchRecipeUseCase.kt
│   │   ├── CreateMealUseCase.kt
│   │   ├── RandomDishUseCase.kt
│   │   └── SyncDataUseCase.kt
│   └── repository/              # Repository 接口
│       └── *.kt
├── ui/
│   ├── theme/                   # Material 3 主题
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt
│   ├── navigation/              # 导航图
│   │   └── NavGraph.kt
│   ├── home/                    # 首页
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── search/                  # 全网菜谱搜索
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt
│   ├── dishdetail/              # 菜品详情
│   │   ├── DishDetailScreen.kt
│   │   └── DishDetailViewModel.kt
│   ├── dishlibrary/             # 菜品库
│   │   ├── DishLibraryScreen.kt
│   │   └── DishLibraryViewModel.kt
│   ├── adddish/                 # 添加/编辑菜品
│   │   ├── AddDishScreen.kt
│   │   └── AddDishViewModel.kt
│   ├── meal/                    # 点餐流程
│   │   ├── StartMealScreen.kt
│   │   ├── MealOrderScreen.kt
│   │   ├── MealResultScreen.kt
│   │   └── MealViewModel.kt
│   ├── random/                  # 随机推荐
│   │   ├── RandomScreen.kt
│   │   └── RandomViewModel.kt
│   ├── wishlist/                # 心愿单
│   │   ├── WishlistScreen.kt
│   │   └── WishlistViewModel.kt
│   ├── history/                 # 历史记录
│   │   ├── HistoryScreen.kt
│   │   └── HistoryViewModel.kt
│   └── profile/                 # 个人设置
│       ├── ProfileScreen.kt
│       └── ProfileViewModel.kt
├── util/                        # 工具类
│   ├── DateFormatter.kt
│   ├── ImagePicker.kt
│   └── SyncManager.kt
└── MyOrderApp.kt                # Application 类
```

---

## 三、数据模型设计（Supabase）

### 3.1 ER 关系图

```
profiles ──1:N── dishes
profiles ──1:N── meals
dishes   ──N:M── meals (through meal_items)
dishes   ──1:N── wishlists
dishes   ──1:N── tags (through dish_tags)
```

### 3.2 表结构详细定义

#### `profiles` — 用户信息表

```sql
CREATE TABLE profiles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    pair_id     UUID NOT NULL,              -- 情侣配对 ID，两人共享
    nickname    TEXT NOT NULL,
    avatar_url  TEXT,
    taste_prefs JSONB DEFAULT '{}',         -- 口味偏好 {"spicy": true, "sweet": false}
    allergies   TEXT[],                      -- 忌口
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

-- 索引
CREATE INDEX idx_profiles_pair_id ON profiles(pair_id);
CREATE INDEX idx_profiles_user_id ON profiles(user_id);
```

**taste_prefs 结构示例：**
```json
{
  "spicy": true,
  "sweet": false,
  "sour": true,
  "salty": false,
  "light": true,
  "heavy": false
}
```

#### `dishes` — 菜品库表

```sql
CREATE TABLE dishes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pair_id       UUID NOT NULL,
    name          TEXT NOT NULL,
    source        TEXT NOT NULL CHECK (source IN ('custom', 'external')),
    external_id   TEXT,                      -- 外部 API 菜品 ID
    external_source TEXT,                    -- 外部 API 来源标识
    category      TEXT,                      -- 分类：中餐/西餐/甜品/饮品/日料/韩餐/东南亚
    image_url     TEXT,                      -- 展示图 URL
    cook_steps    JSONB DEFAULT '[]',        -- 制作步骤
    ingredients   TEXT[],                    -- 食材清单
    difficulty    INTEGER CHECK (difficulty BETWEEN 1 AND 5),  -- 难度 1-5
    cook_time_min INTEGER,                   -- 预计烹饪时间（分钟）
    who_likes     TEXT[],                    -- 谁爱吃 ['user_a', 'user_b']
    rating        REAL,                      -- 评分 1-5
    notes         TEXT,                      -- 备注
    created_by    UUID REFERENCES profiles(id),
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);

-- 索引
CREATE INDEX idx_dishes_pair_id ON dishes(pair_id);
CREATE INDEX idx_dishes_source ON dishes(pair_id, source);
CREATE INDEX idx_dishes_category ON dishes(pair_id, category);
CREATE INDEX idx_dishes_name_search ON dishes USING gin(to_tsvector('simple', name));
```

**cook_steps 结构示例：**
```json
[
  {
    "step": 1,
    "description": "将鸡胸肉切成小块，加入料酒、生抽腌制15分钟",
    "image_url": "https://..."
  },
  {
    "step": 2,
    "description": "热锅冷油，放入蒜末爆香",
    "image_url": null
  },
  {
    "step": 3,
    "description": "倒入鸡丁翻炒至变色，加入豆瓣酱继续翻炒",
    "image_url": "https://..."
  }
]
```

#### `dish_tags` — 菜品标签关联表

```sql
CREATE TABLE dish_tags (
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dish_id  UUID REFERENCES dishes(id) ON DELETE CASCADE,
    pair_id  UUID NOT NULL,
    name     TEXT NOT NULL,                  -- 标签名
    color    TEXT DEFAULT '#FF6B6B',         -- 标签颜色
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_dish_tags_pair_id ON dish_tags(pair_id);
CREATE INDEX idx_dish_tags_dish_id ON dish_tags(dish_id);
```

#### `meals` — 点餐记录表

```sql
CREATE TABLE meals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pair_id     UUID NOT NULL,
    meal_type   TEXT NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'supper', 'other')),
    date        DATE NOT NULL DEFAULT CURRENT_DATE,
    status      TEXT NOT NULL CHECK (status IN ('ordering', 'confirmed', 'completed', 'cancelled')),
    created_by  UUID REFERENCES profiles(id),
    confirmed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_meals_pair_id ON meals(pair_id);
CREATE INDEX idx_meals_date ON meals(pair_id, date DESC);
```

#### `meal_items` — 点餐明细表

```sql
CREATE TABLE meal_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_id    UUID REFERENCES meals(id) ON DELETE CASCADE,
    dish_id    UUID REFERENCES dishes(id),
    chosen_by  UUID REFERENCES profiles(id),
    quantity   INTEGER DEFAULT 1,
    notes      TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_meal_items_meal_id ON meal_items(meal_id);
```

#### `wishlists` — 心愿单表

```sql
CREATE TABLE wishlists (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pair_id    UUID NOT NULL,
    dish_id    UUID REFERENCES dishes(id) ON DELETE CASCADE,
    added_by   UUID REFERENCES profiles(id),
    status     TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'tried', 'rejected')),
    notes      TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_wishlists_pair_id ON wishlists(pair_id);
CREATE INDEX idx_wishlists_status ON wishlists(pair_id, status);
```

### 3.3 Row Level Security (RLS) 策略

```sql
-- 启用 RLS
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE dishes ENABLE ROW LEVEL SECURITY;
ALTER TABLE meals ENABLE ROW LEVEL SECURITY;
ALTER TABLE meal_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE wishlists ENABLE ROW LEVEL SECURITY;
ALTER TABLE dish_tags ENABLE ROW LEVEL SECURITY;

-- profiles: 用户只能访问自己的 profile
CREATE POLICY "Users can view own profile"
    ON profiles FOR SELECT
    USING (user_id = auth.uid());

CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE
    USING (user_id = auth.uid());

-- dishes: 按 pair_id 隔离
CREATE POLICY "Users can access pair dishes"
    ON dishes FOR ALL
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

-- meals: 按 pair_id 隔离
CREATE POLICY "Users can access pair meals"
    ON meals FOR ALL
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

-- meal_items: 通过 meal 关联
CREATE POLICY "Users can access pair meal items"
    ON meal_items FOR ALL
    USING (meal_id IN (
        SELECT m.id FROM meals m
        JOIN profiles p ON p.pair_id = m.pair_id
        WHERE p.user_id = auth.uid()
    ));

-- wishlists: 按 pair_id 隔离
CREATE POLICY "Users can access pair wishlists"
    ON wishlists FOR ALL
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));

-- dish_tags: 按 pair_id 隔离
CREATE POLICY "Users can access pair dish tags"
    ON dish_tags FOR ALL
    USING (pair_id IN (
        SELECT pair_id FROM profiles WHERE user_id = auth.uid()
    ));
```

### 3.4 Supabase Realtime 配置

```sql
-- 启用实时推送
ALTER PUBLICATION supabase_realtime ADD TABLE dishes;
ALTER PUBLICATION supabase_realtime ADD TABLE meals;
ALTER PUBLICATION supabase_realtime ADD TABLE meal_items;
ALTER PUBLICATION supabase_realtime ADD TABLE wishlists;
```

### 3.5 Supabase Storage 配置

```sql
-- 创建存储桶
INSERT INTO storage.buckets (id, name, public)
VALUES ('dish-images', 'dish-images', true);

-- 存储策略：按 pair_id 创建子目录
CREATE POLICY "Users can upload dish images"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'dish-images'
        AND (storage.foldername(name))[1] IN (
            SELECT pair_id::text FROM profiles WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "Users can view dish images"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'dish-images');
```

---

## 四、外部菜谱 API 集成

### 4.1 API 选型对比

| API | 中餐覆盖 | 免费额度 | 图片质量 | 制作步骤 | 推荐指数 |
|-----|---------|---------|---------|---------|---------|
| **Spoonacular** | ⭐⭐⭐ | 150次/天 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **TheMealDB** | ⭐⭐ | 无限(测试) | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Edamam** | ⭐⭐⭐ | 100次/天 | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **聚合数据菜谱** | ⭐⭐⭐⭐⭐ | 100次/天 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **天行数据菜谱** | ⭐⭐⭐⭐⭐ | 100次/天 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### 4.2 推荐方案：双 API 组合

- **主 API**：Spoonacular（国际菜品、高质量图片、详细步骤）
- **辅 API**：聚合数据/天行数据（中餐覆盖好）
- **降级策略**：主 API 无结果时自动查询辅 API

### 4.3 Spoonacular API 接口

```kotlin
interface SpoonacularApi {

    // 搜索菜品
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("number") number: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("apiKey") apiKey: String,
        @Query("addRecipeInformation") addInfo: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("instructionsRequired") instructionsRequired: Boolean = true
    ): RecipeSearchResponse

    // 获取菜品详情（含制作步骤）
    @GET("recipes/{id}/information")
    suspend fun getRecipeDetail(
        @Path("id") recipeId: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = false
    ): RecipeDetailResponse

    // 按食材搜索
    @GET("recipes/findByIngredients")
    suspend fun findByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 20,
        @Query("apiKey") apiKey: String
    ): List<RecipeByIngredientResponse>
}
```

### 4.4 数据映射（External → Local）

```kotlin
// Spoonacular 响应 → 本地 Dish 模型
fun RecipeDetailResponse.toDishModel(pairId: String): Dish {
    return Dish(
        pairId = pairId,
        name = this.title,
        source = "external",
        externalId = this.id.toString(),
        externalSource = "spoonacular",
        imageUrl = this.image,
        cookSteps = this.analyzedInstructions
            .flatMap { it.steps }
            .map { step ->
                CookStep(
                    step = step.number,
                    description = step.step,
                    imageUrl = step.equipment?.firstOrNull()?.image
                )
            },
        ingredients = this.extendedIngredients.map { it.name },
        difficulty = estimateDifficulty(this.readyInMinutes, this.servings),
        cookTimeMin = this.readyInMinutes
    )
}
```

### 4.5 API 缓存策略

```
用户搜索 → 检查本地 Room 缓存 → 命中 → 返回缓存数据
                               ↓ 未命中
                          调用外部 API → 展示结果（不缓存搜索结果）
                                        ↓ 用户点击详情/收藏
                                   保存完整数据到 Supabase + Room
```

- 搜索结果：不缓存（实时性优先）
- 菜品详情：收藏后持久化到 Supabase
- 图片 URL：直接引用外部 CDN，不重复存储

---

## 五、同步策略

### 5.1 实时同步流程

```
设备 A 操作 ──→ Supabase ──→ Realtime 推送 ──→ 设备 B 更新
     ↑                                              │
     └──── Room 本地缓存 ←──────────────────────────┘
```

### 5.2 冲突处理

| 场景 | 策略 |
|------|------|
| 同时编辑同一菜品 | Last Write Wins（最后写入胜出） |
| 同时发起点餐 | 允许并行，各自独立 |
| 同时删除同一菜品 | 先删先生效，后删跳过 |
| 网络断开 | 操作写入本地队列，恢复后批量同步 |

### 5.3 离线优先策略

```kotlin
class DishRepositoryImpl @Inject constructor(
    private val dishDao: DishDao,           // Room 本地
    private val supabaseClient: SupabaseClient, // 云端
    private val connectivityManager: ConnectivityManager
) : DishRepository {

    override fun getDishes(pairId: String): Flow<List<Dish>> {
        return if (connectivityManager.isConnected()) {
            // 在线：Supabase 实时流 + 本地缓存
            supabaseClient.getDishesRealtime(pairId)
                .onEach { dishes -> dishDao.upsertAll(dishes) }
        } else {
            // 离线：纯本地缓存
            dishDao.getAllByPairId(pairId)
        }
    }
}
```

---

## 六、功能模块详细设计

### 6.1 首页模块

**功能：**
- 显示今日点餐状态（未发起 / 进行中 / 已完成）
- 快捷入口：发起点餐、搜索菜谱、随机推荐
- 最近点过的菜（Top 5 快捷复点）
- 今日推荐（基于历史偏好）

**交互流程：**
```
进入首页
  ├── 查看今日状态卡片
  │   ├── 未发起 → 点击「发起点餐」→ 发起点餐页
  │   ├── 进行中 → 点击「查看」→ 点餐合并结果页
  │   └── 已完成 → 点击「查看」→ 历史记录页
  ├── 搜索框 → 全网菜谱搜索页
  ├── 随机推荐 → 转盘页
  └── 最近菜品 → 菜品详情页
```

### 6.2 全网菜谱搜索模块

**功能：**
- 关键词搜索全网菜品
- 搜索结果展示：菜名 + 展示图 + 简介 + 烹饪时间 + 难度
- 点击进入菜品详情（含完整制作方法）
- 一键收藏到菜品库

**交互流程：**
```
输入关键词
  → 调用外部 API（Spoonacular / 聚合数据）
  → 展示搜索结果列表
  → 点击菜品卡片
    → 显示详情（大图 + 食材 + 步骤）
    → [加入菜品库] → 保存到 Supabase
    → [加入心愿单] → 添加到心愿单
    → [发起点餐] → 跳转点餐流程
```

### 6.3 菜品库模块

**功能：**
- 统一展示：自定义菜品 + 全网收藏菜品
- 多维度筛选：来源、分类、标签、难度、谁爱吃
- 列表/卡片视图切换
- 搜索（本地菜品库内搜索）

### 6.4 自定义菜品模块

**功能：**
- 添加菜品表单：名称、展示图、分类、食材、难度、耗时、备注
- **制作方法编辑器**：
  - 步骤式编辑（添加/删除/拖拽排序）
  - 每步可配图（拍照/相册）
  - 支持富文本描述
- 编辑已有自定义菜品
- 删除菜品（软删除，可恢复）

**制作方法编辑器交互：**
```
[添加步骤]
  ├── 步骤序号（自动编号）
  ├── 描述输入（多行文本）
  ├── 配图（可选：拍照 / 从相册选择 / 输入URL）
  └── [删除步骤]

[拖拽排序] ← 长按步骤卡片拖动

[保存] → 写入 Supabase → 实时同步到对方
```

### 6.5 点餐流程模块

**功能：**
- 发起点餐：选择餐次（早餐/午餐/晚餐/夜宵）
- 双人独立选菜：各自从菜品库中选择
- 实时同步对方选择进度
- 合并展示：双方选择的菜品汇总
- 确认下单 / 取消

**完整流程：**
```
用户 A 发起点餐
  → 选择餐次（午餐）
  → 进入选菜界面
  → A 选了 2 道菜，提交

用户 B 收到通知
  → 进入同一轮点餐
  → B 选了 2 道菜，提交

双方都提交后
  → 合并展示页：4 道菜汇总
  → 确认 → 标记完成
  → 写入历史记录
```

### 6.6 随机推荐模块

**功能：**
- 转盘动画（展示 8 道候选菜）
- 筛选条件：分类、谁爱吃、最近没吃过的
- 摇一摇触发（手机陀螺仪）
- 结果展示 → 可重新摇 / 加入点餐

### 6.7 心愿单模块

**功能：**
- 「想吃」列表：待尝试的菜品
- 状态管理：待尝试 → 已尝试 / 已拒绝
- 一键加入点餐
- 添加来源备注

### 6.8 历史记录模块

**功能：**
- 日历视图：按日期查看历史点餐
- 列表视图：按时间倒序
- 统计面板：
  - 最爱菜品 Top 10
  - 各类菜品占比（饼图）
  - 谁更挑食（点菜数量对比）
  - 避免重复提醒

### 6.9 个人设置模块

**功能：**
- 个人信息：昵称、头像
- 口味偏好设置（辣/甜/酸/咸/清淡/重口）
- 忌口管理
- Supabase 连接状态
- 配对管理（生成配对码 / 输入配对码）
- 数据导出

---

## 七、页面原型图清单

共 12 个核心页面，详见 `prototypes/` 目录：

| 编号 | 文件名 | 页面 | 说明 |
|------|--------|------|------|
| 01 | `01_home` | 首页 | 状态卡片 + 快捷入口 + 最近菜品 |
| 02 | `02_search` | 全网菜谱搜索 | 搜索框 + 结果卡片流 |
| 03 | `03_detail_online` | 菜品详情（全网） | 大图 + 步骤 + 食材 + 收藏 |
| 04 | `04_detail_custom` | 菜品详情（自定义） | 展示图 + 步骤 + 编辑 |
| 05 | `05_add_dish` | 添加/编辑菜品 | 表单 + 图片上传 + 步骤编辑 |
| 06 | `06_dish_library` | 菜品库列表 | 合并列表 + 筛选 |
| 07 | `07_start_meal` | 发起点餐 | 餐次选择 + 选菜 |
| 08 | `08_meal_result` | 点餐合并结果 | 双方选择汇总 |
| 09 | `09_random` | 随机推荐 | 转盘动画 |
| 10 | `10_wishlist` | 心愿单 | 待尝试列表 |
| 11 | `11_history` | 历史记录 | 日历 + 统计 |
| 12 | `12_settings` | 个人设置 | 偏好 + 配对 + 同步 |

---

## 八、UI 设计规范

### 8.1 色彩方案（Material 3）

```kotlin
// 主色调 - 温暖橙色系（食欲感）
val primary = Color(0xFFFF6B35)        // 主色
val onPrimary = Color(0xFFFFFFFF)
val primaryContainer = Color(0xFFFFDBC8)
val onPrimaryContainer = Color(0xFF331200)

// 辅助色 - 清新绿色（健康感）
val secondary = Color(0xFF4CAF50)
val onSecondary = Color(0xFFFFFFFF)
val secondaryContainer = Color(0xFFC8E6C9)

// 背景色
val background = Color(0xFFFFFBF5)     // 温暖米白
val surface = Color(0xFFFFFFFF)
val surfaceVariant = Color(0xFFF5F0EA)

// 功能色
val error = Color(0xFFE53935)
val success = Color(0xFF43A047)
val warning = Color(0xFFFFA726)
```

### 8.2 字体规范

```kotlin
// 标题
val headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
val headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
val headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

// 正文
val bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
val bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)

// 标签
val labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
val labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
```

### 8.3 间距 & 圆角

```kotlin
// 间距
val spacing_xs = 4.dp
val spacing_sm = 8.dp
val spacing_md = 16.dp
val spacing_lg = 24.dp
val spacing_xl = 32.dp

// 圆角
val radius_sm = 8.dp
val radius_md = 12.dp
val radius_lg = 16.dp
val radius_xl = 24.dp
val radius_full = 999.dp  // 药丸形
```

### 8.4 图标

使用 Material Symbols Rounded 风格，保持圆润可爱的感觉。

---

## 九、依赖清单（build.gradle.kts）

```kotlin
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.1.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.1.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.1.0")
    implementation("io.github.jan-tennert.supabase:auth-kt:2.1.0")
    implementation("io.github.jan-tennert.supabase:compose-auth:2.1.0")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // Room (local cache)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore (preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // Lottie (animations)
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## 十、开发排期建议

### Phase 1 — 基础框架（1 周）
- [ ] Android 项目初始化（Compose + Hilt + Navigation）
- [ ] Supabase 项目创建 & 表结构初始化
- [ ] Supabase Auth 集成（邮箱注册/登录）
- [ ] 配对功能（生成/输入配对码）
- [ ] 基础 UI 主题

### Phase 2 — 菜品库核心（1.5 周）
- [ ] 自定义菜品 CRUD（含图片上传）
- [ ] 制作方法编辑器
- [ ] 菜品库列表 & 筛选
- [ ] 标签管理
- [ ] Room 本地缓存

### Phase 3 — 全网菜谱（1 周）
- [ ] Spoonacular API 集成
- [ ] 搜索界面 & 结果展示
- [ ] 菜品详情（全网）展示
- [ ] 一键收藏到菜品库
- [ ] 备选 API 集成（聚合数据）

### Phase 4 — 点餐流程（1.5 周）
- [ ] 发起点餐
- [ ] 双人选菜流程
- [ ] Supabase Realtime 实时同步
- [ ] 合并结果展示
- [ ] 确认 & 完成

### Phase 5 — 增强功能（1 周）
- [ ] 随机推荐（转盘 + 摇一摇）
- [ ] 心愿单
- [ ] 历史记录 & 统计
- [ ] 个人偏好设置
- [ ] 避免重复提醒

### Phase 6 — 打磨上线（0.5 周）
- [ ] UI 细节打磨
- [ ] 性能优化
- [ ] 测试 & Bug 修复
- [ ] APK 签名 & 发布

**总计：约 6-7 周**

---

## 附录 A：原型图

所有原型图位于项目 `prototypes/` 目录，SVG 源文件在 `svg/`，PNG 导出在 `png/`。

---

*文档结束*
