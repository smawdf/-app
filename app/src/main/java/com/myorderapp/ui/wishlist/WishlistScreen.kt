package com.myorderapp.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.myorderapp.ui.theme.CategoryDisplay

data class WishDish(
    val id: String, val name: String, val category: String, val source: String,
    val addedBy: String, val addedDate: String, val note: String,
    val status: String, val emoji: String, val bgColor: Color
)

@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel = koinViewModel(),
    onDishClick: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    val tabInfo = listOf(
        Triple("pending", "待尝试", uiState.items.size),
        Triple("tried", "已尝试", 0),
        Triple("rejected", "已放弃", 0)
    )

    val filteredItems = uiState.items
    val totalCount = filteredItems.size + 0 // placeholder for total

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp).padding(top = 56.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("💫 心愿单", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("共 ${totalCount} 项", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("待尝试", "已尝试", "已放弃").forEach { tab ->
                val isSelected = tab == uiState.selectedTab
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onTabSelected(tab) },
                    label = { Text(tab, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFF3E0), selectedLabelColor = Color(0xFFFF6B35)),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected,
                        selectedBorderColor = Color(0xFFFF6B35), borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(filteredItems) { wish ->
                val (emoji, bg) = CategoryDisplay.emojiAndBg(wish.dishCategory)
                val wd = WishDish(
                    id = wish.id, name = wish.dishName, category = wish.dishCategory,
                    source = wish.externalSource ?: "自定义菜谱",
                    addedBy = wish.addedByName, addedDate = wish.createdAt, note = wish.notes,
                    status = wish.status, emoji = emoji, bgColor = bg
                )
                WishlistCard(
                    wish = wd,
                    onMarkTried = { viewModel.markTried(wish.id) },
                    onOrderNow = { onDishClick(wish.dishId, "custom") }
                )
            }
        }
    }
}

@Composable
fun WishlistCard(wish: WishDish, onMarkTried: () -> Unit, onOrderNow: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(wish.bgColor),
                contentAlignment = Alignment.Center) { Text(wish.emoji, fontSize = 30.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(wish.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${wish.category} · 来自 ${wish.source}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${wish.addedBy}添加 · ${wish.addedDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (wish.note.isNotBlank()) {
                    Text(wish.note, fontSize = 10.sp, color = Color(0xFFFF6B35), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (wish.status == "pending") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F5E9), onClick = onMarkTried) {
                        Text("✓ 试过了", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 9.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF3E0), onClick = onOrderNow) {
                        Text("🍽 点这个", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 9.sp, color = Color(0xFFFF6B35), fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (wish.status == "tried") {
                Text("好吃！已加入菜品库", fontSize = 10.sp, color = Color(0xFF4CAF50))
            }
        }
    }
}
