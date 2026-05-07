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
