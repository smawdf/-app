package com.myorderapp.core.worker

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.repository.RoomMenuRepository
import com.myorderapp.data.repository.SingleShopRepository
import com.myorderapp.domain.repository.ProfileRepository
import java.util.concurrent.TimeUnit
import java.io.File
import java.util.UUID
import org.koin.java.KoinJavaComponent.inject

class CloudImageUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val uploader: SupabaseStorageUploader by inject(SupabaseStorageUploader::class.java)
    private val session: SessionManager by inject(SessionManager::class.java)
    private val profileRepository: ProfileRepository by inject(ProfileRepository::class.java)
    private val shopRepository: SingleShopRepository by inject(SingleShopRepository::class.java)
    private val menuRepository: RoomMenuRepository by inject(RoomMenuRepository::class.java)

    override suspend fun doWork(): Result {
        val target = inputData.getString(KEY_TARGET).orEmpty()
        val targetId = inputData.getString(KEY_TARGET_ID).orEmpty()
        val expectedUserId = inputData.getString(KEY_USER_ID).orEmpty()
        val expectedSessionId = inputData.getString(KEY_SESSION_ID).orEmpty()
        val uri = inputData.getString(KEY_URI)?.let(Uri::parse)
            ?: return Result.failure(workDataOf(KEY_ERROR to "missing image uri"))
        if (
            expectedUserId.isBlank() ||
            expectedSessionId.isBlank() ||
            session.currentUserId != expectedUserId ||
            session.currentSessionId != expectedSessionId
        ) {
            cleanupPendingFile(uri)
            return Result.failure(workDataOf(KEY_ERROR to "account session changed"))
        }
        val upload = if (target == TARGET_AVATAR) {
            uploader.compressAndUploadAvatar(applicationContext, uri)
        } else {
            uploader.compressAndUpload(applicationContext, uri, targetId.ifBlank { target })
        }
        val url = upload.publicUrl ?: run {
            if (runAttemptCount >= MAX_RETRIES) cleanupPendingFile(uri)
            return retryOrFailure(upload.error.orEmpty())
        }

        return runCatching {
            when (target) {
                TARGET_AVATAR -> profileRepository.updateAvatar(url)
                TARGET_SHOP -> shopRepository.updateShopImageUrl(url)
                TARGET_MENU -> menuRepository.updateDishImage(targetId, url)
                else -> error("unknown image target")
            }
        }.fold(
            onSuccess = {
                cleanupPendingFile(uri)
                Result.success(workDataOf(KEY_URL to url))
            },
            onFailure = {
                if (runAttemptCount >= MAX_RETRIES) cleanupPendingFile(uri)
                retryOrFailure(it.message.orEmpty())
            }
        )
    }

    private fun retryOrFailure(error: String): Result {
        return if (runAttemptCount < MAX_RETRIES) Result.retry()
        else Result.failure(workDataOf(KEY_ERROR to error))
    }

    companion object {
        const val TARGET_AVATAR = "avatar"
        const val TARGET_SHOP = "shop"
        const val TARGET_MENU = "menu"
        const val KEY_URL = "uploaded_url"
        const val KEY_ERROR = "error"
        private const val KEY_TARGET = "target"
        private const val KEY_TARGET_ID = "target_id"
        private const val KEY_URI = "image_uri"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SESSION_ID = "session_id"
        private const val MAX_RETRIES = 5

        private fun snapshotForWork(context: Context, uri: Uri): Uri {
            if (uri.scheme == "file") return uri
            return runCatching {
                val directory = File(context.filesDir, "pending_uploads").apply { mkdirs() }
                val target = File(directory, "upload_${UUID.randomUUID()}.img")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use(input::copyTo)
                } ?: error("unable to read selected image")
                Uri.fromFile(target)
            }.getOrDefault(uri)
        }

        fun enqueue(
            context: Context,
            target: String,
            targetId: String,
            uri: Uri,
            userId: String,
            sessionId: String
        ): String {
            val workUri = snapshotForWork(context, uri)
            val request = OneTimeWorkRequestBuilder<CloudImageUploadWorker>()
                .setInputData(
                    workDataOf(
                        KEY_TARGET to target,
                        KEY_TARGET_ID to targetId,
                        KEY_URI to workUri.toString(),
                        KEY_USER_ID to userId,
                        KEY_SESSION_ID to sessionId
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("cloud_image_upload")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "cloud_image_upload_${target}_${targetId}",
                ExistingWorkPolicy.REPLACE,
                request
            )
            return request.id.toString()
        }
    }

    private fun cleanupPendingFile(uri: Uri) {
        if (uri.scheme != "file") return
        val file = uri.path?.let(::File) ?: return
        val pendingDirectory = File(applicationContext.filesDir, "pending_uploads")
        if (file.parentFile == pendingDirectory) file.delete()
    }
}
