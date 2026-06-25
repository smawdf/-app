package com.myorderapp.data.remote.supabase

import android.util.Log
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RealtimeService {
    private val client = SupabaseClientProvider.client
    private var job: Job? = null
    private val _mealItemChanges = MutableSharedFlow<MealItemChange>()
    val mealItemChanges: SharedFlow<MealItemChange> = _mealItemChanges.asSharedFlow()

    suspend fun subscribeToPairMeals(pairId: String) {
        unsubscribe()
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val channel = client.realtime.channel("meals-$pairId")
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "meal_items"
                    filter = "meal_id=in.(select id from meals where pair_id=eq.$pairId)"
                }
                channel.subscribe()
                changeFlow.collect { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            Log.d("Realtime", "New meal item: ${action.record}")
                            _mealItemChanges.emit(MealItemChange.Inserted)
                        }
                        is PostgresAction.Delete -> {
                            Log.d("Realtime", "Deleted meal item")
                            _mealItemChanges.emit(MealItemChange.Deleted)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("Realtime", "Subscription error: ${e.message}")
            }
        }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}

sealed class MealItemChange {
    data object Inserted : MealItemChange()
    data object Deleted : MealItemChange()
}
