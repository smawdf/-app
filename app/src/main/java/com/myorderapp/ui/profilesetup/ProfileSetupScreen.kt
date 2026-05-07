package com.myorderapp.ui.profilesetup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.myorderapp.ui.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun ProfileSetupScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    var nickname by remember { mutableStateOf(profile?.nickname ?: "") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarLocalPath by remember { mutableStateOf(profile?.avatarUrl ?: "") }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it
            avatarLocalPath = it.toString()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            avatarUri = cameraUri
            avatarLocalPath = cameraUri.toString()
        }
    }

    fun launchCamera() {
        val photoFile = File(
            context.externalCacheDir ?: context.cacheDir,
            "avatar_${System.currentTimeMillis()}.jpg"
        )
        photoFile.parentFile?.mkdirs()
        cameraUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraUri!!)
    }

    // Photo picker bottom sheet
    if (showPhotoSheet) {
        AlertDialog(
            onDismissRequest = { showPhotoSheet = false },
            title = { Text("设置头像", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButton(
                        onClick = { showPhotoSheet = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📷", style = MaterialTheme.typography.labelMedium)
                            Text("拍照", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    TextButton(
                        onClick = { showPhotoSheet = false; galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🖼️", style = MaterialTheme.typography.labelMedium)
                            Text("从相册选择", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSheet = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "🎉",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "完善你的资料",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "设置头像和昵称，让伴侣认识你",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Avatar area
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showPhotoSheet = true },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null && avatarLocalPath.isNotBlank()) {
                    AsyncImage(
                        model = avatarLocalPath,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "📷",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            "点击设置头像",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nickname input
            OutlinedTextField(
                value = nickname,
                onValueChange = { if (it.length <= 12) nickname = it },
                placeholder = { Text("输入你的昵称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (nickname.isBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "昵称不能为空",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Bottom area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (nickname.isNotBlank()) {
                            viewModel.updateNickname(nickname)
                            if (avatarLocalPath.isNotBlank()) {
                                viewModel.updateAvatar(avatarLocalPath)
                            }
                            onComplete()
                        }
                    },
                    enabled = nickname.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "完成",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onComplete) {
                    Text(
                        "稍后设置",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
