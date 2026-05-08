package com.myorderapp.data.remote.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.myorderapp.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID

data class UploadResult(
    val publicUrl: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = publicUrl != null
}

class SupabaseStorageUploader(
    private val client: OkHttpClient,
    private val supabaseUrl: String,
    private val sessionManager: SessionManager
) {
    private val bucket = "dish-images"
    private val tag = "StorageUploader"

    suspend fun compressAndUpload(
        context: Context,
        uri: Uri,
        dishId: String
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            // 1. 读取原始图片
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext UploadResult(error = "无法读取图片文件")
            val originalBytes = inputStream.readBytes()
            inputStream.close()

            if (originalBytes.isEmpty()) {
                return@withContext UploadResult(error = "图片文件为空")
            }

            Log.d(tag, "读取图片: ${originalBytes.size} bytes")

            // 2. 压缩
            val compressed = compressImage(originalBytes)
                ?: originalBytes

            Log.d(tag, "压缩后: ${compressed.size} bytes")

            // 3. 上传
            val fileName = "${UUID.randomUUID().toString().take(8)}.jpg"
            val path = "$dishId/$fileName"
            val token = sessionManager.accessToken.ifBlank { null }

            Log.d(tag, "开始上传: path=$path, token=${if (token != null) "有" else "无"}")

            upload(path, compressed, token)
        } catch (e: Exception) {
            Log.e(tag, "上传异常: ${e.javaClass.simpleName}: ${e.message}", e)
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

    private fun upload(path: String, bytes: ByteArray, token: String?): UploadResult {
        return try {
            val url = "$supabaseUrl/storage/v1/object/$bucket/$path"
            val request = Request.Builder()
                .url(url)
                .post(bytes.toRequestBody("image/jpeg".toMediaType()))
                .header("Content-Type", "image/jpeg")
                .header("apikey", ApiConfig.SUPABASE_ANON_KEY)
                .apply {
                    if (token != null) header("Authorization", token)
                }
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucket/$path"
                Log.d(tag, "上传成功: $publicUrl")
                UploadResult(publicUrl = publicUrl)
            } else {
                Log.e(tag, "上传失败: HTTP ${response.code} - $responseBody")
                val errorMsg = when (response.code) {
                    403 -> "上传被拒绝(403): 请检查 Supabase RLS 策略"
                    413 -> "图片太大(413): 请选择较小的图片"
                    401 -> "认证失败(401)"
                    else -> "上传失败(HTTP ${response.code})"
                }
                UploadResult(error = errorMsg)
            }
        } catch (e: Exception) {
            Log.e(tag, "上传网络错误: ${e.message}")
            UploadResult(error = "网络错误: ${e.message}")
        }
    }
}
