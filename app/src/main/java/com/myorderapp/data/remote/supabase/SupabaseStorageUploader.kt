package com.myorderapp.data.remote.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

data class UploadResult(
    val publicUrl: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = publicUrl != null
}

class SupabaseStorageUploader(
    private val session: SessionManager,
    private val cloudErrorLogger: CloudErrorLogger
) {
    private val client = SupabaseClientProvider.client
    private val bucket = "dish-images"
    private val tag = "StorageUploader"

    suspend fun compressAndUpload(
        context: Context,
        uri: Uri,
        dishId: String
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext UploadResult(error = "无法读取图片文件")
            val originalBytes = inputStream.readBytes()
            inputStream.close()

            if (originalBytes.isEmpty()) {
                return@withContext UploadResult(error = "图片文件为空")
            }

            Log.d(tag, "读取图片: ${originalBytes.size} bytes")

            val compressed = compressImage(originalBytes) ?: originalBytes
            Log.d(tag, "压缩后: ${compressed.size} bytes")

            val fileName = "${UUID.randomUUID().toString().take(8)}.jpg"
            val pairId = session.currentPairId
            val path = if (pairId.isBlank()) "$dishId/$fileName" else "$pairId/$dishId/$fileName"

            Log.d(tag, "开始上传: path=$path, size=${compressed.size}")

            upload(path, compressed)
        } catch (e: Exception) {
            cloudErrorLogger.log("storage", "upload_menu_image", e, "dishId=$dishId")
            Log.e(tag, "上传异常: ${e.javaClass.simpleName}: ${e.message}", e)
            UploadResult(error = "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    suspend fun compressAndUploadAvatar(
        context: Context,
        uri: Uri
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext UploadResult(error = "无法读取头像文件")
            val originalBytes = inputStream.readBytes()
            inputStream.close()

            if (originalBytes.isEmpty()) {
                return@withContext UploadResult(error = "头像文件为空")
            }

            val compressed = compressImage(originalBytes) ?: originalBytes
            val userFolder = session.currentUserId.ifBlank { "anonymous" }
            val pairFolder = session.currentPairId.ifBlank { "avatars" }
            val fileName = "${UUID.randomUUID().toString().take(8)}.jpg"
            upload("$pairFolder/avatars/$userFolder/$fileName", compressed)
        } catch (e: Exception) {
            cloudErrorLogger.log("storage", "upload_avatar", e)
            Log.e(tag, "头像上传异常: ${e.javaClass.simpleName}: ${e.message}", e)
            UploadResult(error = "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun compressImage(bytes: ByteArray): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            val maxWidth = 800
            val scaled: Bitmap = if (bitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / bitmap.width
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
            } else bitmap

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)

            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()

            output.toByteArray()
        } catch (e: Exception) {
            Log.e(tag, "压缩失败: ${e.message}")
            null
        }
    }

    private suspend fun upload(path: String, bytes: ByteArray): UploadResult {
        return try {
            client.storage.from(bucket).upload(path, bytes)
            val publicUrl = client.storage.from(bucket).publicUrl(path)
            Log.d(tag, "上传成功: $publicUrl")
            UploadResult(publicUrl = publicUrl)
        } catch (e: Exception) {
            cloudErrorLogger.log("storage", "upload_bytes", e, "path=$path size=${bytes.size}")
            Log.e(tag, "上传失败: ${e.message}")
            UploadResult(error = "上传失败: ${e.message}")
        }
    }
}
