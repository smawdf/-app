package com.myorderapp.ui.adddish

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import com.myorderapp.domain.model.CookStep
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDishScreen(
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    editDishId: String? = null,
    viewModel: AddDishViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("中餐", "西餐", "甜品", "饮品", "日料", "韩餐", "东南亚")
    val categoryExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(editDishId) {
        if (editDishId != null) viewModel.loadDishForEdit(editDishId)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) {
            if (uiState.uploadMessage != null) {
                snackbarHostState.showSnackbar(uiState.uploadMessage!!)
            }
            onSave()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("取消", color = MaterialTheme.colorScheme.primary) }
            Text(if (editDishId != null) "编辑菜品" else "添加菜品",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = { viewModel.save() },
                enabled = !uiState.isSaving
            ) {
                Text(
                    if (uiState.isSaving) "保存中..." else "保存",
                    color = if (uiState.isSaving) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Image Upload
            val context = LocalContext.current
            var showPhotoSheet by remember { mutableStateOf(false) }
            var cameraUri by remember { mutableStateOf<Uri?>(null) }

            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { viewModel.onImageUrlChanged(it.toString()) }
            }

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success ->
                if (success && cameraUri != null) {
                    viewModel.onImageUrlChanged(cameraUri.toString())
                }
            }

            fun launchCamera() {
                val photoFile = File(
                    context.externalCacheDir ?: context.cacheDir,
                    "dish_${System.currentTimeMillis()}.jpg"
                )
                photoFile.parentFile?.mkdirs()
                cameraUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                cameraLauncher.launch(cameraUri!!)
            }

            if (showPhotoSheet) {
                AlertDialog(
                    onDismissRequest = { showPhotoSheet = false },
                    title = { Text("上传展示图", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            TextButton(onClick = { showPhotoSheet = false; launchCamera() },
                                modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("拍照")
                                }
                            }
                            TextButton(onClick = { showPhotoSheet = false; galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("从相册选择")
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { showPhotoSheet = false }) { Text("取消") } }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth().height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showPhotoSheet = true },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = uiState.imageUrl,
                        contentDescription = "展示图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("点击上传展示图", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("拍照 / 从相册选择", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Dish Name
            InputLabel("菜品名称 *")
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入菜品名称", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Category & Difficulty
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    InputLabel("分类")
                    ExposedDropdownMenuBox(expanded = categoryExpanded.value, onExpandedChange = { categoryExpanded.value = it }) {
                        OutlinedTextField(
                            value = uiState.category, onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded.value) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded.value, onDismissRequest = { categoryExpanded.value = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    viewModel.onCategoryChanged(cat)
                                    categoryExpanded.value = false
                                })
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    InputLabel("难度")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (1..5).forEach { level ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "难度 $level",
                                tint = if (level <= uiState.difficulty) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.onDifficultyChanged(level) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Cook Time
            InputLabel("烹饪时间（分钟）")
            OutlinedTextField(
                value = uiState.cookTimeMin, onValueChange = { viewModel.onCookTimeChanged(it) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Who Likes
            InputLabel("谁爱吃")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.whoLikesYou,
                    onClick = { viewModel.toggleWhoLikesYou() },
                    label = { Text("🧑 ${uiState.myName}", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = MaterialTheme.colorScheme.primary)
                )
                FilterChip(
                    selected = uiState.whoLikesPartner,
                    onClick = { viewModel.toggleWhoLikesPartner() },
                    label = { Text("👧 ${uiState.partnerName}", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer, selectedLabelColor = MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Ingredients
            InputLabel("食材清单")
            Row {
                OutlinedTextField(
                    value = uiState.ingredientInput, onValueChange = { viewModel.onIngredientInputChanged(it) },
                    modifier = Modifier.weight(1f), placeholder = { Text("输入食材名称", fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp), singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { viewModel.addIngredient() },
                    shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加食材",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.ingredients.forEachIndexed { index, ingredient ->
                    Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ingredient, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "移除食材",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.removeIngredient(index) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Cook Steps
            InputLabel("制作步骤")
            uiState.cookSteps.forEachIndexed { index, step ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center) {
                                Text("${step.step}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = step.description,
                                onValueChange = { viewModel.updateStep(index, it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("描述这一步的操作...", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp), singleLine = true
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(28.dp),
                                onClick = { viewModel.removeStep(index) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除步骤",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addStep() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("添加步骤", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }

            // Notes
            Spacer(modifier = Modifier.height(16.dp))
            InputLabel("备注")
            OutlinedTextField(
                value = uiState.notes, onValueChange = { viewModel.onNotesChanged(it) },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("输入备注...", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp), minLines = 2
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }
}

@Composable
fun InputLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(4.dp))
}
