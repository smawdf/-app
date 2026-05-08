package com.myorderapp.ui.about

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val version = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        catch (_: Exception) { "1.1.1" }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("关于", style = MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🍽️", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("今天吃什么？", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "v$version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    scope.launch { snackbarHostState.showSnackbar("已经是最新版本了") }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 更新日志
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("更新日志", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    VersionItem("v1.4.1", "2026-05-08 19:50", listOf(
                        "点菜页双方昵称展示",
                        "对方选菜实时可见（3秒同步）"
                    ))
                    VersionItem("v1.4.0", "2026-05-08 19:20", listOf(
                        "单设备登录，新设备登录原设备下线"
                    ))
                    VersionItem("v1.3.2", "2026-05-08 19:08", listOf(
                        "修复点菜页返回键回到餐次选择"
                    ))
                    VersionItem("v1.3.1", "2026-05-08 19:00", listOf(
                        "修复登录页点击注册未跳转"
                    ))
                    VersionItem("v1.3.0", "2026-05-08 18:31", listOf(
                        "配对展示双人心形头像",
                        "配对码长按复制",
                        "点菜页双人左右布局",
                        "删除已提交菜品自动重置"
                    ))
                    VersionItem("v1.2.0", "2026-05-08 15:12", listOf(
                        "搜索页新增\"我的菜单\"数据源 Tab",
                        "自建菜品详情支持加入心愿单",
                        "保存速度大幅提升（图片后台异步上传）",
                        "首页\"最近菜品\"改为\"我的菜单\"",
                        "\"自建\"统一改名为\"我的菜单\""
                    ))
                    VersionItem("v1.1.16", "2026-05-08 14:40", listOf(
                        "修复拍照闪退（FileProvider 路径）",
                        "移除 URL 输入上传选项"
                    ))
                    VersionItem("v1.1.15", "2026-05-08 14:36", listOf(
                        "修复过期 JWT 导致上传 HTTP 400"
                    ))
                    VersionItem("v1.1.14", "2026-05-08 14:32", listOf(
                        "上传失败显示具体错误原因"
                    ))
                    VersionItem("v1.1.13", "2026-05-08 14:29", listOf(
                        "修复上传双 Bearer，公开上传测试"
                    ))
                    VersionItem("v1.1.12", "2026-05-08 14:26", listOf(
                        "修复图片上传双 Bearer 认证失败"
                    ))
                    VersionItem("v1.1.11", "2026-05-08 14:19", listOf(
                        "修复图片上传缺 apikey 导致失败",
                        "修复连击保存导致重复菜品",
                        "保存后自动跳转菜品库",
                        "极速数据图片正常显示",
                        "步骤图展示"
                    ))
                    VersionItem("v1.1.10", "2026-05-08 13:42", listOf(
                        "修复搜索页进入时闪退"
                    ))
                    VersionItem("v1.1.9", "2026-05-08 13:34", listOf(
                        "数据源 Tab 始终显示，切换不消失",
                        "修复快速连击保存导致重复菜品",
                        "保存中按钮禁用，显示\"保存中...\""
                    ))
                    VersionItem("v1.1.8", "2026-05-08 13:17", listOf(
                        "上传失败不再删除菜品，保留本地图片",
                        "上传成功/失败 Snackbar 提示",
                        "数据源 Tab 切换即时过滤",
                        "无结果数据源自动隐藏"
                    ))
                    VersionItem("v1.1.7", "2026-05-08 12:43", listOf(
                        "修复图片上传失败导致菜品图片丢失",
                        "搜索结果按数据源 Tab 切换",
                        "移除菜品详情页\"自定义\"标签"
                    ))
                    VersionItem("v1.1.6", "2026-05-08", listOf(
                        "天行数据 + 极速数据双菜谱搜索源",
                        "三 API 并行搜索，结果合并去重"
                    ))
                    VersionItem("v1.1.5", "2026-05-08", listOf(
                        "退出登录按钮根据登录状态显示",
                        "昵称/头像本地预填充，加载不闪烁"
                    ))
                    VersionItem("v1.1.4", "2026-05-08 02:18", listOf(
                        "移除心愿单内置测试数据",
                        "移除昵称旁编辑图标"
                    ))
                    VersionItem("v1.1.3", "2026-05-08 01:54", listOf(
                        "昵称/头像与用户账号绑定，卸载重装可恢复",
                        "菜品库分类改为下拉框选择",
                        "心愿单全部页签支持长按删除",
                        "更新日志同步到关于页面"
                    ))
                    VersionItem("v1.1.2", "2026-05-08", listOf(
                        "修复昵称/头像与账号绑定，卸载重装可恢复",
                        "菜品库列表展示菜品缩略图",
                        "心愿单点击跳转详情 + 长按删除",
                        "谁爱吃默认不选，选中后显示",
                        "菜品库分类改为下拉框选择",
                        "心愿单已尝试页签支持删除"
                    ))
                    VersionItem("v1.1.1", "2026-05-08", listOf(
                        "搜索优先本地数据库，减少 API 调用",
                        "随机选菜：自定义筛选 + 不重复机制",
                        "移除英文 API，仅保留聚合数据",
                        "新增关于页面",
                        "首页卡片展示菜品图片"
                    ))
                    VersionItem("v1.0.1", "2026-05-08", listOf("修复启动闪退问题"))
                    VersionItem("v1.0.0", "2026-05-07", listOf(
                        "全新 UI 设计（清简日常风格）",
                        "聚合数据 + TheMealDB 菜谱搜索",
                        "三步注册流程 + 配对功能",
                        "菜品编辑、长按删除",
                        "图片云端存储",
                        "自定义口味标签"
                    ))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}

@Composable
private fun VersionItem(version: String, date: String, changes: List<String>) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(version, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        changes.forEach { change ->
            Text("• $change", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
