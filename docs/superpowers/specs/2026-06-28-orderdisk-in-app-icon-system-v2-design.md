# OrderDisk App 内图标系统二次优化设计

## 目标

在上一轮完成启动图标和高频页面图标替换后，继续统一 App 内部的操作图标、状态图标和品牌入口图标，让 OrderDisk 的界面从“局部统一”推进到“全局更一致”。

本轮选择方案 B：统一系统版。它会替换主要页面中仍然作为按钮、状态或入口使用的 emoji 和文字符号，同时保留菜品分类 emoji 作为内容占位和分类气质，不做过度清洗。

## 设计原则

- 操作图标使用 Material Icons：返回、删除、拍照、相册、确认、日历、餐盘、配对、个人资料等。
- 品牌图标继续复用本地 `ic_orderdisk_bowl`：用于 Auth、About 之外需要品牌气质的场景。
- 菜品分类 emoji 暂时保留：它们是内容分类标识，不是操作控件，保留后仍能维持轻松、日常的点菜感。
- 不改页面结构和业务行为：只替换图标表达、少量对齐和图标容器，不改变 ViewModel、仓库、导航或同步逻辑。
- 不新增依赖：继续使用项目已有 Compose Material icon 依赖和本地 VectorDrawable。

## 当前残留问题

代码扫描后仍有这些主要符号图标残留：

- `ProfileScreen`：拍照、相册、头像 fallback、配对心形、已配对标题。
- `ProfileSetupScreen`：拍照和相册入口。
- `OnboardingScreen`：拍照、相册、头像占位。
- `StartMealScreen`：返回、点菜中、已提交、双方头像/身份标识。
- `MealResultScreen`：返回。
- `HistoryScreen`：日历入口。
- `DishLibraryScreen`：删除菜单。
- `AddDishScreen`：难度星级仍是文本星号。

这些元素一部分是按钮，一部分是状态标识。它们会随系统 emoji 字体变化，容易和上一轮新图标体系混在一起显得不稳定。

## 本轮范围

### Profile

替换拍照和相册入口为 `PhotoCamera` 和 `Image` 图标。

将配对心形和已配对标题替换为 `Favorite` 或合适的配对语义图标。头像 fallback 可以使用 `Person` 图标，但用户昵称首字母 fallback 可保留，因为它是数据内容展示。

### Profile Setup 与 Onboarding

替换头像上传相关的拍照、相册、头像占位图标。头像占位优先使用 `Person`，拍照入口使用 `PhotoCamera`，相册入口使用 `Image`。

### Meal Flow

`StartMealScreen` 的返回改为 `ArrowBack`，点菜中标题使用餐盘或本地碗图标，已提交状态使用 `Check`。双方用户标识用 `Person` 或已有头像内容，不再在状态文本里混用 emoji。

`MealResultScreen` 的返回改为 `ArrowBack` 图标加文字或纯图标按钮，保持当前交互位置。

### History 与 Dish Library

`HistoryScreen` 的日历入口改为 `CalendarMonth` 或可用的日历图标。

`DishLibraryScreen` 的删除菜单改为 `DropdownMenuItem` 的 `leadingIcon = Delete`，文本只保留“删除”。

### Add Dish Difficulty

将难度控件从文本 `⭐` / `☆` 换成 Material 星标图标。选中状态使用主色，未选中状态使用 outline 或 onSurfaceVariant 降透明度，点击行为保持不变。

## 非目标

- 不替换所有菜品分类 emoji。
- 不重做首页、底部导航、卡片布局或配色系统。
- 不引入 Figma、付费图标、第三方图标包或新依赖。
- 不改变数据库、Supabase、Room、Retrofit、上传、同步、登录配对等业务逻辑。
- 不改版本号、签名配置和包名。

## 验收标准

- 上述页面中作为操作控件或状态控件的 emoji/文字符号被 Material Icon 或本地碗图标替换。
- 菜品分类 emoji 仍可作为内容占位存在，但不再出现在新增替换范围的按钮和菜单动作里。
- 主要图标使用一致的尺寸：小按钮 16dp 到 20dp，标题图标 20dp 到 24dp，大品牌图标 56dp 到 64dp。
- 关键动作图标有合适的 `contentDescription`，纯装饰图标使用 `null`。
- `compileDebugKotlin`、`testDebugUnitTest`、`assembleDebug`、`assembleRelease` 全部通过。
- Debug 与 Release APK 产物存在。

## 实施策略

按页面分批替换，每批后跑一次 Kotlin 编译：

1. Profile、ProfileSetup、Onboarding：头像和图片入口图标统一。
2. StartMeal、MealResult：点菜流程图标统一。
3. History、DishLibrary、AddDish 难度：补齐列表、菜单和评分控件。
4. 最终运行完整验证并提交。

## 风险与处理

- 某些 Material 图标可能在当前依赖中没有可用导出。处理方式是换用语义接近的已可用图标，不新增依赖。
- 头像首字母 fallback 和分类 emoji 若全部替换，会降低个性化和内容辨识度。本轮只替换操作和状态图标。
- Compose 中小尺寸 icon 若触控面积太小，应保留原按钮或 Surface 点击范围，只替换内部视觉元素。

## 自检

- 无占位符或未定范围。
- 设计范围和方案 B 一致，没有扩大到全量去 emoji。
- 验收标准包含编译、测试、Debug APK、Release APK。
- 明确区分了操作/状态图标和内容分类 emoji，避免实现时产生歧义。
