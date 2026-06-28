# OrderDisk Icon System Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the most visible inconsistent OrderDisk icons with a custom warm bowl launcher mark and a consistent Material-style in-app icon vocabulary.

**Architecture:** Keep all changes in Android resources and Compose UI files. Use local VectorDrawable assets for the OrderDisk brand mark and existing Material Icons for standard actions, with no new dependency or Figma requirement.

**Tech Stack:** Android VectorDrawable, Jetpack Compose Material 3, material-icons-extended already present in Gradle, Kotlin, Gradle Android packaging.

---

## File Structure

- Modify `app/src/main/res/drawable/ic_launcher_foreground.xml`
  - Custom 108dp warm bowl and paired chopsticks launcher foreground.
- Modify `app/src/main/res/drawable/ic_launcher_background.xml`
  - Warm cream launcher background.
- Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - Adaptive icon wrapper for modern Android launchers.
- Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
  - Round adaptive icon wrapper.
- Create `app/src/main/res/drawable/ic_orderdisk_bowl.xml`
  - 24dp local brand/action mark for Compose screens.
- Modify `app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt`
  - Replace camera/gallery/add/remove/delete text icons with `Icon`.
- Modify `app/src/main/java/com/myorderapp/ui/dishdetail/DishDetailScreen.kt`
  - Replace back/edit/section/wishlist text icons with `Icon`.
- Modify `app/src/main/java/com/myorderapp/ui/wishlist/WishlistScreen.kt`
  - Replace title/action/delete text icons with `Icon`.
- Modify `app/src/main/java/com/myorderapp/ui/about/AboutScreen.kt`
  - Replace large food emoji brand mark with local bowl vector.
- Modify `app/src/main/java/com/myorderapp/ui/auth/AuthScreen.kt`
  - Replace large food emoji brand mark with local bowl vector.

Do not stage `.superpowers/brainstorm`, `.codegraph`, `preview`, `AGENTS.md`, or old unrelated superpowers files.

---

### Task 1: Add Brand Icon Resources

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `app/src/main/res/drawable/ic_orderdisk_bowl.xml`

- [ ] **Step 1: Replace launcher foreground vector**

Use a 108dp vector with a cream tile, bowl, paired chopsticks, food accent, and leaf accent:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#F7EFE4" android:pathData="M18,14h72a10,10 0,0 1,10 10v60a10,10 0,0 1,-10 10h-72a10,10 0,0 1,-10 -10v-60a10,10 0,0 1,10 -10z" />
    <path android:fillColor="#00000000" android:strokeColor="#6B4B33" android:strokeWidth="7" android:strokeLineCap="round" android:pathData="M28,54c8,18 44,18 52,0" />
    <path android:fillColor="#00000000" android:strokeColor="#6B4B33" android:strokeWidth="7" android:strokeLineCap="round" android:pathData="M33,62c3,15 12,24 21,24s18,-9 21,-24" />
    <path android:fillColor="#00000000" android:strokeColor="#B65A43" android:strokeWidth="5.5" android:strokeLineCap="round" android:pathData="M38,31l30,38M70,31L40,69" />
    <path android:fillColor="#D98C45" android:pathData="M54,54m-6,0a6,6 0,1 0,12 0a6,6 0,1 0,-12 0" />
    <path android:fillColor="#00000000" android:strokeColor="#7F9F6A" android:strokeWidth="4" android:strokeLineCap="round" android:pathData="M45,43c4,-7 16,-7 20,0" />
</vector>
```

- [ ] **Step 2: Replace launcher background vector**

Use:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#F7EFE4" android:pathData="M0,0h108v108H0z" />
</vector>
```

- [ ] **Step 3: Add adaptive icon wrappers**

Create both `mipmap-anydpi-v26/ic_launcher.xml` and `mipmap-anydpi-v26/ic_launcher_round.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 4: Add 24dp brand bowl vector**

Create `ic_orderdisk_bowl.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="@android:color/transparent" android:strokeColor="#6B4B33" android:strokeWidth="1.8" android:strokeLineCap="round" android:pathData="M5,11.5c1.6,4.2 12.4,4.2 14,0" />
    <path android:fillColor="@android:color/transparent" android:strokeColor="#6B4B33" android:strokeWidth="1.8" android:strokeLineCap="round" android:pathData="M6.5,14c0.8,3.5 2.8,5 5.5,5s4.7,-1.5 5.5,-5" />
    <path android:fillColor="@android:color/transparent" android:strokeColor="#B65A43" android:strokeWidth="1.5" android:strokeLineCap="round" android:pathData="M8,5l6,8M16,5l-6,8" />
    <path android:fillColor="#D98C45" android:pathData="M12,12m-1.5,0a1.5,1.5 0,1 0,3 0a1.5,1.5 0,1 0,-3 0" />
</vector>
```

- [ ] **Step 5: Compile resources**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 2: Replace Add Dish Action Icons

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt`

- [ ] **Step 1: Add Material icon imports**

Add imports for:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
```

- [ ] **Step 2: Replace camera/gallery dialog text symbols**

Use `Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))` for the camera row and `Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))` for the gallery row.

- [ ] **Step 3: Replace empty image placeholder symbol**

Use:

```kotlin
Icon(
    Icons.Default.PhotoCamera,
    contentDescription = null,
    modifier = Modifier.size(34.dp),
    tint = MaterialTheme.colorScheme.onSurfaceVariant
)
```

- [ ] **Step 4: Replace add/remove/delete controls**

Use `Icon(Icons.Default.Add, ...)` inside the ingredient add `FilledIconButton`, `Icon(Icons.Default.Close, ...)` for ingredient removal, `Icon(Icons.Default.Delete, ...)` for step deletion, and add a leading add icon in the add-step `OutlinedButton`.

- [ ] **Step 5: Compile**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 3: Replace Dish Detail Icons

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/dishdetail/DishDetailScreen.kt`

- [ ] **Step 1: Add Material icon imports**

Add imports for:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
```

- [ ] **Step 2: Replace back and edit text controls**

Use `Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White, modifier = Modifier.size(18.dp))` for back and `Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White, modifier = Modifier.size(18.dp))` for edit.

- [ ] **Step 3: Replace section title emoji**

Use `Row` with `Icon(Icons.Default.Restaurant, ...)` for ingredients and `Icon(Icons.Default.MenuBook, ...)` for cook steps.

- [ ] **Step 4: Replace notes and wishlist button icons**

Use `Icon(Icons.Default.Lightbulb, ...)` in the notes card. In the wishlist button, show `Icons.Default.Check` when added, otherwise `Icons.Default.Favorite`, followed by plain text.

- [ ] **Step 5: Compile**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 4: Replace Wishlist And Brand Header Icons

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/wishlist/WishlistScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/about/AboutScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/auth/AuthScreen.kt`

- [ ] **Step 1: Update wishlist imports and title**

Add Material icon imports for `Check`, `Delete`, `Favorite`, and `Restaurant`. Replace the title emoji with a row containing `Icon(Icons.Default.Favorite, ...)` and the title text.

- [ ] **Step 2: Replace wishlist action text icons**

For pending items, use icon+text rows for "试过了" and "点这个". Use `Icon(Icons.Default.Delete, ...)` in the long-press delete menu.

- [ ] **Step 3: Replace About/Auth large brand emoji**

Add:

```kotlin
import androidx.compose.ui.res.painterResource
import com.myorderapp.R
```

Replace the large food emoji with:

```kotlin
Icon(
    painter = painterResource(R.drawable.ic_orderdisk_bowl),
    contentDescription = null,
    tint = MaterialTheme.colorScheme.primary,
    modifier = Modifier.size(64.dp)
)
```

- [ ] **Step 4: Compile**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 5: Final Build Verification And Commit

**Files:**
- Read: `git status --short --branch`
- Read: APK outputs

- [ ] **Step 1: Run compile verification**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Build debug APK**

Run:

```bash
./gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build release APK**

Run:

```bash
./gradlew.bat assembleRelease
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Verify APK files**

Run:

```bash
Test-Path app/build/outputs/apk/debug/app-debug.apk
Test-Path app/build/outputs/apk/release/app-release.apk
```

Expected:

```text
True
True
```

- [ ] **Step 5: Commit only relevant files**

Stage Android resource/UI files and this plan. Do not stage preview files or unrelated local files:

```bash
git add app/src/main/res/drawable app/src/main/res/mipmap-anydpi-v26 app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt app/src/main/java/com/myorderapp/ui/dishdetail/DishDetailScreen.kt app/src/main/java/com/myorderapp/ui/wishlist/WishlistScreen.kt app/src/main/java/com/myorderapp/ui/about/AboutScreen.kt app/src/main/java/com/myorderapp/ui/auth/AuthScreen.kt docs/superpowers/plans/2026-06-28-orderdisk-icon-system-redesign.md
git commit -m "style: redraw orderdisk icon system"
```

---

## Self-Review

- Spec coverage: Task 1 covers launcher and local brand icon resources. Tasks 2 through 4 cover high-impact emoji/text action icon replacements. Task 5 covers compile, debug build, release build, APK artifact checks, and staging boundaries.
- Marker scan: This plan contains no unfinished markers and no vague instructions without concrete files, code snippets, or commands.
- Scope check: The plan does not add dependencies, change app behavior, redesign layout, or require Figma.
