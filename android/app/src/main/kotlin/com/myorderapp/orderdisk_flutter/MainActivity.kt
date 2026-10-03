package com.myorderapp.orderdisk_flutter

import android.content.Intent
import android.net.Uri
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.net.URLEncoder

/**
 * 自研本地存储与系统跳转桥接。
 *
 * 零第三方依赖、零额外构建要求：
 * 1. 读写 App 私有目录下的 JSON 文件；
 * 2. 唤起抖音 / 哔哩哔哩客户端搜索菜谱，未安装时回退至浏览器网页搜索。
 */
class MainActivity : FlutterActivity() {

    private val storeChannelName = "com.myorderapp.orderdisk_flutter/store"
    private val appChannelName = "com.myorderapp.orderdisk_flutter/app"
    private val fileName = "orderdisk_local_store.json"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 本地配置存储 Channel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, storeChannelName)
            .setMethodCallHandler { call, result ->
                val file = File(filesDir, fileName)
                when (call.method) {
                    "read" -> {
                        result.success(if (file.exists()) file.readText() else "")
                    }
                    "write" -> {
                        val content = call.argument<String>("content").orEmpty()
                        try {
                            file.writeText(content)
                            result.success(true)
                        } catch (e: Exception) {
                            result.error("WRITE_FAILED", e.message, null)
                        }
                    }
                    "clear" -> {
                        if (file.exists()) file.delete()
                        result.success(true)
                    }
                    else -> result.notImplemented()
                }
            }

        // 外部应用跳转 Channel（抖音 / B站）
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, appChannelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "openSearch" -> {
                        val platform = call.argument<String>("platform").orEmpty()
                        val keyword = call.argument<String>("keyword").orEmpty()
                        val success = openPlatformSearch(platform, keyword)
                        result.success(success)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun openPlatformSearch(platform: String, keyword: String): Boolean {
        return try {
            val encoded = URLEncoder.encode(keyword, "UTF-8")
            val isDouyin = platform.contains("抖音")
            val isBilibili = platform.contains("哔") || platform.contains("b站") || platform.contains("B站")

            if (isDouyin) {
                // 1. 尝试直接以明确包名启动抖音/抖音极速版原生 SearchActivity
                val douyinPackages = listOf("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.lite")
                for (pkg in douyinPackages) {
                    try {
                        val uri = Uri.parse("snssdk1128://search?keyword=$encoded")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage(pkg)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        return true
                    } catch (_: Exception) {
                    }
                }

                // 2. 尝试不指定包名的通用 Scheme
                try {
                    val appUri = Uri.parse("snssdk1128://search?keyword=$encoded")
                    val intent = Intent(Intent.ACTION_VIEW, appUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    return true
                } catch (_: Exception) {
                }

                // 3. 原生未安装或唤起失败时返回 false，由 Flutter 端弹窗温馨提示未安装
                return false
            } else if (isBilibili) {
                // 1. 尝试直接以明确包名启动哔哩哔哩原生 SearchActivity
                val biliPackages = listOf("tv.danmaku.bili", "com.bilibili.app.in")
                for (pkg in biliPackages) {
                    try {
                        val uri = Uri.parse("bilibili://search?keyword=$encoded")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage(pkg)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                        return true
                    } catch (_: Exception) {
                    }
                }

                // 2. 尝试不指定包名的通用 Scheme
                try {
                    val appUri = Uri.parse("bilibili://search?keyword=$encoded")
                    val intent = Intent(Intent.ACTION_VIEW, appUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    return true
                } catch (_: Exception) {
                }

                // 3. 原生未安装或唤起失败时返回 false，由 Flutter 端弹窗温馨提示未安装
                return false
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
