package com.myorderapp.core.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.myorderapp.data.remote.supabase.SupabaseStorageUploader
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.TimeUnit

/**
 * 图片上传 Worker — 失败自动重试，支持离线排队
 */
class ImageUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val uploader: SupabaseStorageUploader by inject(SupabaseStorageUploader::class.java)

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_URI) ?: return Result.failure()
        val dishId = inputData.getString(KEY_DISH_ID) ?: return Result.failure()

        Log.d(TAG, "开始上传: uri=$uriString, dishId=$dishId")

        val uri = Uri.parse(uriString)
        val uploadResult = uploader.compressAndUpload(applicationContext, uri, dishId)

        return if (uploadResult.isSuccess) {
            val outputData = workDataOf(KEY_URL to uploadResult.publicUrl)
            Log.d(TAG, "上传成功: ${uploadResult.publicUrl}")
            Result.success(outputData)
        } else {
            Log.w(TAG, "上传失败: ${uploadResult.error}")
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR to uploadResult.error))
            }
        }
    }

    companion object {
        private const val TAG = "ImageUploadWorker"
        private const val KEY_URI = "image_uri"
        private const val KEY_DISH_ID = "dish_id"
        const val KEY_URL = "uploaded_url"
        const val KEY_ERROR = "error"
        private const val MAX_RETRIES = 3

        /**
         * 创建上传任务（加入队列，自动重试）
         */
        fun enqueue(context: Context, uri: Uri, dishId: String): String {
            val workName = "upload_${dishId}_${System.currentTimeMillis()}"
            val inputData = workDataOf(KEY_URI to uri.toString(), KEY_DISH_ID to dishId)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ImageUploadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("dish_upload")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)

            return request.id.toString()
        }
    }
}
