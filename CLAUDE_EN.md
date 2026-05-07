# CLAUDE_EN.md

Guidance for Claude Code working in the OrderDisk repository.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew lint                   # Lint checks
```

## Project Overview

**今天吃什么？** (package `com.myorderapp`, formerly OrderDisk) — A couple-oriented meal ordering and recipe management Android app. Search recipes → start a meal → each person picks dishes → results merge in real time.

### Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 + Coil |
| Architecture | MVVM (ViewModel → Repository) |
| DI | Koin |
| Networking | Retrofit + OkHttp + Moshi |
| Cloud DB & Auth | Supabase REST API (direct, not supabase-kt SDK) |
| Local cache | InMemoryRepository + SharedPreferences (session) |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |

### Key Files

| File | Purpose |
|------|---------|
| `ApiConfig.kt` | All API keys (Juhe, Spoonacular, Supabase) |
| `di/NetworkModule.kt` | Retrofit instances + SessionManager |
| `di/AppModule.kt` | Repository + ViewModel bindings |
| `data/remote/supabase/SessionManager.kt` | JWT + SharedPreferences persistence |
| `data/remote/recipe/FoodTranslator.kt` | 180+ entry CN→EN food dictionary |
| `domain/usecase/DualRecipeSearchUseCase.kt` | Parallel Juhe+Spoonacular search |
| `data/repository/HybridDishRepository.kt` | Online/offline auto-switch |
| `table/*.sql` | Full Supabase schema + RLS + Realtime + Storage |

## Recipe Data Sources

| Source | source value | Notes |
|--------|-------------|-------|
| Built-in | `"builtin"` | `assets/recipes.json` — 90 Chinese dishes |
| Juhe API | `"external"` / `externalSource="juhe"` | Raw `type_name` used as category |
| Spoonacular | `"external"` / `externalSource="spoonacular"` | Auto CN→EN translate, 636x393 images |
| User-created | `"custom"` | Local or Supabase |

**Search flow**: input → 300ms debounce → local + Juhe + Spoonacular in parallel → merge & deduplicate → auto-cache all results.

## Online/Offline Architecture

- `SessionManager` persists JWT via `SharedPreferences`, auto-restores on launch
- `HybridDishRepository` delegates to `SupabaseDishRepository` when logged in, `InMemoryDishRepository` when offline
- After login: `dishRepo.syncFromCloud()` + `profileRepo.loadFromCloud()` + `mealRepo.syncFromCloud()`

## API Keys

| API | Key | Daily Limit |
|-----|-----|-------------|
| Juhe | `7fc3e0bdf2061a5c38781c2e82908d31` | ~100 |
| Spoonacular | `c4d077f93cbd4545ae6337c03b1c925a` | 150 |
| Supabase | `sb_publishable_8N_jUSyhvKOmWAPXGRAIhA__q96dF7a` | unlimited |

Search results are auto-cached to avoid redundant API calls.

## Key Design Decisions

- **Koin DI** (not Hilt): `modules(appModule, networkModule)`
- **Direct Retrofit calls to Supabase** (not supabase-kt SDK): compatible with existing Moshi setup
- **@JsonClass(generateAdapter) removed**: AGP 9.x built-in Kotlin doesn't support kapt; uses `KotlinJsonAdapterFactory` reflection instead
- **Domain models use @Json(name=snake_case)**: Maps Supabase column names (e.g. `@Json(name="pair_id") val pairId`)
- **Raw API categories**: Juhe `type_name` and Spoonacular `cuisines` used directly as dish category, not mapped to a fixed set
- **Auto-cache search results**: `onlineResult.dishes.forEach { cacheSearchResult(it) }`
