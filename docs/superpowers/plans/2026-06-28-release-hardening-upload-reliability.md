# Release Hardening And Upload Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the current OrderDisk optimization branch for release by fixing low-risk release warnings, tightening image upload retry/writeback behavior, and running the final debug/release verification sequence.

**Architecture:** Keep the existing single-module Clean Architecture + MVVM shape. Make only small, release-oriented edits in Room entities/migrations, add-dish upload policy/ViewModel/Worker, Koin/Compose warning sites, docs, and verification boundaries.

**Tech Stack:** Android, Kotlin 2.2.10, Jetpack Compose Material 3, Room 2.7.1, WorkManager 2.10.1, Koin 4.0.2, Supabase Kotlin SDK, Gradle/AGP 9.2.0.

---

## File Structure

- Modify `app/src/main/java/com/myorderapp/data/local/entity/MealItemEntity.kt`
  - Responsibility: Room entity for meal dish rows. Add the `mealId` index that Room warns about for the foreign key.
- Modify `app/src/main/java/com/myorderapp/data/local/AppDatabase.kt`
  - Responsibility: Room database configuration and migrations. Bump the schema version and add a tiny index migration.
- Modify `app/src/main/java/com/myorderapp/ui/adddish/AddDishImageUploadPolicy.kt`
  - Responsibility: Pure decision logic for which image URL values should queue WorkManager upload.
- Modify `app/src/test/java/com/myorderapp/ui/adddish/AddDishImageUploadPolicyTest.kt`
  - Responsibility: Fast JVM coverage for local-vs-remote upload queue decisions.
- Modify `app/src/main/java/com/myorderapp/ui/adddish/AddDishViewModel.kt`
  - Responsibility: Primary dish save state and upload work enqueue timing.
- Modify `app/src/main/java/com/myorderapp/core/worker/ImageUploadWorker.kt`
  - Responsibility: Background upload, retry/failure classification, public URL writeback.
- Modify `app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt`
  - Responsibility: Replace the deprecated `menuAnchor()` call with the current Material 3 overload.
- Modify `app/src/main/java/com/myorderapp/di/AppModule.kt`
  - Responsibility: Migrate simple Koin ViewModel definitions to `viewModelOf` where constructor references are direct and keep context-assisted definitions explicit.
- Modify `docs/TECH_UPGRADE_PLAN.md`
  - Responsibility: Record any remaining release warnings that are intentionally not fixed in this pass.

Do not stage `.codegraph/`, `preview/`, or unrelated generated files.

---

### Task 1: Add Room Index Migration For `meal_items.mealId`

**Files:**
- Modify: `app/src/main/java/com/myorderapp/data/local/entity/MealItemEntity.kt`
- Modify: `app/src/main/java/com/myorderapp/data/local/AppDatabase.kt`

- [ ] **Step 1: Add the entity index**

Replace the imports and `@Entity` declaration in `MealItemEntity.kt` with:

```kotlin
package com.myorderapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_items",
    foreignKeys = [ForeignKey(
        entity = MealEntity::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["mealId"])]
)
data class MealItemEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val dishId: String,
    val dishName: String,
    val dishCategory: String = "",
    val dishImageUrl: String? = null,
    val cookTimeMin: Int = 0,
    val difficulty: Int = 1,
    val chosenBy: String = "",
    val chosenByName: String = "",
    val quantity: Int = 1,
    val notes: String = ""
)
```

- [ ] **Step 2: Add a Room 2-to-3 migration**

In `AppDatabase.kt`, change the database version from `2` to `3`, then add this migration below `MIGRATION_1_2`:

```kotlin
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_items_mealId` ON `meal_items` (`mealId`)")
            }
        }
```

Then update `getInstance()` migration registration to:

```kotlin
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
```

- [ ] **Step 3: Compile to verify Room accepts the schema**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. The previous Room foreign-key index warning for `mealId` should disappear from the compile/build output. If a Room migration error appears, keep version `3` and fix only the SQL/index name needed by Room.

- [ ] **Step 4: Commit the Room hardening change**

Run:

```bash
git add app/src/main/java/com/myorderapp/data/local/entity/MealItemEntity.kt app/src/main/java/com/myorderapp/data/local/AppDatabase.kt
git commit -m "fix: add meal item room index migration"
```

---

### Task 2: Harden Image Upload Queue Decisions

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishImageUploadPolicy.kt`
- Modify: `app/src/test/java/com/myorderapp/ui/adddish/AddDishImageUploadPolicyTest.kt`
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishViewModel.kt`

- [ ] **Step 1: Extend the pure upload policy test**

Append these tests to `AddDishImageUploadPolicyTest`:

```kotlin
    @Test
    fun `uppercase local uri scheme should be queued for upload`() {
        assertTrue(AddDishImageUploadPolicy.shouldQueueUpload("CONTENT://media/picker/1"))
    }

    @Test
    fun `remote url with surrounding whitespace should not be queued`() {
        assertFalse(AddDishImageUploadPolicy.shouldQueueUpload("  https://example.com/dish.jpg  "))
    }

    @Test
    fun `local uri with surrounding whitespace should be queued`() {
        assertTrue(AddDishImageUploadPolicy.shouldQueueUpload("  file:///tmp/dish.jpg  "))
    }
```

- [ ] **Step 2: Run the focused test and confirm the uppercase case fails**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests com.myorderapp.ui.adddish.AddDishImageUploadPolicyTest
```

Expected before implementation: at least the uppercase local URI test fails because the current policy checks `startsWith("content://")` case-sensitively.

- [ ] **Step 3: Make the policy case-insensitive**

Replace `AddDishImageUploadPolicy.kt` with:

```kotlin
package com.myorderapp.ui.adddish

import java.util.Locale

object AddDishImageUploadPolicy {
    fun shouldQueueUpload(imageUrl: String): Boolean {
        val value = imageUrl.trim().lowercase(Locale.US)
        return value.startsWith("content://") || value.startsWith("file://")
    }
}
```

- [ ] **Step 4: Keep WorkManager enqueue after the stable dish id**

Inspect `AddDishViewModel.save()`. Keep this ordering intact:

```kotlin
                _uiState.value = _uiState.value.copy(savedSuccess = true, isSaving = false)

                val imgUrl = state.imageUrl
                if (AddDishImageUploadPolicy.shouldQueueUpload(imgUrl)) {
                    ImageUploadWorker.enqueue(appContext, Uri.parse(imgUrl), dishId)
                }
```

If the code differs, restore this ordering so the upload queues only after `addDish()` or `updateDish()` returns a stable `dishId`.

- [ ] **Step 5: Run the focused policy test**

Run:

```bash
./gradlew.bat testDebugUnitTest --tests com.myorderapp.ui.adddish.AddDishImageUploadPolicyTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the upload policy hardening**

Run:

```bash
git add app/src/main/java/com/myorderapp/ui/adddish/AddDishImageUploadPolicy.kt app/src/test/java/com/myorderapp/ui/adddish/AddDishImageUploadPolicyTest.kt app/src/main/java/com/myorderapp/ui/adddish/AddDishViewModel.kt
git commit -m "fix: harden dish image upload queue policy"
```

---

### Task 3: Tighten Worker Input And Retry Classification

**Files:**
- Modify: `app/src/main/java/com/myorderapp/core/worker/ImageUploadWorker.kt`

- [ ] **Step 1: Make malformed Worker input fail with explicit output data**

In `ImageUploadWorker.doWork()`, replace the first two input lines:

```kotlin
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val dishId = inputData.getString(KEY_DISH_ID) ?: return Result.failure()
```

with:

```kotlin
        val uriString = inputData.getString(KEY_URI)?.trim()
        if (uriString.isNullOrBlank()) {
            return Result.failure(workDataOf(KEY_ERROR to "missing image uri"))
        }

        val dishId = inputData.getString(KEY_DISH_ID)?.trim()
        if (dishId.isNullOrBlank()) {
            return Result.failure(workDataOf(KEY_ERROR to "missing dish id"))
        }
```

- [ ] **Step 2: Catch unexpected upload exceptions as retryable**

Replace the upload call:

```kotlin
        val uri = Uri.parse(uriString)
        val uploadResult = uploader.compressAndUpload(applicationContext, uri, dishId)
```

with:

```kotlin
        val uri = Uri.parse(uriString)
        val uploadResult = try {
            uploader.compressAndUpload(applicationContext, uri, dishId)
        } catch (e: Exception) {
            Log.w(TAG, "图片上传异常: ${e.message}")
            return retryOrFailure("图片上传异常: ${e.message}")
        }
```

- [ ] **Step 3: Preserve writeback retry behavior**

Confirm the successful upload branch still writes back through `dishRepository.updateDish(dish.copy(imageUrl = publicUrl))` and that the `catch` branch returns:

```kotlin
                retryOrFailure("图片已上传但回写失败: ${e.message}")
```

If the message is still garbled from earlier encoding, replacing only the string literal is allowed. Do not change the control flow.

- [ ] **Step 4: Compile to verify Worker changes**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit Worker hardening**

Run:

```bash
git add app/src/main/java/com/myorderapp/core/worker/ImageUploadWorker.kt
git commit -m "fix: harden image upload worker retries"
```

---

### Task 4: Clean Up Low-Risk Deprecated API Warnings

**Files:**
- Modify: `app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt`
- Modify: `app/src/main/java/com/myorderapp/di/AppModule.kt`
- Modify: `docs/TECH_UPGRADE_PLAN.md`

- [ ] **Step 1: Replace the deprecated add-dish menu anchor call**

In `AddDishScreen.kt`, replace:

```kotlin
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
```

with:

```kotlin
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
```

The file already has `@OptIn(ExperimentalMaterial3Api::class)` and `import androidx.compose.material3.*`, so no extra import is required.

- [ ] **Step 2: Migrate simple Koin ViewModels to constructor DSL**

In `AppModule.kt`, add:

```kotlin
import org.koin.core.module.dsl.viewModelOf
```

Keep this import because context-assisted ViewModels still use it:

```kotlin
import org.koin.androidx.viewmodel.dsl.viewModel
```

Replace simple ViewModel definitions with constructor references:

```kotlin
    viewModelOf(::HomeViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::DishDetailViewModel)
    viewModel { DishLibraryViewModel(get<HybridDishRepository>(), get()) }
    viewModel { AddDishViewModel(get(), get(), androidContext()) }
    viewModelOf(::MealViewModel)
    viewModel { RandomViewModel(get(), get(), androidContext()) }
    viewModelOf(::WishlistViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::OnboardingViewModel)
```

Do not migrate `DishLibraryViewModel`, `AddDishViewModel`, or `RandomViewModel` in this pass because their definitions use explicit type disambiguation or `androidContext()`.

- [ ] **Step 3: Compile to verify Koin and Compose API usage**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. If `viewModelOf` is unavailable for this Koin artifact, revert only the `AppModule.kt` Koin DSL edit and document the warning in `docs/TECH_UPGRADE_PLAN.md`.

- [ ] **Step 4: Document any remaining intentionally unfixed warnings**

In `docs/TECH_UPGRADE_PLAN.md`, under `Current Status As Of 2026-06-28`, add a short bullet if any warning remains after compile/release:

```markdown
- Remaining release warnings are tracked explicitly: external native library strip warnings are packaging-level and not app-source changes; context-assisted Koin ViewModel definitions remain explicit where constructor DSL would reduce clarity.
```

Only add this bullet if the warning still appears or if Task 4 Step 3 required reverting a Koin DSL migration.

- [ ] **Step 5: Commit warning cleanup**

Run:

```bash
git add app/src/main/java/com/myorderapp/ui/adddish/AddDishScreen.kt app/src/main/java/com/myorderapp/di/AppModule.kt docs/TECH_UPGRADE_PLAN.md
git commit -m "chore: clean up release warning sites"
```

---

### Task 5: Final Release Verification And Staging Boundary

**Files:**
- Read: full git status
- Read: Gradle build output
- Read: APK output paths
- Modify: none unless verification reveals a release-blocking issue

- [ ] **Step 1: Run JVM unit tests**

Run:

```bash
./gradlew.bat testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run Kotlin debug compile**

Run:

```bash
./gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build debug APK**

Run:

```bash
./gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 4: Build release APK**

Run:

```bash
./gradlew.bat assembleRelease
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/release/app-release.apk` exists.

- [ ] **Step 5: Verify APK artifacts**

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

- [ ] **Step 6: Review remaining dirty tree without staging noise**

Run:

```bash
git status --short --branch
```

Expected: `.codegraph/`, `preview/`, and unrelated generated files remain unstaged. Any implementation files still dirty should be explained as either intentional prior optimization work or part of this hardening pass.

- [ ] **Step 7: Commit final verification note only if docs changed**

If `docs/TECH_UPGRADE_PLAN.md` was updated during verification, run:

```bash
git add docs/TECH_UPGRADE_PLAN.md
git commit -m "docs: record release verification notes"
```

If no files changed during verification, do not create an empty commit.

---

## Self-Review

- Spec coverage: Task 1 covers Room release warning cleanup and schema safety. Tasks 2 and 3 cover image upload queue/writeback reliability. Task 4 covers low-risk deprecated API warning cleanup and warning documentation. Task 5 covers final verification, APK artifact checks, and git boundary discipline.
- Marker scan: The plan contains no unfinished-marker terms and no vague instructions without code or commands.
- Type consistency: `MealItemEntity`, `AppDatabase`, `AddDishImageUploadPolicy`, `AddDishViewModel`, `ImageUploadWorker`, `AddDishScreen`, and `AppModule` names match the current source tree.
- Scope check: This plan stays within release hardening and does not add product features, broad UI redesign, or a dependency upgrade.
