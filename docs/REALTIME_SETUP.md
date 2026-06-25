# Supabase Realtime Setup

## Overview

The OrderDisk app uses **Supabase Realtime** to provide live updates when meal items change for a paired user group. This replaces polling-based sync with instant push notifications over WebSocket.

## How It Works

1. **RealtimeService** (`com.myorderapp.data.remote.supabase.RealtimeService`) subscribes to Postgres changes on the `meal_items` table filtered by the current pair's ID.
2. When either user in a pair adds or removes a meal item, Supabase pushes the change to all connected clients in real time.
3. The service exposes a `SharedFlow<MealItemChange>` that ViewModels can collect to react to inserts and deletes instantly.

## Architecture

```
Supabase Realtime (WebSocket)
        │
        ▼
  RealtimeService
        │
        ▼  SharedFlow<MealItemChange>
  ViewModel (collect changes)
        │
        ▼
  UI recomposition
```

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| `RealtimeService` | `data/remote/supabase/RealtimeService.kt` | Manages WebSocket subscription to `meal_items` table |
| `SupabaseClientProvider` | `data/remote/supabase/SupabaseClientProvider.kt` | Provides the shared Supabase client with Realtime installed |

## Usage

### Dependency Injection

`RealtimeService` is registered as a Koin singleton in `AppModule.kt`:

```kotlin
single { RealtimeService() }
```

### Subscribing in a ViewModel

```kotlin
class MyViewModel(private val realtimeService: RealtimeService) : ViewModel() {

    fun startListening(pairId: String) {
        viewModelScope.launch {
            realtimeService.subscribeToPairMeals(pairId)
        }
        viewModelScope.launch {
            realtimeService.mealItemChanges.collect { change ->
                when (change) {
                    is MealItemChange.Inserted -> { /* refresh meal list */ }
                    is MealItemChange.Deleted  -> { /* refresh meal list */ }
                }
            }
        }
    }

    override fun onCleared() {
        realtimeService.unsubscribe()
    }
}
```

## Filter Syntax

The Realtime subscription uses a Postgres filter with a subquery:

```
meal_id=in.(select id from meals where pair_id=eq.<pairId>)
```

This ensures only meal items belonging to the current pair's meals are received.

## Dependencies

- `io.github.jan-tennert.supabase:realtime-kt:2.1.0` (defined in `gradle/libs.versions.toml`)
- `Realtime` plugin installed in `SupabaseClientProvider`

## Replacing Polling

The app previously used 3-second polling to sync meal data between paired users. With Realtime, updates arrive instantly over WebSocket, reducing server load and improving user experience.
