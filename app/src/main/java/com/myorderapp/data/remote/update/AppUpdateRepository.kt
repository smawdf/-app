package com.myorderapp.data.remote.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionName: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val releaseUrl: String?
)

sealed interface InstallApkResult {
    data object Started : InstallApkResult
    data object PermissionRequired : InstallApkResult
    data class Failed(val message: String) : InstallApkResult
}

class AppUpdateRepository(
    private val api: GitHubReleaseApi,
    private val httpClient: OkHttpClient,
    private val context: Context
) {
    private val downloadClient = httpClient.newBuilder()
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    suspend fun checkLatest(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val release = api.latestRelease()
        if (release.draft || release.prerelease) {
            throw IOException("没有可用的正式更新")
        }

        val versionName = release.tagName.removePrefix("v").trim()
        if (versionName.isBlank()) throw IOException("更新版本信息不完整")

        val apk = release.assets.firstOrNull {
            it.name.equals("$versionName.apk", ignoreCase = true)
        } ?: release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        } ?: throw IOException("更新包暂未上传")

        if (apk.downloadUrl.isBlank()) throw IOException("更新包地址无效")

        AppUpdateInfo(
            versionName = versionName,
            title = release.name?.takeIf { it.isNotBlank() } ?: "高糖小食 $versionName",
            notes = release.body?.trim().orEmpty(),
            downloadUrl = apk.downloadUrl,
            releaseUrl = release.htmlUrl
        )
    }

    suspend fun download(
        update: AppUpdateInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "app_updates").apply { mkdirs() }
        val target = File(directory, "gaotang-${update.versionName}.apk")
        val temporary = File(directory, "${target.name}.download")
        temporary.delete()

        val request = Request.Builder()
            .url(update.downloadUrl)
            .header("User-Agent", "GaoTangXiaoShi-Android")
            .build()
        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("下载失败 (${response.code})")
            }
            val body = response.body ?: throw IOException("更新内容为空")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            body.byteStream().use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloadedBytes += count
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
            }
        }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
        target
    }

    companion object {
        fun isNewerVersion(remote: String, current: String): Boolean {
            return AppVersionComparator.isNewer(remote, current)
        }
    }
}

class AppUpdateInstaller(private val context: Context) {
    fun canInstallPackages(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun install(file: File): InstallApkResult {
        if (!file.exists() || file.length() == 0L) {
            return InstallApkResult.Failed("更新包不存在，请重新下载")
        }
        if (!canInstallPackages()) return InstallApkResult.PermissionRequired

        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.startActivity(intent)
            InstallApkResult.Started
        }.getOrElse { error ->
            InstallApkResult.Failed(error.message ?: "无法打开安装程序")
        }
    }
}
