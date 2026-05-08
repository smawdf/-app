package com.myorderapp.data.remote.supabase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.myorderapp.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID

class SupabaseStorageUploader(
    private val client: OkHttpClient,
    private val supabaseUrl: String,
    private val sessionManager: SessionManager
) {

    private val bucket = "dish-images"

    /**
     * 压缩 + 上传图片到 Supabase Storage，返回公开 URL
     */
    suspend fun compressAndUpload(
        context: Context,
        uri: Uri,
        dishId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 1. 读取原始图片
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBytes = inputStream.readBytes()
            inputStream.close()

            // 2. 压缩
            val compressed = compressImage(originalBytes)
                ?: originalBytes  // 压缩失败就用原图

            // 3. 上传
            val fileName = "${UUID.randomUUID().toString().take(8)}.jpg"
            val path = "$dishId/$fileName"
            val token = sessionManager.accessToken

            upload(path, compressed, token)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compressImage(bytes: ByteArray): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            // 缩放到最大宽度 800px
            val maxWidth = 800
            val scaled: Bitmap = if (bitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / bitmap.width
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
            } else bitmap

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)

            // 释放内存
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()

            output.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun upload(path: String, bytes: ByteArray, token: String): String? {
        return try {
            val mediaType = "image/jpeg".toMediaType()
            val body = bytes.toRequestBody(mediaType)
            val url = "$supabaseUrl/storage/v1/object/$bucket/$path"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "image/jpeg")
                .header("apikey", ApiConfig.SUPABASE_ANON_KEY)
                .apply {
                    if (token.isNotBlank()) header("Authorization", token)
                }
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                "$supabaseUrl/storage/v1/object/public/$bucket/$path"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
