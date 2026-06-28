# OrderDisk Full Project Optimization Design

## Goal

Turn the current in-progress technical upgrade into a stable, coherent, faster, and more polished OrderDisk release without changing the product premise: a two-person Android ordering app built with Jetpack Compose, Koin, Room, Paging, Retrofit recipe APIs, and Supabase.

The optimization should protect daily use first: login, logout, pairing, adding dishes, deleting dishes, searching, opening the dish library, choosing meals, uploading images, and restarting the app should behave predictably.

## Current State

The codebase has already moved beyond the older project docs:

- Supabase app data now uses the official Kotlin SDK through `SupabaseClientProvider`.
- Room is present through `AppDatabase`, `DishDao`, Room entities, and `RoomDishRepository`.
- Dish library paging is partially wired through `RoomPagingDishRepository` and `LazyPagingItems`.
- Coil 3 image loading is initialized in `MyOrderApp`.
- Wishlist now has a Room repository and DAO.
- WorkManager upload infrastructure exists through `ImageUploadWorker`, but `AddDishViewModel` still uploads images with an ad-hoc coroutine.
- `docs/TECH_UPGRADE_PLAN.md` and the older stability spec no longer exactly match the current implementation.

The current uncommitted state builds and passes JVM unit tests, so the optimization should preserve that baseline while cleaning up risks.

## Non-Goals

- Do not redesign OrderDisk into a multi-user social app.
- Do not replace Koin, Compose, Room, or Supabase.
- Do not migrate to a multi-module project in this pass.
- Do not add broad instrumentation tests before the pure JVM seams are covered.
- Do not rotate real API keys or signing credentials in code; document the risk separately.
- Do not refactor unrelated feature packages just for style.

## Optimization Strategy

Use a stability-first sequence:

1. Freeze the current baseline and keep each phase independently verifiable.
2. Stabilize data/session boundaries before making the UI prettier.
3. Finish architecture migration seams only where they affect correctness or performance.
4. Improve performance and perceived responsiveness after data flow is trustworthy.
5. Polish visible UI states and release checks at the end.

## Phase 0: Baseline And Guardrails

Record the current working state before changing behavior:

- `./gradlew.bat testDebugUnitTest`
- `./gradlew.bat compileDebugKotlin`
- `git diff --stat`
- Current known dirty files and intent.

Add or keep lightweight tests around pure business rules so later changes have fast feedback. Avoid starting a large UI rewrite while repository/session behavior is still unsettled.

Acceptance:

- Existing tests pass.
- Kotlin debug compile passes.
- Optimization phases are documented.
- Any commits made during the work contain only intentional files.

## Phase 1: Stability And Data Consistency

### Problem

The highest-risk bugs are around identity and cached data:

- `HybridDishRepository.getAllDishes()` currently merges cloud dishes even when the user is logged out.
- `syncFromCloud()` does nothing when logged out instead of clearing cloud cache.
- Pair changes must refresh dish and meal caches.
- Add/edit dish save failures are not caught by `AddDishViewModel.save()`, leaving the UI stuck in saving state.
- Cloud save should be the primary success gate; image upload can remain asynchronous.
- Room cache is useful for speed and offline display, but it must not become an uncontrolled second source of truth across users/pairs.

### Design

Make `SessionManager` login state and pair state the boundary for all cloud participation.

`HybridDishRepository` should:

- Combine local dishes, cloud dishes, and `session.isLoggedIn`.
- Merge cloud dishes only when logged in.
- Clear `SupabaseDishRepository` cache when logged out.
- Cache freshly loaded cloud dishes into Room for fast paging and restart behavior.
- Prefer cloud operations while logged in, but only update Room after cloud operations succeed.

`SupabaseDishRepository` should:

- Keep `CloudDishLoadState` scoped by user id and pair id.
- Clear `_dishes` on logout or invalid session.
- Propagate create/update/delete failures.
- Avoid marking failed cloud loads as loaded.

`AddDishViewModel` should:

- Wrap the primary save in `try/catch`.
- Set `savedSuccess = true` only after `addDish` or `updateDish` returns.
- Reset `isSaving` and show a readable error when primary save fails.
- Keep image upload non-blocking.
- Prefer the existing `ImageUploadWorker` for queued uploads if it can be wired without making navigation wait.

`ProfileViewModel`, `OnboardingViewModel`, and `AuthViewModel` should:

- Trigger dish and meal sync after login and successful pair changes.
- Clear cloud caches on logout.
- Surface pairing failures clearly.

Acceptance:

- Logged-out users do not see stale cloud-only dishes from a previous session.
- Pair switch reloads dish and meal data.
- Add/edit dish reports primary save failure and exits saving state.
- Cloud write failures do not mutate local Room cache as if successful.
- Unit tests cover logged-in merge, logged-out merge, cloud cache reset, and save failure UI behavior where possible.

## Phase 2: Architecture Convergence

### Problem

The project is mid-migration. Several docs and comments still describe the old JSON/Retrofit Supabase architecture. Some dependencies and files also suggest both old and new paths.

### Design

Make the current architecture explicit:

- Supabase app data: official Kotlin SDK.
- Recipe search providers: Retrofit APIs for Juhe, Tian, and Jisu.
- Local persistence: Room for dishes, wishlist, meals, and profiles where implemented.
- UI state: ViewModels exposing `StateFlow`.
- Dependency injection: Koin only.

Clean up only where it reduces confusion or risk:

- Remove stale imports and unused helpers.
- Keep disabled tests disabled only if their purpose is documented or replace them with current SDK tests.
- Update docs that still say local persistence is JSON-only.
- Ensure `NetworkModule` only owns recipe API Retrofit clients, not Supabase app data.
- Keep `RoomPagingDishRepository` focused on paging reads, and keep write behavior in `RoomDishRepository`.

Acceptance:

- Docs match the current architecture.
- No active source file references removed in-memory dish/wishlist repositories.
- Supabase SDK and Retrofit responsibilities are clearly separated.
- Existing build remains green.

## Phase 3: Performance And Responsiveness

### Problem

Several flows may do extra work or have incomplete loading states:

- Search queries can trigger duplicate local work because `SearchViewModel` and `DualRecipeSearchUseCase` both consult the dish repository.
- Dish library paging is present, but empty/error states are thin.
- Image upload infrastructure exists, but the add-dish flow does not fully use WorkManager.
- Network logging and timeout policy should stay modest for debug and quiet for release.

### Design

Optimize visible and measurable paths:

- Debounce dish library search before recreating paging sources if rapid typing becomes noisy.
- Add clear refresh, append, empty, and error UI states for `LazyPagingItems`.
- Keep `RoomPagingDishRepository` page size and prefetch modest (`pageSize = 20`, `prefetchDistance = 5`) unless measurement says otherwise.
- Avoid duplicate search caching where it does not change behavior.
- Use WorkManager for local image URI uploads so failures can retry across process death.
- Keep Coil memory and disk cache as currently configured unless screenshots or device tests show memory pressure.
- Keep OkHttp logging at `BASIC` in debug and `NONE` in release.

Acceptance:

- Dish library remains responsive with a large Room dataset.
- Empty and error states are visible and actionable.
- Local image uploads can retry when network is unavailable.
- Search still returns local and remote results without duplicate tabs or confusing source labels.

## Phase 4: UI Polish

### Problem

The app is usable, but key operational states need clearer feedback:

- Save failure and upload pending/succeeded/failed states are easy to miss.
- Dish library cards and filters can be more scan-friendly.
- Some buttons use text symbols where icons would be clearer.
- Empty states should tell the user what action is available, without long instructional copy.

### Design

Polish only the screens touched by stability and performance work:

- Dish library: search leading icon, compact source filter, empty state, paging error retry, stable card heights.
- Add/edit dish: disabled save state, primary save error, background upload pending message if WorkManager is used.
- Profile/pairing: clear success/failure messages after join/unpair.
- Search: preserve source tabs, show partial API failures without hiding successful results.

Keep the visual language aligned with the existing Compose Material 3 style. Do not add a marketing landing page or decorative visual layer.

Acceptance:

- Text does not overflow buttons or cards on common phone widths.
- Loading, empty, error, and success states are distinct.
- Core workflows remain one or two taps from the current navigation structure.

## Phase 5: Release Verification

Before calling the optimization complete:

- Run `./gradlew.bat testDebugUnitTest`.
- Run `./gradlew.bat compileDebugKotlin`.
- Run `./gradlew.bat assembleDebug` if time allows.
- Review `git diff --stat`.
- Manually smoke-test:
  - Login
  - Logout
  - Register or existing-login happy path
  - Join pair
  - Unpair
  - Open dish library
  - Search dish library
  - Add dish without image
  - Add dish with local image
  - Delete custom dish
  - Start meal and add/remove dish

## Testing Strategy

Use focused JVM tests first:

- `CloudDishLoadStateTest` for pair/user scoped cache load decisions.
- `HybridDishRepositoryTest` for logged-in/logged-out merge behavior.
- `AddDishViewModelTest` for save success and failure state transitions.
- Existing `EntityMapperTest` and wishlist repository tests for Room mapping behavior.
- Existing `SearchViewModelTest` for search source behavior after any search changes.

Avoid slow instrumentation tests until these pure seams are stable.

## Rollback Strategy

Each phase should be small enough to revert independently:

- Stability changes must not require UI polish changes.
- Docs updates must not be bundled with behavior changes unless they describe the exact change.
- WorkManager upload adoption should be separable from primary dish save behavior.
- If Supabase SDK behavior is uncertain, keep the smallest possible repository-level changes and verify against existing compile/tests first.

## Residual Risks

- Supabase server-side RLS and schema compatibility cannot be fully proven by JVM tests.
- The pairing product model still appears to mix short pair codes with UUID-style `pair_id` values in places.
- Real image upload behavior depends on Storage bucket policy.
- Debug builds currently use the release signing config, which may be convenient locally but should be treated carefully before public distribution.
