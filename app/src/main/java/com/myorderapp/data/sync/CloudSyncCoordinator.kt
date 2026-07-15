package com.myorderapp.data.sync

import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.RoomMenuRepository
import com.myorderapp.data.repository.SingleShopRepository
import com.myorderapp.data.repository.SupabaseProfileRepository
import com.myorderapp.data.repository.UserPreferencesRepository
import com.myorderapp.domain.repository.CandyCoinLedgerRepository
import com.myorderapp.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth

enum class CloudSyncPhase {
    IDLE,
    SYNCING,
    SUCCESS,
    PARTIAL_FAILURE
}

data class CloudSyncState(
    val phase: CloudSyncPhase = CloudSyncPhase.IDLE,
    val completedSteps: Int = 0,
    val totalSteps: Int = 0,
    val failedSteps: List<String> = emptyList()
)

internal data class CloudSyncTask(
    val name: String,
    val block: suspend () -> Unit
)

private class CloudSyncStepFailed(name: String) : IllegalStateException("Cloud sync step failed: $name")

internal suspend fun runCloudSyncTasks(
    tasks: List<CloudSyncTask>,
    onProgress: (completed: Int, failures: List<String>) -> Unit = { _, _ -> },
    onFailure: (name: String, throwable: Throwable) -> Unit = { _, _ -> }
): List<String> {
    val failures = mutableListOf<String>()
    tasks.forEachIndexed { index, task ->
        runCatching { task.block() }
            .onFailure { error ->
                failures += task.name
                onFailure(task.name, error)
            }
        onProgress(index + 1, failures.toList())
    }
    return failures
}

class CloudSyncCoordinator(
    private val dishRepository: HybridDishRepository,
    private val profileRepository: SupabaseProfileRepository,
    private val candyCoinLedgerRepository: CandyCoinLedgerRepository,
    private val shopRepository: SingleShopRepository,
    private val menuRepository: RoomMenuRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val orderRepository: OrderRepository,
    private val cloudErrorLogger: CloudErrorLogger,
    private val applicationScope: CoroutineScope
) {
    private val client by lazy { SupabaseClientProvider.client }
    private val _state = MutableStateFlow(CloudSyncState())
    val state: StateFlow<CloudSyncState> = _state.asStateFlow()
    private val syncMutex = Mutex()

    fun syncInBackground() {
        applicationScope.launch { syncAll() }
    }

    suspend fun syncAll() = syncMutex.withLock {
        if (!awaitAuthenticatedSession()) {
            _state.value = CloudSyncState()
            return@withLock
        }
        if (!SupabaseClientProvider.ensureFreshAuthSession()) {
            _state.value = CloudSyncState(
                phase = CloudSyncPhase.PARTIAL_FAILURE,
                completedSteps = 0,
                totalSteps = 1,
                failedSteps = listOf("session")
            )
            return@withLock
        }
        val tasks = listOf(
            CloudSyncTask("profile") {
                if (!profileRepository.loadFromCloud()) throw CloudSyncStepFailed("profile")
            },
            trackedTask("shop", "shop") { shopRepository.loadFromCloud() },
            trackedTask("menu", "menu") { menuRepository.loadFromCloud() },
            trackedTask("orders", "orders") { orderRepository.refreshOrders() },
            trackedTask("candy_coins", "candy_coin") { candyCoinLedgerRepository.loadFromCloud() },
            trackedTask("preferences", "preferences") { preferencesRepository.loadFromCloud() },
            trackedTask("dishes", "dishes") { dishRepository.syncFromCloud() }
        )
        _state.value = CloudSyncState(phase = CloudSyncPhase.SYNCING, totalSteps = tasks.size)
        val failures = runCloudSyncTasks(
            tasks = tasks,
            onProgress = { completed, failed ->
                _state.value = CloudSyncState(
                    phase = CloudSyncPhase.SYNCING,
                    completedSteps = completed,
                    totalSteps = tasks.size,
                    failedSteps = failed
                )
            },
            onFailure = { name, error ->
                cloudErrorLogger.log("cloud_sync", name, error)
            }
        )
        _state.value = CloudSyncState(
            phase = if (failures.isEmpty()) CloudSyncPhase.SUCCESS else CloudSyncPhase.PARTIAL_FAILURE,
            completedSteps = tasks.size,
            totalSteps = tasks.size,
            failedSteps = failures
        )
    }

    private suspend fun awaitAuthenticatedSession(): Boolean {
        return withTimeoutOrNull(AUTH_RESTORE_TIMEOUT_MS) {
            client.auth.sessionStatus
                .filter { it !is SessionStatus.LoadingFromStorage }
                .first() is SessionStatus.Authenticated
        } ?: false
    }

    private fun trackedTask(name: String, errorArea: String, block: suspend () -> Unit): CloudSyncTask {
        return CloudSyncTask(name) {
            val before = cloudErrorLogger.currentErrorSequence(errorArea)
            block()
            if (cloudErrorLogger.currentErrorSequence(errorArea) != before) throw CloudSyncStepFailed(name)
        }
    }

    private companion object {
        const val AUTH_RESTORE_TIMEOUT_MS = 10_000L
    }
}
