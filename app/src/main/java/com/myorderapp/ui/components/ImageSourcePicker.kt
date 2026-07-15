package com.myorderapp.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ImageSourcePickerDialog(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val currentOnImageSelected by rememberUpdatedState(onImageSelected)
    var pendingCameraUri by rememberSaveable { mutableStateOf("") }
    var pendingCameraFile by rememberSaveable { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // The URI remains valid for the current upload even if the picker does not grant persistence.
            }
            currentOnImageSelected(it)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val file = pendingCameraFile.takeIf { it.isNotBlank() }?.let(::File)
        pendingCameraUri = ""
        pendingCameraFile = ""
        if (success && uri != null) {
            currentOnImageSelected(uri)
        } else {
            file?.delete()
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ImageSourceOption(
                    icon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    text = "从相册选择",
                    onClick = {
                        onDismiss()
                        galleryLauncher.launch(arrayOf("image/*"))
                    }
                )
                ImageSourceOption(
                    icon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) },
                    text = "拍照上传",
                    onClick = {
                        onDismiss()
                        try {
                            val directory = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                            val file = File.createTempFile("photo_", ".jpg", directory)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            pendingCameraFile = file.absolutePath
                            pendingCameraUri = uri.toString()
                            cameraLauncher.launch(uri)
                        } catch (_: Exception) {
                            pendingCameraFile.takeIf { it.isNotBlank() }?.let(::File)?.delete()
                            pendingCameraFile = ""
                            pendingCameraUri = ""
                            Toast.makeText(context, "无法打开相机", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun ImageSourceOption(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
