# OrderDisk UI 重设计 · 设计规范

**日期**：2026-05-07
**状态**：已确认
**设计方向**：清简日常（日系极简 · 温暖克制）

---

## 1. 设计理念

将"今天吃什么？"从一个功能型工具 App 升级为一个有温度、有质感的二人专属美食空间。

**关键词**：克制、温润、留白、质感

**用户**：一对情侣，日常高频使用。界面需要"久看不腻"，避免视觉疲劳。

---

## 2. 色彩系统

从现有暖橙（`#FF6B35`）全面转向以**中性暖调**为主的克制品色系。

| Token | 颜色 | 用途 |
|-------|------|------|
| Background | `#F5F0E8` (米白) | 页面底色 |
| Surface | `#FFFFFF` (纯白) | 卡片、弹窗背景 |
| OnBackground/OnSurface | `#5C4B3A` (深棕) | 主文字 |
| OnSurfaceVariant | `#9B8579` (暖灰) | 辅助文字、说明 |
| Primary | `#A8C5A0` (淡绿) | 主按钮、选中态、强调 |
| OnPrimary | `#FFFFFF` | 主按钮文字 |
| PrimaryContainer | `#E8F0E4` (浅绿底) | 标签、chip 背景 |
| Secondary | `#D4A574` (暖木) | 次要强调、收藏/喜欢 |
| Outline | `#DDD5C8` (浅边框) | 分割线、Card 边框 |
| SurfaceVariant | `#F0EBE3` (暖灰底) | 输入框背景、chip |

**删除**：Android 12+ 动态颜色（`dynamicColor = false`），保持品牌一致性。

---

## 3. 字体系统

| 层级 | 字体 | 大小 | 用途 |
|------|------|------|------|
| Display | Noto Serif SC Bold | 28sp | 页面大标题 |
| Headline | Noto Serif SC SemiBold | 20sp | 卡片标题、Section |
| Title | Noto Sans SC Medium | 16sp | 列表标题 |
| Body | Noto Sans SC Regular | 14sp | 正文 |
| Caption | Noto Sans SC Regular | 12sp | 说明文字 |
| Label | Noto Sans SC Medium | 11sp | Chip、标签 |

注：Noto Serif SC 和 Noto Sans SC 均为 Google Fonts 可下载的免费字体，通过 `downloadableFonts` 或打包 `res/font` 引入。

---

## 4. 页面改造清单

### 4.1 首页（HomeScreen）

**当前问题**：橙色渐变 Banner 过于浓重，emoji 卡片幼稚，整体缺乏层次。

**改造**：
- 顶部：去除白色 Header 背景，直接融入米色底色。时段问候 + serif 标题「今天吃什么？」
- Hero 卡片：淡绿渐变（`#A8C5A0 → #C5D5B5`），半径 16dp，内容与现在类似但颜色克制
- 快捷操作：三张卡片去掉纯色背景，统一白色 Card + 浅边框，emoji 保留但缩小
- 最近菜品：卡片加细边框，emoji 缩略图背景统一为 `#F0EBE3`
- 整体：增大留白，卡片间距 12dp+

### 4.2 菜品库（DishLibraryScreen）

**改造**：
- 标题用 Noto Serif SC
- 搜索框用 `SurfaceVariant` 填充背景，去掉 Material 默认的 Outlined 样式
- 网格卡片：统一白底，去掉纯色 emoji 背景，改用浅灰 `#F0EBE3` 或浅绿 `#E8F0E4`
- 「自建」标签颜色改为淡绿

### 4.3 搜索（SearchScreen）

**改造**：
- 返回按钮改回箭头 Icon（非 Text "←"）
- 搜索框风格统一为填充式
- 结果卡片与首页 RecentDishCard 风格统一
- Source 标签（聚合数据/Spoonacular）颜色收敛为淡绿 vs 暖木

### 4.4 点餐（StartMealScreen + MealResultScreen）

**改造**：
- 双人面板用白色卡片 + 浅边框区分，去掉纯色背景
- 「已选」状态用淡绿容器色
- 确认按钮用 Primary 淡绿

### 4.5 心愿单（WishlistScreen）

**改造**：
- 与菜品库网格风格统一
- 空状态插图简化

### 4.6 个人中心（ProfileScreen）

**改造**：
- 头像用圆角方形（与图标统一）
- 编辑按钮用淡绿
- 减少分割线，用留白区分区域

### 4.7 详情页（DishDetailScreen）

**改造**：
- 顶部图片改为圆角底部裁切
- 菜谱步骤用数字圆点而非默认列表样式
- 食材标签用 `PrimaryContainer` 背景

### 4.8 随机（RandomScreen）

**改造**：
- 动画保留，背景颜色克制
- 结果卡片与整体风格统一

### 4.9 历史记录（HistoryScreen）

**改造**：
- 时间线用细线 + 小圆点，颜色淡绿
- 卡片风格统一

### 4.10 添加菜品（AddDishScreen）

**改造**：
- 输入框统一 `SurfaceVariant` 背景
- 保存按钮用淡绿

### 4.11 登录/注册（AuthScreen + OnboardingScreen + ProfileSetupScreen）

**改造**：
- 渐变背景改为米色系纯色 + 淡绿点缀
- 卡片白色
- 按钮淡绿
- 整体干净舒适

---

## 5. 应用图标

- **设计**：米色渐变圆角方形底 + 衬线体「食」字
- **形状**：圆角矩形（与 Android adaptive icon 规范兼容）
- **背景**：`#E8DDD0 → #F5EFE8` 渐变
- **前景**：「食」字，颜色 `#5C4B3A`，字体 Noto Serif SC Bold
- **交付**：替换 `res/mipmap-*` 下所有 `ic_launcher` 和 `ic_launcher_round`
- **Adaptive Icon**：更新 `ic_launcher_foreground` + `ic_launcher_background` drawable

---

## 6. 底部导航

- 保持 5 个 Tab 不变：首页、菜品库、点餐、心愿单、我的
- 图标：保留当前 Material Icons，颜色跟随 Primary（淡绿）
- 选中态指示器：`PrimaryContainer` (淡绿底)
- 标签文字：Noto Sans SC 11sp

---

## 7. 动效

- 页面切换：保留 Compose 默认过渡
- 卡片点击：轻微缩放（0.97x）+ 水波纹
- 列表加载：FadeIn + 错位（staggerDelay 30ms/item）
- 随机摇一摇：保留现有旋转动画，改配色

---

## 8. 不改变的内容

- MVVM 架构、ViewModel、Repository 层
- 导航结构和路由
- 数据模型（Dish, Meal, Profile 等）
- Koin DI 配置
- 所有业务逻辑

---

## 9. 文件变更范围

```
app/src/main/java/com/myorderapp/ui/theme/
  ├── Color.kt           ← 全面重写
  ├── Type.kt            ← 引入 Noto Serif SC + Noto Sans SC
  ├── Theme.kt           ← dynamicColor = false
  └── CategoryDisplay.kt ← 调整背景色

app/src/main/java/com/myorderapp/ui/
  ├── home/HomeScreen.kt
  ├── dishlibrary/DishLibraryScreen.kt
  ├── search/SearchScreen.kt
  ├── dishdetail/DishDetailScreen.kt
  ├── meal/StartMealScreen.kt
  ├── meal/MealResultScreen.kt
  ├── random/RandomScreen.kt
  ├── adddish/AddDishScreen.kt
  ├── wishlist/WishlistScreen.kt
  ├── history/HistoryScreen.kt
  ├── profile/ProfileScreen.kt
  ├── auth/AuthScreen.kt
  ├── onboarding/OnboardingScreen.kt
  ├── profilesetup/ProfileSetupScreen.kt
  └── navigation/
      ├── BottomNavItem.kt
      └── NavGraph.kt (不改变)

app/src/main/res/
  ├── mipmap-*/ic_launcher*.webp   ← 替换
  ├── mipmap-anydpi-v26/ic_launcher*.xml  ← 更新引用
  ├── drawable/ic_launcher_foreground.xml ← 重做
  └── drawable/ic_launcher_background.xml ← 重做
```
