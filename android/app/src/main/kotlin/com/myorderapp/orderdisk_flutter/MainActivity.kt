package com.myorderapp.orderdisk_flutter

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File

/**
 * 自研极简本地存储桥接。
 *
 * 之所以不使用 shared_preferences / path_provider 等插件：
 * 本机 Windows 未开启开发者模式，Flutter 插件构建需要符号链接支持会直接失败。
 * 这里用系统自带能力实现同等效果（读写 App 私有目录下的一个 JSON 文件），
 * 零第三方依赖、零额外构建要求。
 */
class MainActivity : FlutterActivity() {

    private val channelName = "com.myorderapp.orderdisk_flutter/store"
    private val fileName = "orderdisk_local_store.json"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
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
    }
}
