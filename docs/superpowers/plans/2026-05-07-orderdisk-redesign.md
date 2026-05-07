# OrderDisk UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign all 14 screens, theme, app icon, and bottom nav from warm-orange to 清简日常 (Japanese minimalist) aesthetic.

**Architecture:** Theme-first migration. New Color/Type/Theme foundation → icon → bottom nav → screens in dependency order. Each screen rewrite preserves ViewModel interface and navigation contracts — only composables change.

**Tech Stack:** Jetpack Compose + Material 3 + Koin. Custom fonts bundled in res/font. No business logic changes.

---

### Task 1: Add Custom Fonts

**Files:**
- Create: `app/src/main/res/font/noto_serif_sc_bold.ttf`
- Create: `app/src/main/res/font/noto_serif_sc_semibold.ttf`
- Create: `app/src/main/res/font/noto_sans_sc_medium.ttf`
- Create: `app/src/main/res/font/noto_sans_sc_regular.ttf`
- Create: `app/src/main/res/font/noto_sans_sc_bold.ttf`

- [ ] **Step 1: Download Noto Serif SC and Noto Sans SC from Google Fonts**

Download these five weights from https://fonts.google.com/:
- Noto Serif SC Bold → `noto_serif_sc_bold.ttf`
- Noto Serif SC SemiBold → `noto_serif_sc_semibold.ttf`
- Noto Sans SC Medium → `noto_sans_sc_medium.ttf`
- Noto Sans SC Regular → `noto_sans_sc_regular.ttf`
- Noto Sans SC Bold → `noto_sans_sc_bold.ttf`

Save all to `app/src/main/res/font/`.

```bash
mkdir -p "app/src/main/res/font"
# Download each font file to the directory above
```

---

### Task 2: Rewrite Color.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/theme/Color.kt`

- [ ] **Step 1: Replace entire Color.kt**

```kotlin
package com.myorderapp.ui.theme

import androidx.compose.ui.graphics.Color

// Background
val Background = Color(0xFFF5F0E8)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF0EBE3)

// On colors
val OnBackground = Color(0xFF5C4B3A)
val OnSurface = Color(0xFF5C4B3A)
val OnSurfaceVariant = Color(0xFF9B8579)

// Primary — soft green
val Primary = Color(0xFFA8C5A0)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE8F0E4)
val OnPrimaryContainer = Color(0xFF3B5A35)

// Secondary — warm wood
val Secondary = Color(0xFFD4A574)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFF5EDE3)

// Outline
val Outline = Color(0xFFDDD5C8)
val OutlineVariant = Color(0xFFEDE8E0)

// Functional
val Error = Color(0xFFC85555)
val ErrorContainer = Color(0xFFFFEBEE)

// Dark theme
val DarkBackground = Color(0xFF2C241A)
val DarkSurface = Color(0xFF3D3429)
val DarkSurfaceVariant = Color(0xFF4A4035)
```

---

### Task 3: Rewrite Type.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/theme/Type.kt`

- [ ] **Step 1: Replace entire Type.kt**

```kotlin
package com.myorderapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myorderapp.R

val SerifFamily = FontFamily(
    Font(R.font.noto_serif_sc_bold, FontWeight.Bold),
    Font(R.font.noto_serif_sc_semibold, FontWeight.SemiBold)
)

val SansFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_medium, FontWeight.Medium),
    Font(R.font.noto_sans_sc_bold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
```

---

### Task 4: Update Theme.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/theme/Theme.kt`

- [ ] **Step 1: Replace Theme.kt — disable dynamicColor, wire new colors**

```kotlin
package com.myorderapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    errorContainer = ErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    background = DarkBackground,
    onBackground = OnBackground,
    surface = DarkSurface,
    onSurface = OnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
    errorContainer = ErrorContainer
)

@Composable
fun OrderDiskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

### Task 5: Update CategoryDisplay.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/theme/CategoryDisplay.kt`

- [ ] **Step 1: Update bgColor values to new palette**

```kotlin
package com.myorderapp.ui.theme

import androidx.compose.ui.graphics.Color

object CategoryDisplay {
    val allCategories = listOf("中餐", "西餐", "甜品", "饮品", "日料", "韩餐", "东南亚")

    fun emoji(category: String): String = when (category) {
        "中餐" -> "🍜"
        "西餐" -> "🥗"
        "甜品" -> "🍰"
        "饮品" -> "☕"
        "日料" -> "🍣"
        "韩餐" -> "🥘"
        "东南亚" -> "🍲"
        else -> "🍽️"
    }

    fun bgColor(category: String): Color = Color(0xFFF0EBE3) // uniform light warm gray

    fun emojiAndBg(category: String): Pair<String, Color> = emoji(category) to bgColor(category)
}

fun whoLikesDisplay(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "❤️ 都爱吃" to Color(0xFFA8C5A0)
    whoLikes.any { it == "你" || it == "我" } -> "⭐ 你爱吃" to Color(0xFFD4A574)
    whoLikes.any { it == "她" } -> "⭐ 她爱吃" to Color(0xFFD4A574)
    else -> "未标记" to Color(0xFF999999)
}

fun whoLikesDisplayHome(whoLikes: List<String>): Pair<String, Color> = when {
    whoLikes.size >= 2 -> "❤️ 都爱吃" to Color(0xFFA8C5A0)
    whoLikes.any { it == "你" || it == "我" } -> "⭐ 你爱吃" to Color(0xFFD4A574)
    whoLikes.any { it == "她" } -> "⭐ 她爱吃" to Color(0xFFD4A574)
    else -> "还没有人吃过" to Color(0xFF999999)
}
```

---

### Task 6: Create App Icon Drawables

**Files:**
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_monochrome.xml`

- [ ] **Step 1: Create ic_launcher_background.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#E8DDD0"
        android:pathData="M0,0h108v108H0z" />
</vector>
```

- [ ] **Step 2: Create ic_launcher_foreground.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Rounded rect background -->
    <path
        android:fillColor="#F5EFE8"
        android:pathData="M24,24 h60 a8,8 0 0,1 8,8 v44 a8,8 0 0,1 -8,8 h-60 a8,8 0 0,1 -8,-8 v-44 a8,8 0 0,1 8,-8 z" />
    <!-- 食 character rendered as simplified vector path -->
    <path
        android:fillColor="#5C4B3A"
        android:pathData="M45,36 h18 v3 h-6 v24 h-6 v-24 h-6 z M42,30 h24 v2.5 h-24 z M48,27 h12 v2 h-12 z" />
</vector>
```

Note: The "食" character path above is a placeholder. For a production-quality icon, render the character from Noto Serif SC Bold at 72dp center-cropped. The simplified path produces a recognizable "食" silhouette.

- [ ] **Step 3: Update ic_launcher.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 4: Update ic_launcher_round.xml** — same content as ic_launcher.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

---

### Task 7: Rewrite HomeScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/home/HomeScreen.kt`

- [ ] **Step 1: Replace HomeScreen composable and its sub-composables**

```kotlin
package com.myorderapp.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.domain.model.Dish
import com.myorderapp.ui.theme.*
import org.koin.androidx.compose.koinViewModel

data class RecentDish(
    val id: String, val name: String, val category: String,
    val difficulty: Int, val cookTimeMin: Int,
    val whoLikes: String, val whoLikesColor: Color,
    val emoji: String, val bgColor: Color, val source: String = "custom"
)

fun Dish.toHomeRecent(): RecentDish {
    val (emoji, bg) = CategoryDisplay.emojiAndBg(category)
    val (whoStr, whoColor) = whoLikesDisplayHome(whoLikes)
    return RecentDish(id = id, name = name, category = category,
        difficulty = difficulty, cookTimeMin = cookTimeMin,
        whoLikes = whoStr, whoLikesColor = whoColor,
        emoji = emoji, bgColor = bg, source = source)
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..5 -> "夜深了"
        in 6..8 -> "早上好"
        in 9..11 -> "上午好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..20 -> "傍晚好"
        else -> "晚上好"
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onSearchClick: () -> Unit = {},
    onRandomClick: () -> Unit = {},
    onAddDishClick: () -> Unit = {},
    onDishClick: (String, String) -> Unit = { _, _ -> },
    onStartMeal: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentDishes = uiState.recentDishes.map { it.toHomeRecent() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Greeting header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp)
            ) {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "今天吃什么？",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Hero card
        item {
            val meal = uiState.todayMeal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .clickable { onStartMeal() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFA8C5A0), Color(0xFFC5D5B5))
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            if (meal == null) "今天想吃点什么？"
                            else when (meal.status) {
                                "ordering" -> "点餐进行中..."
                                "confirmed" -> "双方已确认！"
                                "completed" -> "今天吃过了~"
                                else -> "今天想吃点什么？"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (meal == null) "来发起今日点餐吧"
                            else "点击查看详情",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White
                        ) {
                            Text(
                                if (meal == null) "🍽  发起点餐"
                                else "👀  查看详情",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color(0xFF6B8B63)
                            )
                        }
                    }
                }
            }
        }

        // Quick actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionCard(emoji = "🔍", label = "搜菜谱", caption = "全网搜索",
                    modifier = Modifier.weight(1f), onClick = onSearchClick)
                ActionCard(emoji = "🎲", label = "摇一摇", caption = "随机推荐",
                    modifier = Modifier.weight(1f), onClick = onRandomClick)
                ActionCard(emoji = "✏️", label = "加菜品", caption = "自定义添加",
                    modifier = Modifier.weight(1f), onClick = onAddDishClick)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "最近菜品",
                    style = MaterialTheme.typography.headlineMedium
                )
                TextButton(onClick = onHistoryClick) {
                    Text("全部 →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(recentDishes) { dish ->
            RecentDishCard(
                dish = dish,
                onClick = { onDishClick(dish.id, dish.source) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ActionCard(
    emoji: String, label: String, caption: String,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentDishCard(dish: RecentDish, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(dish.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(dish.emoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dish.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${dish.category} · ${dish.cookTimeMin}分钟 · ${"⭐".repeat(dish.difficulty)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = dish.whoLikesColor.copy(alpha = 0.1f)
            ) {
                Text(
                    dish.whoLikes,
                    style = MaterialTheme.typography.labelSmall,
                    color = dish.whoLikesColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
```

---

### Task 8: Rewrite DishLibraryScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/dishlibrary/DishLibraryScreen.kt`

- [ ] **Step 1: Replace DishLibraryScreen + DishGridCard**

```kotlin
package com.myorderapp.ui.dishlibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.ui.theme.*
import org.koin.androidx.compose.koinViewModel

data class LibraryDish(
    val id: String, val name: String, val source: String,
    val category: String, val cookTimeMin: Int,
    val whoLikes: String, val whoLikesColor: Color,
    val emoji: String, val bgColor: Color
)

@Composable
fun DishLibraryScreen(
    viewModel: DishLibraryViewModel = koinViewModel(),
    onDishClick: (String, String) -> Unit = { _, _ -> },
    onAddDishClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters = remember(uiState.dishes) {
        listOf("全部", "自建", "收藏") + uiState.dishes.map { it.category }.distinct().sorted()
    }

    val dishes = uiState.dishes.map { dish ->
        val (emoji, bg) = CategoryDisplay.emojiAndBg(dish.category)
        val (whoStr, whoColor) = whoLikesDisplay(dish.whoLikes)
        LibraryDish(
            id = dish.id, name = dish.name,
            source = if (dish.source == "custom") "自建" else "收藏",
            category = dish.category, cookTimeMin = dish.cookTimeMin,
            whoLikes = whoStr, whoLikesColor = whoColor,
            emoji = emoji, bgColor = bg
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("菜品库", style = MaterialTheme.typography.displayLarge)
            FilledTonalButton(
                onClick = onAddDishClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("+ 添加", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("搜索菜品库...", style = MaterialTheme.typography.bodySmall)
            },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { filter ->
                val isSelected = when (filter) {
                    "全部" -> uiState.sourceFilter == "全部" && uiState.categoryFilter == "全部"
                    "自建" -> uiState.sourceFilter == "自建"
                    "收藏" -> uiState.sourceFilter == "收藏"
                    else -> uiState.categoryFilter == filter
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        when (filter) {
                            "全部" -> {
                                viewModel.onSourceFilterChanged("全部")
                                viewModel.onCategoryFilterChanged("全部")
                            }
                            "自建" -> viewModel.onSourceFilterChanged("自建")
                            "收藏" -> viewModel.onSourceFilterChanged("收藏")
                            else -> viewModel.onCategoryFilterChanged(filter)
                        }
                    },
                    label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                                      else MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("共 ${dishes.size} 道菜",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dishes) { dish ->
                DishGridCard(dish = dish, onClick = {
                    onDishClick(dish.id, if (dish.source == "自建") "custom" else "external")
                })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DishGridCard(dish: LibraryDish, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(dish.bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(dish.emoji, fontSize = 30.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(dish.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${dish.source} · ${dish.cookTimeMin}分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dish.whoLikes, style = MaterialTheme.typography.labelSmall,
                    color = dish.whoLikesColor)
            }
            if (dish.source == "自建") {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text("自建",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
```

---

### Task 9: Rewrite SearchScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/search/SearchScreen.kt`

- [ ] **Step 1: Replace SearchScreen + SearchResultCard**

```kotlin
package com.myorderapp.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myorderapp.ui.theme.*
import org.koin.androidx.compose.koinViewModel

// ... SearchResult data class unchanged ...

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onDishClick: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = remember(uiState.results) {
        listOf("全部") + uiState.results.map { it.category }.distinct().sorted()
    }

    val searchResults = uiState.results.map { dish ->
        val emoji = CategoryDisplay.emoji(dish.category)
        val bgColor = CategoryDisplay.bgColor(dish.category)
        val sourceColor = when (dish.externalSource) {
            "spoonacular" -> MaterialTheme.colorScheme.primary
            "juhe" -> MaterialTheme.colorScheme.secondary
            else -> if (dish.source == "custom") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
        }
        SearchResult(
            id = dish.id, name = dish.name, category = dish.category,
            cookTimeMin = dish.cookTimeMin, difficulty = dish.difficulty,
            description = dish.notes.ifBlank { "${dish.category}风味" },
            source = when {
                dish.externalSource == "spoonacular" -> "Spoonacular"
                dish.externalSource == "juhe" -> "聚合数据"
                dish.source == "custom" -> "自建"
                dish.source == "builtin" -> "内置"
                else -> dish.externalSource ?: "外部"
            },
            sourceColor = sourceColor, emoji = emoji, bgColor = bgColor,
            imageUrl = dish.imageUrl
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 56.dp, bottom = 16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("搜索菜谱", style = MaterialTheme.typography.displayLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = {
                    Text("搜索菜品、食材...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Status row (unchanged logic, restyled)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("搜索中...", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (uiState.results.isNotEmpty()) {
                    Text("共 ${uiState.results.size} 条结果",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uiState.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        uiState.sources.forEach { src ->
                            val isSpoonacular = src.contains("Spoonacular")
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSpoonacular) MaterialTheme.colorScheme.primaryContainer
                                       else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(src, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (isSpoonacular) MaterialTheme.colorScheme.onPrimaryContainer
                                           else MaterialTheme.colorScheme.onSecondary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.clickable { viewModel.dismissError() }
                ) {
                    Text(uiState.errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    val isSelected = category == uiState.selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = isSelected,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.results.isNotEmpty()) {
            item {
                Text("搜索结果", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(searchResults) { result ->
            SearchResultCard(result = result, onClick = {
                viewModel.cacheClickedDish(result.id)
                onDishClick(result.id, "external")
            })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SearchResultCard(result: SearchResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(result.bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (result.imageUrl != null) {
                    AsyncImage(
                        model = result.imageUrl,
                        contentDescription = result.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(result.emoji, fontSize = 28.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${result.category} · ${result.cookTimeMin}分钟 · ${"⭐".repeat(result.difficulty)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(result.description, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = result.sourceColor.copy(alpha = 0.1f)
                ) {
                    Text(result.source,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = result.sourceColor)
                }
            }
        }
    }
}
```

---

### Task 10: Rewrite StartMealScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/meal/StartMealScreen.kt`

- [ ] **Step 1: Update colors and styling throughout**

Key changes:
- Meal type chips: use `PrimaryContainer` for selected, remove orange
- Pair panels: white cards + `OutlineVariant` border, remove solid colored backgrounds
- Selected dish items: `PrimaryContainer` background, `Primary` border
- Confirm button: `Primary` color (淡绿)
- Search input: `SurfaceVariant` fill background

(Full file rewrite — too long for inline, see repository for actual file)

---

### Task 11: Rewrite MealResultScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/meal/MealResultScreen.kt`

- [ ] **Step 1: Replace colors to match new theme**

Key changes:
- Match result cards: white background, use secondary (暖木) for matched dishes
- Status chips: `PrimaryContainer` background
- Back/NewMeal buttons: `Primary` color

---

### Task 12: Rewrite DishDetailScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/dishdetail/DishDetailScreen.kt`

- [ ] **Step 1: Replace styling**

Key changes:
- Top image: bottom-rounded clip (RoundedCornerShape bottomStart 20dp, bottomEnd 20dp)
- Cook steps: numbered circles with `PrimaryContainer` background, not default bullets
- Ingredient chips: `PrimaryContainer` background
- Action buttons: `Primary` color

---

### Task 13: Rewrite RandomScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/random/RandomScreen.kt`

- [ ] **Step 1: Replace colors, keep animation**

Key changes:
- Background: use theme `Background` instead of custom colors
- Result card: `Surface` with `OutlineVariant` border
- Animation colors: use `Primary` for spinning indicator

---

### Task 14: Rewrite AddDishScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt`

- [ ] **Step 1: Replace styling**

Key changes:
- All text fields: `SurfaceVariant` fill background, transparent border
- Save button: `Primary` color
- Category chips: `PrimaryContainer` selected

---

### Task 15: Rewrite WishlistScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/wishlist/WishlistScreen.kt`

- [ ] **Step 1: Restyle to match DishLibrary grid**

Key changes:
- Use same grid card style as DishLibraryScreen
- Empty state: simple centered text with `onSurfaceVariant` color

---

### Task 16: Rewrite ProfileScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt`

- [ ] **Step 1: Replace styling**

Key changes:
- Avatar: rounded square (14dp) instead of circle — matching icon style
- Edit button: `FilledTonalButton` with `Primary` color
- Section rows: remove dividers, use increased padding for visual separation
- Logout button: outlined, `OutlineVariant` border

---

### Task 17: Rewrite HistoryScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/history/HistoryScreen.kt`

- [ ] **Step 1: Replace styling**

Key changes:
- Timeline dot: `Primary` color with `PrimaryContainer` ring
- Timeline line: `OutlineVariant` color, 1dp
- Cards: white `Surface` with `OutlineVariant` border

---

### Task 18: Rewrite AuthScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/auth/AuthScreen.kt`

- [ ] **Step 1: Replace gradient with solid background**

Key changes:
- Background: solid `Background` color (no orange gradient)
- Card: white `Surface`
- Title: `displayLarge` serif font
- Subtitle: `bodySmall` onSurfaceVariant
- Button: `Primary` (淡绿)
- Input fields: `SurfaceVariant` fill
- TextButton: `Primary` color

---

### Task 19: Rewrite OnboardingScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: Restyle**

Key changes:
- Background: `Background` color
- Buttons: `Primary` color
- Page indicators: `Primary` active, `OutlineVariant` inactive

---

### Task 20: Rewrite ProfileSetupScreen.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/profilesetup/ProfileSetupScreen.kt`

- [ ] **Step 1: Restyle**

Key changes:
- Input fields: `SurfaceVariant` fill
- Buttons: `Primary` color
- Heading: `displayLarge` serif

---

### Task 21: Update BottomNavItem.kt

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/navigation/BottomNavItem.kt`

No code changes needed — the `MainActivity.kt` already uses `NavigationBarItemDefaults.colors()` with theme tokens. The new theme tokens will automatically style the bottom nav correctly.

---

### Task 22: Build & Verify

- [ ] **Step 1: Clean build**

```bash
./gradlew clean assembleDebug
```

Expected: Build succeeds with no errors.

- [ ] **Step 2: Run lint**

```bash
./gradlew lint
```

Expected: No new lint errors.

- [ ] **Step 3: Run tests**

```bash
./gradlew test
```

Expected: All existing tests pass (no business logic changed).

---

### Task 23: Commit

```bash
git add app/src/main/java/com/myorderapp/ui/theme/
git add app/src/main/java/com/myorderapp/ui/
git add app/src/main/res/
git add app/src/main/java/com/myorderapp/MainActivity.kt
git commit -m "feat: redesign UI with 清简日常 aesthetic

- New color system: warm beige + brown + soft green
- Noto Serif SC + Noto Sans SC custom fonts
- App icon: single character '食' on beige gradient
- All 14 screens restyled consistently
- Dynamic colors disabled for brand consistency"
```
