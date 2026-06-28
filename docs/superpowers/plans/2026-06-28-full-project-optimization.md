# OrderDisk Full Project Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stabilize OrderDisk's current Room/Paging/Supabase upgrade, improve save failure handling, add dish-library loading states, and align docs with the current architecture.

**Architecture:** Add small pure helpers for dish merge policy and add-dish save error text so risky behavior has JVM tests. Wire the helpers into `HybridDishRepository` and `AddDishViewModel`, then make the dish library's Paging UI show loading, empty, and error states clearly.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Room, Paging 3, Koin, Supabase Kotlin SDK, JUnit 4, Gradle.

---

## File Structure

- Create: `app/src/main/java/com/myorderapp/data/repository/DishMergePolicy.kt`
  - Pure merge rule for local/cloud dish lists.
- Create: `app/src/test/java/com/myorderapp/data/repository/DishMergePolicyTest.kt`
  - JVM tests for logged-in/logged-out merge behavior and duplicate handling.
- Create: `app/src/main/java/com/myorderapp/ui/adddish/AddDishSaveMessages.kt`
  - Pure error-message helper used by add/edit dish save.
- Create: `app/src/test/java/com/myorderapp/ui/adddish/AddDishSaveMessagesTest.kt`
  - JVM tests for readable add/edit failure messages.
- Modify: `app/src/main/java/com/myorderapp/data/repository/HybridDishRepository.kt`
  - Use `DishMergePolicy`, observe login state in merged flow, and clear cloud cache on logout sync.
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishViewModel.kt`
  - Catch primary save failures, reset saving state, keep image upload non-blocking.
- Modify: `app/src/main/java/com/myorderapp/ui/dishlibrary/DishLibraryScreen.kt`
  - Add icons, empty state, refresh error state, append error state, stable card dimensions.
- Modify: `docs/TECH_UPGRADE_PLAN.md`
  - Add current-status note so docs stop describing the project as JSON/Retrofit-Supabase only.

## Task 1: Dish Merge Policy

**Files:**
- Create: `app/src/test/java/com/myorderapp/data/repository/DishMergePolicyTest.kt`
- Create: `app/src/main/java/com/myorderapp/data/repository/DishMergePolicy.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/com/myorderapp/data/repository/DishMergePolicyTest.kt`:

```kotlin
package com.myorderapp.data.repository

import com.myorderapp.domain.model.Dish
import org.junit.Assert.assertEquals
import org.junit.Test

class DishMergePolicyTest {
    @Test
    fun `logged out merge excludes cloud dishes`() {
        val local = listOf(dish("local-1", "番茄炒蛋", "builtin"))
        val cloud = listOf(dish("cloud-1", "云端红烧肉", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = false)

        assertEquals(listOf("local-1"), merged.map { it.id })
    }

    @Test
    fun `logged in merge keeps cloud first and local fallback`() {
        val local = listOf(dish("local-1", "番茄炒蛋", "builtin"))
        val cloud = listOf(dish("cloud-1", "云端红烧肉", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = true)

        assertEquals(listOf("cloud-1", "local-1"), merged.map { it.id })
    }

    @Test
    fun `logged in merge deduplicates by normalized name with cloud winning`() {
        val local = listOf(dish("local-1", " 番茄炒蛋 ", "builtin"))
        val cloud = listOf(dish("cloud-1", "番茄炒蛋", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = true)

        assertEquals(listOf("cloud-1"), merged.map { it.id })
    }

    @Test
    fun `blank dish names fall back to id for duplicate key`() {
        val local = listOf(dish("local-1", "", "builtin"))
        val cloud = listOf(dish("cloud-1", "", "custom"))

        val merged = DishMergePolicy.merge(local, cloud, includeCloud = true)

        assertEquals(listOf("cloud-1", "local-1"), merged.map { it.id })
    }

    private fun dish(id: String, name: String, source: String): Dish =
        Dish(id = id, name = name, source = source)
}
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests com.myorderapp.data.repository.DishMergePolicyTest
```

Expected: FAIL because `DishMergePolicy` does not exist.

- [ ] **Step 3: Add the merge policy**

Create `app/src/main/java/com/myorderapp/data/repository/DishMergePolicy.kt`:

```kotlin
package com.myorderapp.data.repository

import com.myorderapp.domain.model.Dish

object DishMergePolicy {
    fun merge(
        local: List<Dish>,
        cloud: List<Dish>,
        includeCloud: Boolean
    ): List<Dish> {
        val merged = mutableListOf<Dish>()
        val seen = mutableSetOf<String>()

        if (includeCloud) {
            cloud.forEach { dish ->
                if (seen.add(dish.mergeKey())) merged.add(dish)
            }
        }

        local.forEach { dish ->
            if (seen.add(dish.mergeKey())) merged.add(dish)
        }

        return merged
    }

    private fun Dish.mergeKey(): String {
        val normalizedName = name.trim().lowercase()
        return normalizedName.ifBlank { "id:${id.trim().lowercase()}" }
    }
}
```

- [ ] **Step 4: Run the focused test and confirm it passes**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests com.myorderapp.data.repository.DishMergePolicyTest
```

Expected: PASS.

## Task 2: Wire Login-Aware Dish Merge

**Files:**
- Modify: `app/src/main/java/com/myorderapp/data/repository/HybridDishRepository.kt`

- [ ] **Step 1: Replace `getAllDishes()` and `syncFromCloud()`**

Replace the current implementations with:

```kotlin
override fun getAllDishes(): Flow<List<Dish>> {
    return combine(
        localRepo.getAllDishes(),
        cloudRepo.getAllDishes(),
        session.isLoggedIn
    ) { local, cloud, isLoggedIn ->
        DishMergePolicy.merge(
            local = local,
            cloud = cloud,
            includeCloud = isLoggedIn
        )
    }
}

suspend fun syncFromCloud() {
    if (!session.isLoggedIn.value) {
        cloudRepo.clearCloudCache()
        return
    }

    cloudRepo.loadFromCloud()
    cloudRepo.getAllDishes().first().forEach { localRepo.cacheSearchResult(it) }
}
```

Also remove unused imports from the file after this change:

```kotlin
import kotlinx.coroutines.flow.map
```

- [ ] **Step 2: Run repository tests**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests com.myorderapp.data.repository.DishMergePolicyTest --tests com.myorderapp.data.repository.CloudDishLoadStateTest
```

Expected: PASS.

## Task 3: Add-Dish Save Failure Handling

**Files:**
- Create: `app/src/main/java/com/myorderapp/ui/adddish/AddDishSaveMessages.kt`
- Create: `app/src/test/java/com/myorderapp/ui/adddish/AddDishSaveMessagesTest.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishViewModel.kt`

- [ ] **Step 1: Write failing message-helper tests**

Create `app/src/test/java/com/myorderapp/ui/adddish/AddDishSaveMessagesTest.kt`:

```kotlin
package com.myorderapp.ui.adddish

import org.junit.Assert.assertEquals
import org.junit.Test

class AddDishSaveMessagesTest {
    @Test
    fun `blank exception message uses fallback`() {
        val message = AddDishSaveMessages.primarySaveFailed(RuntimeException(""))

        assertEquals("保存失败，请重试", message)
    }

    @Test
    fun `exception message is trimmed`() {
        val message = AddDishSaveMessages.primarySaveFailed(RuntimeException("  网络不可用  "))

        assertEquals("保存失败：网络不可用", message)
    }

    @Test
    fun `long exception message is capped`() {
        val message = AddDishSaveMessages.primarySaveFailed(RuntimeException("a".repeat(120)))

        assertEquals("保存失败：${"a".repeat(80)}", message)
    }
}
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests com.myorderapp.ui.adddish.AddDishSaveMessagesTest
```

Expected: FAIL because `AddDishSaveMessages` does not exist.

- [ ] **Step 3: Add message helper**

Create `app/src/main/java/com/myorderapp/ui/adddish/AddDishSaveMessages.kt`:

```kotlin
package com.myorderapp.ui.adddish

object AddDishSaveMessages {
    fun primarySaveFailed(error: Throwable): String {
        val detail = error.message?.trim().orEmpty()
        if (detail.isBlank()) return "保存失败，请重试"
        return "保存失败：${detail.take(80)}"
    }
}
```

- [ ] **Step 4: Run the focused test and confirm it passes**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests com.myorderapp.ui.adddish.AddDishSaveMessagesTest
```

Expected: PASS.

- [ ] **Step 5: Wrap primary save in `AddDishViewModel.save()`**

Inside `viewModelScope.launch`, wrap the primary dish save block with `try/catch`. The body should keep existing dish construction but move the success update after `addDish` or `updateDish` returns:

```kotlin
try {
    val dishId = if (state.editDishId != null) {
        dishRepository.updateDish(Dish(
            id = state.editDishId,
            name = state.name, category = state.category,
            difficulty = state.difficulty,
            cookTimeMin = state.cookTimeMin.toIntOrNull() ?: 0,
            imageUrl = state.imageUrl,
            ingredients = state.ingredients, cookSteps = state.cookSteps,
            notes = state.notes, whoLikes = whoLikes,
            source = "custom", createdBy = "${state.myName}创建"
        ))
        state.editDishId
    } else {
        dishRepository.addDish(Dish(
            name = state.name, category = state.category,
            difficulty = state.difficulty,
            cookTimeMin = state.cookTimeMin.toIntOrNull() ?: 0,
            imageUrl = state.imageUrl,
            ingredients = state.ingredients, cookSteps = state.cookSteps,
            notes = state.notes, whoLikes = whoLikes,
            source = "custom", createdBy = "${state.myName}创建"
        ))
    }

    _uiState.value = _uiState.value.copy(savedSuccess = true, isSaving = false)

    val imgUrl = state.imageUrl
    if (imgUrl.isNotBlank() && (imgUrl.startsWith("content://") || imgUrl.startsWith("file://"))) {
        launch {
            try {
                val result = storageUploader.compressAndUpload(
                    appContext, Uri.parse(imgUrl), dishId
                )
                if (result.publicUrl != null) {
                    dishRepository.updateDish(Dish(
                        id = dishId, name = state.name, category = state.category,
                        difficulty = state.difficulty,
                        cookTimeMin = state.cookTimeMin.toIntOrNull() ?: 0,
                        imageUrl = result.publicUrl,
                        ingredients = state.ingredients, cookSteps = state.cookSteps,
                        notes = state.notes, whoLikes = whoLikes,
                        source = "custom", createdBy = "${state.myName}创建"
                    ))
                }
            } catch (_: Exception) {
            }
        }
    }
} catch (e: Exception) {
    _uiState.value = _uiState.value.copy(
        isSaving = false,
        savedSuccess = false,
        uploadMessage = AddDishSaveMessages.primarySaveFailed(e)
    )
}
```

- [ ] **Step 6: Run Kotlin compile**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: PASS.

## Task 4: Dish Library State Polish

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/dishlibrary/DishLibraryScreen.kt`

- [ ] **Step 1: Add Material icons imports**

Add these imports:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
```

- [ ] **Step 2: Replace text-plus add button content**

Replace:

```kotlin
) { Text("+ 添加", style = MaterialTheme.typography.labelLarge) }
```

with:

```kotlin
) {
    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text("添加", style = MaterialTheme.typography.labelLarge)
}
```

- [ ] **Step 3: Add search leading icon**

Add this property to the search `OutlinedTextField`:

```kotlin
leadingIcon = {
    Icon(
        Icons.Default.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
},
```

- [ ] **Step 4: Add refresh error and empty states before list items**

Inside `LazyVerticalGrid`, after the refresh-loading block and before `items(...)`, add:

```kotlin
val refreshState = pagedDishes.loadState.refresh
val appendState = pagedDishes.loadState.append

if (refreshState is LoadState.Error) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        LibraryMessage(
            title = "菜品加载失败",
            body = refreshState.error.message ?: "请稍后重试",
            actionText = "重试",
            onAction = { pagedDishes.retry() }
        )
    }
}

if (refreshState is LoadState.NotLoading && pagedDishes.itemCount == 0) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        LibraryMessage(
            title = "还没有菜品",
            body = if (uiState.searchQuery.isBlank()) "添加一道常吃的菜吧" else "换个关键词试试",
            actionText = if (uiState.searchQuery.isBlank()) "添加" else null,
            onAction = if (uiState.searchQuery.isBlank()) onAddDishClick else null
        )
    }
}
```

Then replace later direct append loading check with `appendState`:

```kotlin
if (appendState is LoadState.Loading) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

if (appendState is LoadState.Error) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        TextButton(
            onClick = { pagedDishes.retry() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("继续加载失败，点此重试")
        }
    }
}
```

- [ ] **Step 5: Add `LibraryMessage` composable**

Add this function near the bottom of `DishLibraryScreen.kt` before `DishGridCard`:

```kotlin
@Composable
private fun LibraryMessage(
    title: String,
    body: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionText != null && onAction != null) {
            FilledTonalButton(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                Text(actionText)
            }
        }
    }
}
```

- [ ] **Step 6: Make card image height stable**

Keep the existing `height(100.dp)` for image content and do not add dynamic text sizing. This preserves stable grid rows.

- [ ] **Step 7: Run Kotlin compile**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: PASS.

## Task 5: Documentation Convergence

**Files:**
- Modify: `docs/TECH_UPGRADE_PLAN.md`

- [ ] **Step 1: Add current-status note near the top**

Add this section after the opening overview:

```markdown
## Current Status As Of 2026-06-28

Several items from this plan are already partially implemented in the working tree:

- Supabase app data uses the official Kotlin SDK through `SupabaseClientProvider`.
- Room is active for dish, wishlist, meal, and profile entities.
- Dish library paging is wired through `RoomPagingDishRepository` and Paging Compose.
- Coil 3 is active through the application-level `SingletonImageLoader`.
- WorkManager upload infrastructure exists, but add/edit dish still needs careful queued-upload integration before direct coroutine upload can be removed.

Treat the rest of this document as historical upgrade context, not an exact description of the current source tree.
```

- [ ] **Step 2: Run a docs diff check**

Run:

```bash
git diff -- docs/TECH_UPGRADE_PLAN.md
```

Expected: diff contains only the current-status note.

## Task 6: Final Verification

**Files:**
- Verify all modified files.

- [ ] **Step 1: Run all unit tests**

Run:

```bash
./gradlew.bat testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 2: Run Kotlin compile**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 3: Build debug APK if compile and tests pass**

Run:

```bash
./gradlew.bat assembleDebug
```

Expected: PASS and APK exists under `app/build/outputs/apk/debug/`.

- [ ] **Step 4: Review diff**

Run:

```bash
git diff --stat
git status --short
```

Expected: changes include the planned helper/tests/repository/ViewModel/UI/docs files plus pre-existing user changes. Do not revert unrelated pre-existing changes.
