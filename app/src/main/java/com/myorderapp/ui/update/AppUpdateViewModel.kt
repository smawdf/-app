package com.myorderapp.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.BuildConfig
import com.myorderapp.data.remote.update.AppUpdateInfo
import com.myorderapp.data.remote.update.AppUpdateInstaller
import com.myorderapp.data.remote.update.AppUpdateRepository
import com.myorderapp.data.remote.update.InstallApkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val latest: AppUpdateInfo? = null,
    val isUpdateAvailable: Boolean = false,
    val downloadedApk: File? = null,
    val message: String? = null,
    val installPermissionRequired: Boolean = false
)

class AppUpdateViewModel(
    private val repository: AppUpdateRepository,
    private val installer: AppUpdateInstaller
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate() {
        if (_uiState.value.isChecking || _uiState.value.isDownloading) return
        _uiState.value = _uiState.value.copy(
            isChecking = true,
            message = null,
            installPermissionRequired = false
        )
        viewModelScope.launch {
            runCatching { repository.checkLatest() }
                .onSuccess { latest ->
                    _uiState.value = AppUpdateUiState(
                        latest = latest,
                        isUpdateAvailable = AppUpdateRepository.isNewerVersion(
                            latest.versionName,
                            BuildConfig.VERSION_NAME
                        ),
                        message = null
                    )
                    if (!AppUpdateRepository.isNewerVersion(latest.versionName, BuildConfig.VERSION_NAME)) {
                        _uiState.value = _uiState.value.copy(message = "当前已经是最新版本")
                    }
                }
                .onFailure { error ->
                    _uiState.value = AppUpdateUiState(message = error.toUserMessage())
                }
        }
    }

    fun downloadUpdate() {
        val update = _uiState.value.latest ?: return
        if (!_uiState.value.isUpdateAvailable || _uiState.value.isDownloading) return
        _uiState.value = _uiState.value.copy(
            isDownloading = true,
            downloadProgress = 0,
            message = null,
            downloadedApk = null,
            installPermissionRequired = false
        )
        viewModelScope.launch {
            runCatching {
                repository.download(update) { downloaded, total ->
                    val progress = if (total > 0L) {
                        ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                    } else 0
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }
            }.onSuccess { apk ->
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 100,
                    downloadedApk = apk,
                    message = "更新包已下载，可以安装"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    message = error.toUserMessage()
                )
            }
        }
    }

    fun installUpdate() {
        val apk = _uiState.value.downloadedApk ?: return
        when (val result = installer.install(apk)) {
            InstallApkResult.Started -> {
                _uiState.value = _uiState.value.copy(message = "正在打开系统安装程序")
            }
            InstallApkResult.PermissionRequired -> {
                _uiState.value = _uiState.value.copy(
                    installPermissionRequired = true,
                    message = "请先允许高糖小食安装未知应用"
                )
                installer.openInstallPermissionSettings()
            }
            is InstallApkResult.Failed -> {
                _uiState.value = _uiState.value.copy(message = result.message)
            }
        }
    }

    fun openInstallPermissionSettings() = installer.openInstallPermissionSettings()

    private fun Throwable.toUserMessage(): String = when {
        message?.contains("Unable to resolve host", ignoreCase = true) == true -> "网络连接失败，请检查网络后重试"
        message?.contains("timeout", ignoreCase = true) == true -> "网络响应超时，请稍后重试"
        else -> message?.takeIf { it.isNotBlank() } ?: "检查更新失败，请稍后重试"
    }
}
