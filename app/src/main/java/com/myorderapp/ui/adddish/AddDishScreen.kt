package com.myorderapp.ui.adddish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.myorderapp.domain.model.CookStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDishScreen(
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    viewModel: AddDishViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("中餐", "西餐", "甜品", "饮品", "日料", "韩餐", "东南亚")
    val categoryExpanded = remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) onSave()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("取消", color = Color(0xFFFF6B35)) }
            Text("添加菜品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.save() }) {
                Text("保存", color = Color(0xFFFF6B35), fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Image Upload
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F0EA))
                    .clickable { /* image picker - placeholder for Gallery/Camera intent */ },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 32.sp)
                    Text("点击上传展示图", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("拍照 / 从相册选择 / 输入URL", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
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
                            Text(if (level <= uiState.difficulty) "⭐" else "☆", fontSize = 16.sp,
                                modifier = Modifier.clickable { viewModel.onDifficultyChanged(level) })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Cook Time
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    InputLabel("烹饪时间（分钟）")
                    OutlinedTextField(
                        value = uiState.cookTimeMin, onValueChange = { viewModel.onCookTimeChanged(it) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    InputLabel("份量")
                    OutlinedTextField(
                        value = uiState.servings, onValueChange = { viewModel.onServingsChanged(it) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Who Likes
            InputLabel("谁爱吃")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.whoLikesYou,
                    onClick = { viewModel.toggleWhoLikesYou() },
                    label = { Text("🧑 我", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF3E0), selectedLabelColor = Color(0xFFFF6B35))
                )
                FilterChip(
                    selected = uiState.whoLikesPartner,
                    onClick = { viewModel.toggleWhoLikesPartner() },
                    label = { Text("👧 她", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF3E0), selectedLabelColor = Color(0xFFFF6B35))
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
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFF6B35))
                ) { Text("+", color = Color.White, fontSize = 16.sp) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.ingredients.forEachIndexed { index, ingredient ->
                    Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ingredient, fontSize = 11.sp, color = Color(0xFF555555))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✕", fontSize = 11.sp, modifier = Modifier.clickable { viewModel.removeIngredient(index) })
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
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFFF6B35)),
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
                                color = Color(0xFFFFEBEE),
                                modifier = Modifier.size(28.dp),
                                onClick = { viewModel.removeStep(index) }
                            ) { Box(contentAlignment = Alignment.Center) { Text("🗑", fontSize = 12.sp) } }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addStep() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B35))
            ) { Text("+ 添加步骤", fontWeight = FontWeight.Medium, fontSize = 12.sp) }

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

@Composable
fun InputLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(4.dp))
}
