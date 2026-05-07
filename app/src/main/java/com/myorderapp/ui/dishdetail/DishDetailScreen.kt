package com.myorderapp.ui.dishdetail

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myorderapp.domain.model.CookStep
import com.myorderapp.ui.theme.CategoryDisplay
import org.koin.androidx.compose.koinViewModel

@Composable
fun DishDetailScreen(
    dishId: String,
    source: String,
    onBack: () -> Unit = {},
    viewModel: DishDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(dishId) {
        viewModel.loadDish(dishId)
    }

    val dish = uiState.dish
    val isCustom = dish?.source == "custom"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(CategoryDisplay.bgColor(dish?.category ?: "")),
            contentAlignment = Alignment.Center
        ) {
            if (dish?.imageUrl != null) {
                AsyncImage(
                    model = dish.imageUrl,
                    contentDescription = dish.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(CategoryDisplay.emoji(dish?.category ?: ""), fontSize = 60.sp)
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.3f),
                onClick = onBack
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("←", color = Color.White, fontSize = 14.sp)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (isCustom) "✏️" else "🤍", fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!isCustom) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("↗️", fontSize = 14.sp)
                        }
                    }
                }
            }
            if (isCustom) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text("自定义",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (dish != null) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(dish.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isCustom) "${dish.category} · 来自 ${dish.createdBy} · ${dish.createdAt}"
                    else "${dish.category} · 来自 ${dish.externalSource ?: "外部"}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("⏱ ${dish.cookTimeMin}分钟", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondaryContainer)
                    Chip("⭐".repeat(dish.difficulty), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondaryContainer)
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text("🥬 食材清单", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        dish.ingredients.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { ingredient ->
                                    Text("• $ingredient", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text("👨‍🍳 制作步骤", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                dish.cookSteps.forEach { step ->
                    CookStepCard(step)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (dish.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("💡 备注：${dish.notes}",
                            modifier = Modifier.padding(12.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isCustom) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.addToWishlist() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("💫 加入心愿单", fontSize = 13.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun Chip(text: String, textColor: Color, bgColor: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = bgColor) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
fun CookStepCard(step: CookStep) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center) {
                Text("${step.step}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(step.description, fontSize = 12.sp)
                if (step.tip != null) {
                    Text(step.tip!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
