# 编辑修复 + 创建者昵称 + TheMealDB API · 设计规范

**日期**：2026-05-07
**状态**：已确认

---

## 1. 详情页编辑修复

### 问题
点编辑按钮无反应，导航未触发。

### 方案
- `ADD_DISH` 路由改用标准路径参数 `/add_dish/{editDishId}`
- `editDishId="new"` = 新建模式，其他值 = 编辑模式
- 检查 `DishDetailScreen` 传参链路：`dish.id` → `onEditClick(id)` → `Routes.addDish(id)` → 导航 → AddDishScreen 接收 → `loadDishForEdit()`

---

## 2. 创建者改用昵称

### 当前
```kotlin
// AddDishViewModel.save()
Dish(createdBy = "你创建")  // 写死
```

### 改为
```kotlin
Dish(createdBy = "${state.myName}创建")  // 如"小明创建"
```

- `myName` 已从 Profile 加载（AddDishViewModel.init）
- 详情页已显示 `"${dish.category} · 来自 ${dish.createdBy} · ${dish.createdAt}"`，无需改

---

## 3. TheMealDB API 集成

### API 信息
- Base URL: `https://www.themealdb.com/api/json/v1/1/`
- 免费，key=`1`，无日配额

### 新增文件

| 文件 | 说明 |
|------|------|
| `data/remote/recipe/TheMealDBApi.kt` | Retrofit 接口 |
| `data/remote/recipe/TheMealDBResponse.kt` | 响应 DTO |
| `data/remote/recipe/TheMealDBMapper.kt` | 映射 → Dish |
| `data/remote/recipe/TheMealDBRemoteDataSource.kt` | 数据源 |

### API 接口

```
GET search.php?s=chicken       → 按名称搜索
GET random.php                 → 随机一道菜
GET lookup.php?i=52772         → 按ID查详情
GET categories.php             → 分类列表
GET filter.php?c=Seafood       → 按分类筛选
```

### 搜索流程

```
用户输入 → FoodTranslator 中→英
  → Juhe API（中文原词，100次/天）
  → TheMealDB API（英文翻译词，免费无限）
  → 合并去重 → 中→英回译 → 展示
```

### 改动文件

| 文件 | 改动 |
|------|------|
| `TheMealDBApi.kt` | 新建，Retrofit 接口 |
| `TheMealDBResponse.kt` | 新建，响应 DTO |
| `TheMealDBMapper.kt` | 新建，映射 + 中文回译 |
| `TheMealDBRemoteDataSource.kt` | 新建，数据源 |
| `DualRecipeSearchUseCase.kt` | 加 TheMealDB 并行搜索 |
| `NetworkModule.kt` | 加 TheMealDB Retrofit |
| `AppModule.kt` | 加 TheMealDB DI |
| `AddDishViewModel.kt` | createdBy 改昵称 |
| `NavGraph.kt` | 编辑路由修复 |

---

## 4. 不改变
- UI 页面结构
- 数据模型
- 数据库表结构
