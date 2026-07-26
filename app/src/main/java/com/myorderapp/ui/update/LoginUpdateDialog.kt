package com.myorderapp.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.components.CozySurface
import com.myorderapp.ui.theme.SecondaryContainer

@Composable
fun LoginUpdateDialog(
    state: AppUpdateUiState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val latest = state.latest ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CozySurface,
        title = {
            Text(
                text = "发现新版本 ${latest.versionName}",
                color = CozyCocoa,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(latest.title, color = CozyRose, fontWeight = FontWeight.Bold)
                Text(
                    text = latest.notes.ifBlank { "新版本已经准备好，建议更新后继续使用。" },
                    color = CozyMuted,
                    lineHeight = 21.sp
                )
                if (state.isDownloading) {
                    LinearProgressIndicator(
                        progress = { state.downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = CozyRose,
                        trackColor = SecondaryContainer
                    )
                    Text("正在下载 ${state.downloadProgress}%", color = CozyMuted)
                } else if (state.downloadedApk != null) {
                    Text("更新包已下载，点击安装即可更新。", color = CozyCocoa, fontWeight = FontWeight.Bold)
                } else if (!state.message.isNullOrBlank()) {
                    Text(state.message, color = CozyMuted)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = !state.isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = CozyRose),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        state.isDownloading -> "下载中..."
                        state.downloadedApk != null -> "安装更新"
                        else -> "立即更新"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = CozyMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}
