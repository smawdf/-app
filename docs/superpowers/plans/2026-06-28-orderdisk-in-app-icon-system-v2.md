# OrderDisk In-App Icon System V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the remaining high-impact in-app emoji/text action icons with Material Icons or the local OrderDisk bowl mark while preserving current app behavior.

**Architecture:** Keep changes inside Compose UI files and existing Android drawable resources. Continue using Material Icons already available in the project, with `ic_orderdisk_bowl` only where a brand mark is better than a generic icon. Do not change ViewModels, repositories, navigation, persistence, networking, signing, or versioning.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android VectorDrawable, existing `material-icons-extended`, Gradle Android build.

---

## File Structure

- Modify `app/src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt`
  - Avatar source actions, pairing/status icons, taste tag remove/add icons, pairing management icon, settings row icons.
- Modify `app/src/main/java/com/myorderapp/ui/profilesetup/ProfileSetupScreen.kt`
  - Profile setup hero and avatar picker icons.
- Modify `app/src/main/java/com/myorderapp/ui/onboarding/OnboardingScreen.kt`
  - Step hero icons, avatar picker icons, step completion icon.
- Modify `app/src/main/java/com/myorderapp/ui/meal/StartMealScreen.kt`
  - Back, active meal header, submitted status, partner marker, dish library heading, remove selected dish, selected status.
- Modify `app/src/main/java/com/myorderapp/ui/meal/MealResultScreen.kt`
  - Result hero/status, section header icons, summary heading, back and confirm action icons.
- Modify `app/src/main/java/com/myorderapp/ui/history/HistoryScreen.kt`
  - History heading, calendar/list segmented controls, top dishes heading.
- Modify `app/src/main/java/com/myorderapp/ui/dishlibrary/DishLibraryScreen.kt`
  - Delete menu leading icon.
- Modify `app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt`
  - Difficulty stars changed from text stars to tappable Material star icons.

Keep category emoji in dish image fallback boxes and meal-type choices, because those are content category markers rather than operation controls.

---

### Task 1: Profile, Profile Setup, And Onboarding Icons

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/profilesetup/ProfileSetupScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: Add icon imports to `ProfileScreen.kt`**

Add these imports after existing Compose imports:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
```

- [ ] **Step 2: Replace avatar sheet text symbols in `ProfileScreen.kt`**

Use this row shape for camera/gallery/link choices:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
    Text("拍照", fontSize = 16.sp)
}
```

Use `Icons.Default.Image` for "从相册选择" and `Icons.Default.Link` for "输入URL".

- [ ] **Step 3: Replace pairing and avatar fallback symbols in `ProfileScreen.kt`**

Use `Icon(Icons.Default.Favorite, ...)` between paired avatars:

```kotlin
Icon(
    Icons.Default.Favorite,
    contentDescription = null,
    tint = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(horizontal = 6.dp).size(28.dp)
)
```

For empty avatar fallback, replace the user emoji fallback with:

```kotlin
Icon(
    Icons.Default.Person,
    contentDescription = null,
    tint = Color.White,
    modifier = Modifier.size(32.dp)
)
```

Keep nickname first-letter fallback when a nickname exists.

- [ ] **Step 4: Replace profile status and settings row symbols**

Use icon+text rows for "已配对" and "尚未配对":

```kotlin
Row(
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp))
    Text("已配对", style = MaterialTheme.typography.labelMedium)
}
```

Change `SettingsRow` signature to:

```kotlin
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String = "", onClick: () -> Unit = {})
```

Inside it, render `Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))` and replace the trailing `Text("›")` with `Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))`.

Call it with:

```kotlin
SettingsRow(Icons.Default.History, "历史记录", "查看过往点餐", onClick = onHistoryClick)
SettingsRow(Icons.Default.Restaurant, "菜品管理", "管理你的菜品库", onClick = onDishManageClick)
SettingsRow(Icons.Default.Info, "关于", "今天吃什么？v$appVersion", onClick = onAboutClick)
SettingsRow(Icons.Default.Logout, "退出登录", "切换账号或离线使用", onClick = { showLogoutDialog = true })
```

- [ ] **Step 5: Replace taste tag and add button symbols in `ProfileScreen.kt`**

Replace tag remove text with:

```kotlin
Icon(
    Icons.Default.Close,
    contentDescription = "移除口味标签",
    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
    modifier = Modifier.size(14.dp)
)
```

Replace the add-tag button text with:

```kotlin
Icon(Icons.Default.Add, contentDescription = "添加口味标签", modifier = Modifier.size(18.dp))
```

- [ ] **Step 6: Update `ProfileSetupScreen.kt` imports and avatar UI**

Add:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
```

Replace the top celebration symbol with `Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))`.

Replace photo-sheet camera/gallery text symbols with 20dp `PhotoCamera` and `Image` icons.

Replace the empty avatar camera symbol with:

```kotlin
Icon(
    Icons.Default.PhotoCamera,
    contentDescription = null,
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.size(34.dp)
)
```

- [ ] **Step 7: Update `OnboardingScreen.kt` imports and hero icons**

Add:

```kotlin
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.res.painterResource
import com.myorderapp.R
```

Replace the step hero `Text` with:

```kotlin
when (uiState.step) {
    1 -> Icon(
        painter = painterResource(R.drawable.ic_orderdisk_bowl),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp)
    )
    2 -> Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
    else -> Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
}
```

Replace `Step2Profile` camera/gallery/link rows with `PhotoCamera`, `Image`, and `Link`. Replace empty avatar symbol with a 34dp `PhotoCamera` icon. Replace `StepDot` completed check text with `Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))`.

- [ ] **Step 8: Compile after profile batch**

Run:

```powershell
rtk .\gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 2: Meal Flow Icons

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/meal/StartMealScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/meal/MealResultScreen.kt`

- [ ] **Step 1: Add imports to `StartMealScreen.kt`**

Add:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ViewList
```

- [ ] **Step 2: Replace StartMeal header/status symbols**

Replace the back `TextButton` content with an icon+text row:

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
    Text("返回", style = MaterialTheme.typography.labelLarge)
}
```

Replace "点菜中" title with `Icon(Icons.Default.Restaurant, ...)` plus text. Replace "菜品库 · 点击添加" with `Icon(Icons.Default.ViewList, ...)` plus text.

For submitted chips, use `Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))` followed by `Text("已提交", ...)`.

For partner name, use `Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))` followed by the partner name text.

- [ ] **Step 3: Replace selected dish remove and selected status**

In `MyDishChip`, replace `Text("✕")` with:

```kotlin
Icon(
    Icons.Default.Close,
    contentDescription = "移除菜品",
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.clickable { onRemove(item.id) }.padding(4.dp).size(16.dp)
)
```

In selected `DishGridCard`, replace "已添加" text symbol with a row containing `Check` and `Text("已添加", ...)`.

- [ ] **Step 4: Add imports and replace icons in `MealResultScreen.kt`**

Add:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
```

Replace the result hero celebration with `Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))`.

Change `SectionHeader` signature to:

```kotlin
private fun SectionHeader(icon: ImageVector, title: String, count: Int, color: Color)
```

Render the icon with `Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))`. Call it with `Icons.Default.Person` for "我的选择" and `Icons.Default.Favorite` for partner choices.

Replace "点餐摘要" heading with a `BarChart` icon and text. Replace action buttons with `ArrowBack` and `Check` icon+text rows.

- [ ] **Step 5: Compile after meal batch**

Run:

```powershell
rtk .\gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 3: History, Dish Library, And Difficulty Icons

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/history/HistoryScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/dishlibrary/DishLibraryScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt`

- [ ] **Step 1: Add history icon imports**

Add:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListBulleted
```

Replace the header and segmented control labels with icon+text rows using `BarChart`, `CalendarMonth`, and `FormatListBulleted`. Replace the Top 5 heading with `EmojiEvents` plus text.

- [ ] **Step 2: Add delete icon to `DishLibraryScreen.kt`**

Add:

```kotlin
import androidx.compose.material.icons.filled.Delete
```

Change the long-press `DropdownMenuItem` to:

```kotlin
DropdownMenuItem(
    leadingIcon = {
        Icon(
            Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
    },
    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
    onClick = { showMenu = false; showDeleteDialog = true }
)
```

- [ ] **Step 3: Add star icon to `AddDishScreen.kt`**

Add:

```kotlin
import androidx.compose.material.icons.filled.Star
```

Replace the difficulty `Text` loop with:

```kotlin
(1..5).forEach { level ->
    Icon(
        Icons.Default.Star,
        contentDescription = "难度 $level",
        tint = if (level <= uiState.difficulty) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
        modifier = Modifier.size(18.dp).clickable { viewModel.onDifficultyChanged(level) }
    )
}
```

- [ ] **Step 4: Compile after final UI batch**

Run:

```powershell
rtk .\gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 4: Final Verification And Commit

**Files:**
- Read: `git status --short --branch`
- Read: `app/build/outputs/apk/debug/app-debug.apk`
- Read: `app/build/outputs/apk/release/app-release.apk`

- [ ] **Step 1: Run unit tests**

Run:

```powershell
rtk .\gradlew.bat testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run compile verification**

Run:

```powershell
rtk .\gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build debug APK**

Run:

```powershell
rtk .\gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Build release APK**

Run:

```powershell
rtk .\gradlew.bat assembleRelease
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Verify APK files**

Run:

```powershell
rtk powershell -NoProfile -Command "Test-Path 'app\build\outputs\apk\debug\app-debug.apk'; Test-Path 'app\build\outputs\apk\release\app-release.apk'"
```

Expected:

```text
True
True
```

- [ ] **Step 6: Stage only relevant files**

Run:

```powershell
rtk git add app/src/main/java/com/myorderapp/ui/profile/ProfileScreen.kt app/src/main/java/com/myorderapp/ui/profilesetup/ProfileSetupScreen.kt app/src/main/java/com/myorderapp/ui/onboarding/OnboardingScreen.kt app/src/main/java/com/myorderapp/ui/meal/StartMealScreen.kt app/src/main/java/com/myorderapp/ui/meal/MealResultScreen.kt app/src/main/java/com/myorderapp/ui/history/HistoryScreen.kt app/src/main/java/com/myorderapp/ui/dishlibrary/DishLibraryScreen.kt app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt docs/superpowers/plans/2026-06-28-orderdisk-in-app-icon-system-v2.md
```

Do not stage `.codegraph/`, `.superpowers/brainstorm/`, `preview/`, `AGENTS.md`, or unrelated 2026-06-16 docs.

- [ ] **Step 7: Commit**

Run:

```powershell
rtk git commit -m "style: refine in-app icon system"
```

Expected: commit succeeds.

---

## Self-Review

- Spec coverage: Profile, ProfileSetup, Onboarding, StartMeal, MealResult, History, DishLibrary, and AddDish difficulty are all covered.
- Scope check: Category emoji in dish fallback boxes and meal-type choices are intentionally preserved as content markers.
- Verification coverage: unit tests, Kotlin compile, debug APK, release APK, and APK existence checks are included.
- Staging boundary: unrelated local files are explicitly excluded.
