package com.myorderapp.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.ui.components.OrderSearchField
import com.myorderapp.ui.shop.components.CartSheet
import org.koin.androidx.compose.koinViewModel

private val OrderingPrimary = Color(0xFF247A84)
private val OrderingInk = Color(0xFF213138)
private val OrderingHeart = Color(0xFFFF8585)
private val OrderingWarm = Color(0xFFFFE0AF)
private val CartBadgeRed = OrderingHeart
private val CategoryRailBackground = Color(0xFFE4F7F4)
private val ContentBackground = Color(0xFFFFF7E8)
private val NoticeBackground = Color(0xFFFFF0D2)
private val MutedText = Color(0xFF72858B)
private const val FallbackBannerImageUrl =
    "https://images.unsplash.com/photo-1543353071-10c8ba85a904?auto=format&fit=crop&w=1200&q=80"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderingScreen(
    viewModel: OrderingViewModel = koinViewModel(),
    onShopNameClick: () -> Unit = {},
    onManageMenuClick: () -> Unit = {},
    onCheckoutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCartSheet by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<MenuItem?>(null) }

    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            CartSheet(
                cartState = uiState.cartState,
                onDecrease = viewModel::decrease,
                onIncrease = viewModel::increase,
                onCheckout = {
                    showCartSheet = false
                    onCheckoutClick()
                }
            )
        }
    }

    detailItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { detailItem = null }) {
            OrderingDishDetailSheet(
                item = item,
                onAdd = {
                    viewModel.addToCart(item)
                    detailItem = null
                },
                onClose = { detailItem = null }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEEF9FB),
                        Color(0xFFFFF7E8),
                        Color(0xFFFFE2C6)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ShopBanner(
                shopName = uiState.shopName,
                bannerImageUrl = uiState.shopCoverUrl,
                onShopNameClick = onShopNameClick
            )
            OrderingSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange
            )
            NoticePromotionBar()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = if (uiState.cartState.isEmpty) 0.dp else 64.dp)
            ) {
                CategoryRail(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onSelect = viewModel::selectCategory
                )
                DishList(
                    items = uiState.visibleItems,
                    onAdd = viewModel::addToCart,
                    onDishClick = { detailItem = it },
                    onManageMenuClick = onManageMenuClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!uiState.cartState.isEmpty) {
            CartFloatingBar(
                count = uiState.cartState.itemCount,
                totalPrice = uiState.cartState.totalPrice,
                modifier = Modifier.align(Alignment.BottomCenter),
                onCartClick = { showCartSheet = true },
                onCheckoutClick = onCheckoutClick
            )
        }
    }
}

@Composable
private fun ShopBanner(
    shopName: String,
    bannerImageUrl: String,
    onShopNameClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .background(OrderingPrimary)
        ) {
            AsyncImage(
                model = bannerImageUrl.ifBlank { FallbackBannerImageUrl },
                contentDescription = shopName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                OrderingInk.copy(alpha = 0.64f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = shopName,
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onShopNameClick)
                )
                Text(
                    text = "两个人的今日菜单 · 现点现做",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

    }
}

@Composable
private fun OrderingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(color = Color.Transparent) {
        OrderSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "搜索本店菜品",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun NoticePromotionBar() {
    Surface(color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .height(34.dp)
                .background(NoticeBackground.copy(alpha = 0.92f))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = OrderingPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "小狗厨师提醒：今天也要好好吃饭，现点现做",
                color = OrderingInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<MenuCategory>,
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(86.dp)
            .fillMaxHeight()
            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.74f)),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedCategory
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelect(category.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(if (selected) OrderingPrimary else Color.Transparent)
                )
                Text(
                    text = category.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    color = if (selected) OrderingPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DishList(
    items: List<MenuItem>,
    onAdd: (MenuItem) -> Unit,
    onDishClick: (MenuItem) -> Unit,
    onManageMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        EmptyMenu(onManageMenuClick = onManageMenuClick, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Transparent),
        contentPadding = PaddingValues(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { item ->
            SingleShopDishCard(
                item = item,
                onClick = { onDishClick(item) },
                onAdd = { onAdd(item) }
            )
        }
    }
}

@Composable
private fun SingleShopDishCard(
    item: MenuItem,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DishImage(item = item)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = item.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.description.ifBlank { item.subtitle },
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "月售 ${item.monthlySales}",
                        color = MutedText,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = priceYuanText(item.price),
                        color = OrderingHeart,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(34.dp))
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(30.dp)
                    .clickable(onClick = onAdd),
                shape = CircleShape,
                color = OrderingPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "加入购物车",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DishImage(item: MenuItem) {
    if (item.imageUrl.isNotBlank()) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(18.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(OrderingWarm.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Restaurant,
                contentDescription = null,
                tint = OrderingPrimary,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun CartFloatingBar(
    count: Int,
    totalPrice: Double,
    modifier: Modifier = Modifier,
    onCartClick: () -> Unit,
    onCheckoutClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(22.dp),
        color = OrderingInk,
        shadowElevation = 10.dp,
        onClick = onCartClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = if (count > 0) OrderingPrimary else Color(0xFF8E8E8E),
                    modifier = Modifier.size(28.dp)
                )
                if (count > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-5).dp)
                            .size(18.dp),
                        shape = CircleShape,
                        color = CartBadgeRed
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = count.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Text(
                text = priceYuanText(totalPrice),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 8.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )

            Button(
                onClick = onCheckoutClick,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(44.dp)
                    .width(112.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrderingPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = OrderingPrimary.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.88f)
                ),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Text(
                    text = "去结算",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun EmptyMenu(
    onManageMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
            modifier = modifier
                .fillMaxSize()
            .background(Color.Transparent)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Restaurant,
            contentDescription = null,
            tint = OrderingPrimary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("还没有菜品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "先添加菜名、价格、图片和分类，然后就能开始下单。",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onManageMenuClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrderingPrimary, contentColor = Color.White)
        ) {
            Text("去设置菜品")
        }
    }
}

private fun priceYuanText(value: Double): String = "¥%.2f".format(value)

@Composable
private fun OrderingDishDetailSheet(
    item: MenuItem,
    onAdd: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = OrderingPrimary, modifier = Modifier.size(42.dp))
            }
        }
        Text(item.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(item.description.ifBlank { item.subtitle }, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(999.dp), color = NoticeBackground) {
                Text(item.categoryId, color = OrderingPrimary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            Text("月售 ${item.monthlySales}", color = MutedText)
        }
        Text(priceYuanText(item.price), color = OrderingHeart, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("关闭") }
            Button(
                onClick = onAdd,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrderingPrimary, contentColor = Color.White)
            ) {
                Text("加入购物车")
            }
        }
    }
}
