package com.myorderapp.core.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.repository.SupabaseOrderRepository
import java.util.concurrent.TimeUnit
import org.koin.java.KoinJavaComponent.inject

class OrderSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val session: SessionManager by inject(SessionManager::class.java)
    private val orderRepository: SupabaseOrderRepository by inject(SupabaseOrderRepository::class.java)

    override suspend fun doWork(): Result {
        val expectedUserId = inputData.getString(KEY_USER_ID).orEmpty()
        val expectedSessionId = inputData.getString(KEY_SESSION_ID).orEmpty()
        if (
            expectedUserId.isBlank() ||
            expectedSessionId.isBlank() ||
            session.currentUserId != expectedUserId ||
            session.currentSessionId != expectedSessionId
        ) {
            return Result.failure()
        }
        return runCatching { orderRepository.syncPendingOrders() }.fold(
            onSuccess = { Result.success() },
            onFailure = { if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure() }
        )
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SESSION_ID = "session_id"
        private const val MAX_RETRIES = 5

        fun enqueue(context: Context, userId: String, sessionId: String) {
            val request = OneTimeWorkRequestBuilder<OrderSyncWorker>()
                .setInputData(workDataOf(KEY_USER_ID to userId, KEY_SESSION_ID to sessionId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("order_sync")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "order_sync_$userId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
