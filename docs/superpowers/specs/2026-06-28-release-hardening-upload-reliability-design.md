# OrderDisk Release Hardening And Upload Reliability Design

## Goal

Prepare the current optimization branch for a safer release by tightening the parts that still carry release risk after the first optimization pass:

- Release build warnings that point to real maintainability or runtime risks.
- Background image upload reliability and writeback behavior.
- Final debug/release verification commands.
- Git staging boundaries, so unrelated generated or noisy files do not enter the release work.

This pass should not introduce a new product feature. It should make the current feature set more predictable before packaging.

## Current State

The previous optimization pass already moved major behavior in the right direction:

- Dish data now uses Room as local cache and paging source.
- Cloud dish merging is login-aware through `HybridDishRepository`.
- Add/edit dish primary save failures are caught and surfaced.
- Local `content://` and `file://` dish images are queued through WorkManager.
- `ImageUploadWorker` uploads images, retries failures, and writes the public image URL back to the dish repository.
- Debug unit tests, Kotlin debug compile, debug APK, and release APK have already passed once on this branch.

The branch is still dirty and contains mixed changes. Some of those changes are intentional optimization work, while folders such as `.codegraph/`, `preview/`, and unrelated generated artifacts should not be staged blindly.

## Non-Goals

- Do not redesign app navigation or visual identity.
- Do not replace Koin, Room, Compose, Supabase, WorkManager, or Retrofit.
- Do not add broad instrumentation tests in this pass.
- Do not change signing credentials or rotate production secrets from code.
- Do not commit unrelated local noise or generated workspace metadata.
- Do not make a large dependency upgrade unless it is required to fix a release-blocking issue.

## Approach Options

### Recommended: Release Hardening First

Fix warnings and reliability edges that can affect packaging or runtime confidence, then run the full release verification set again. This keeps the scope small, preserves the passing baseline, and directly supports app distribution.

### Alternative: UI Polish First

Spend the pass on screen-level polish and copy. This may improve perception, but it is less urgent because the current open risks are around build warnings, upload durability, and release readiness.

### Alternative: Broad Architecture Cleanup

Continue removing old repository paths and reshaping modules. This can be useful later, but it is too broad for the immediate release-prep goal and increases the chance of mixing unrelated changes.

## Design

### 1. Release Warning Cleanup

Review the warnings observed during the successful release build and fix the ones that are low-risk and meaningful:

- Add the missing Room index for the `mealId` foreign-key path if the schema currently lacks it.
- Migrate obvious deprecated Koin ViewModel DSL usages where the local Koin version supports the replacement cleanly.
- Replace deprecated Compose `menuAnchor()` calls with the current overload if available.
- Add explicit Kotlin annotation targets only where compiler warnings identify ambiguous targets.

Warnings that are external to the app, dependency-internal, or high-risk to change should be documented instead of force-fixed.

Acceptance:

- `assembleRelease` still passes.
- Fixed warnings do not change app behavior.
- Any remaining warnings are intentional and listed in the final verification notes.

### 2. Image Upload Reliability

Keep the current architecture: primary dish save succeeds or fails independently, while image upload runs in WorkManager for local URIs.

Tighten these points:

- Confirm `AddDishViewModel` only queues WorkManager upload after the primary dish save returns a stable dish id.
- Confirm remote image URLs are not re-uploaded.
- Confirm `ImageUploadWorker` validates required input data and returns `Result.failure()` for malformed work.
- Confirm upload and repository writeback failures return `Result.retry()` where retrying can reasonably recover.
- Confirm the Worker writes the uploaded public URL back through `DishRepository.updateDish`.
- Keep user navigation independent from upload completion.

Acceptance:

- Local image add/edit paths queue upload work.
- Remote image URL paths do not queue upload work.
- Upload failures can retry across process death.
- A successful upload updates the stored dish image URL.
- JVM tests cover the pure upload policy and any practical worker-adjacent behavior.

### 3. Data And Schema Safety

Avoid schema churn unless it is required by a warning or correctness fix.

If the Room index warning requires an entity annotation change, update the schema deliberately and verify the app database version/migration behavior already used in the project. If the project is using destructive migration for local cache data, document that assumption. If it uses explicit migrations, add the smallest required migration.

Acceptance:

- Room tests and compile still pass.
- The schema change is explainable as an index/performance fix, not a data model rewrite.

### 4. Verification Workflow

Run the final release verification sequence after changes:

- `./gradlew.bat testDebugUnitTest`
- `./gradlew.bat compileDebugKotlin`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

Also inspect output artifacts:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

Acceptance:

- All four commands pass.
- APK paths exist after the build.
- Final response lists any remaining warnings honestly.

### 5. Git Boundary

Before staging, review the dirty tree and include only files that belong to this release-hardening pass or the already intentional optimization set being finalized.

Do not stage:

- `.codegraph/`
- `preview/`
- unrelated generated files
- files whose purpose cannot be tied to the current release-hardening or prior approved optimization work

Acceptance:

- Final staging is deliberate and file-specific.
- The final response distinguishes committed spec/planning work from uncommitted implementation work if the tree remains mixed.

## Testing Strategy

Use the fastest useful tests first:

- Existing JVM unit tests for repository, mapper, wishlist, add-dish, and upload policy behavior.
- Kotlin debug compile to catch Compose/Koin/API migration mistakes.
- Debug and release APK builds for packaging confidence.

No new instrumentation test is required in this pass unless a fix cannot be validated through JVM tests and compilation.

## Rollback Strategy

Keep release-hardening edits small enough to revert independently:

- Warning cleanup should be separable from upload reliability changes.
- Upload reliability changes should be separable from docs updates.
- If a warning fix causes dependency/API churn, revert that specific fix and document the warning instead.

## Residual Risks

- Supabase Storage bucket policy and RLS cannot be fully proven by local JVM tests.
- Real device background execution may vary by manufacturer battery restrictions.
- Existing signing credentials are still project-local and should be protected outside this pass.
- The current branch includes pre-existing mixed changes, so staging discipline is part of the release work.
