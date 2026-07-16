package com.myorderapp.ui.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.R
import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.ui.components.CozyCard
import com.myorderapp.ui.components.CozyCherry
import com.myorderapp.ui.components.CozyCocoa
import com.myorderapp.ui.components.CozyIconBadge
import com.myorderapp.ui.components.CozyMuted
import com.myorderapp.ui.components.CozyMotion
import com.myorderapp.ui.components.CozyMotionVisibility
import com.myorderapp.ui.components.CozyPage
import com.myorderapp.ui.components.CozyPill
import com.myorderapp.ui.components.CozyPrimaryButton
import com.myorderapp.ui.components.CozyRose
import com.myorderapp.ui.components.CozySurface
import com.myorderapp.ui.components.CozyTerracotta
import com.myorderapp.ui.components.cozyTextFieldColors
import com.myorderapp.ui.components.cozyPulseOnChange
import com.myorderapp.ui.shop.components.CartSheet
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private val FloatingBottomNavHeight = 68.dp
private val FloatingBottomNavMargin = 14.dp
private val FloatingCartGap = 8.dp
private val FloatingCartHeight = 66.dp
private val OrderingSurface = Color(0xFFFEF8F2)
private val OrderingHandDrawnBorder = Color(0xFF78555E)
private const val SHOP_MENU_REFRESH_INTERVAL_MS = 10_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderingScreen(
    viewModel: OrderingViewModel = koinViewModel(),
    onShopNameClick: () -> Unit = {},
    onManageMenuClick: () -> Unit = {},
    onCheckoutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCartSheet by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<MenuItem?>(null) }
    var cartFlyStart by remember { mutableStateOf<Offset?>(null) }
    var cartIconCenter by remember { mutableStateOf<Offset?>(null) }
    var pageRootOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(viewModel) {
        while (true) {
            viewModel.refreshShopAndMenuFromCloud()
            delay(SHOP_MENU_REFRESH_INTERVAL_MS)
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            containerColor = Color(0xFFFEF8F2),
            dragHandle = {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF5E4B52).copy(alpha = 0.76f),
                    modifier = Modifier
                        .padding(top = 18.dp, bottom = 14.dp)
                        .size(width = 58.dp, height = 6.dp)
                ) {}
            }
        ) {
            CartSheet(
                cartState = uiState.cartState,
                onDecrease = viewModel::decrease,
                onIncrease = viewModel::increase,
                onCheckout = {
                    if (uiState.isEater) {
                        showCartSheet = false
                        onCheckoutClick()
                    }
                }
            )
        }
    }

    detailItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { detailItem = null }) {
            OrderingDishDetailSheet(
                item = item,
                canOrder = uiState.isEater,
                showDescription = uiState.isEater,
                onAdd = {
                    if (viewModel.addToCart(item)) detailItem = null
                },
                onClose = { detailItem = null }
            )
        }
    }

    CozyPage(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            pageRootOffset = coordinates.positionInRoot()
        },
        decorative = false
    ) {
        val navigationBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val floatingNavClearance = navigationBottomPadding + FloatingBottomNavMargin + FloatingBottomNavHeight
        val cartBottomOffset = floatingNavClearance + FloatingCartGap
        val bottomClearance = if (uiState.cartState.isEmpty) {
            floatingNavClearance + 16.dp
        } else {
            cartBottomOffset + FloatingCartHeight + 18.dp
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ShopCard(
                shopName = uiState.shopName.ifBlank { "我们的小饭桌" },
                bannerImageUrl = uiState.shopCoverUrl,
                announcement = uiState.shopAnnouncement.ifBlank { "今天也给你准备了好吃的 ✨" },
                canManageShop = !uiState.isEater,
                onManageShopClick = onShopNameClick
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OrderingSearchBar(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                val menuMaxWidth = if (maxWidth >= 600.dp) 760.dp else maxWidth
                Row(
                    modifier = Modifier
                        .widthIn(max = menuMaxWidth)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    CategoryRail(
                        categories = uiState.orderingCategories,
                        selectedCategory = uiState.selectedCategory,
                        bottomClearance = bottomClearance,
                        onSelect = viewModel::selectCategory
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        DishList(
                            items = uiState.visibleItems,
                            canOrder = uiState.isEater,
                            showDescription = uiState.isEater,
                            onAdd = { item, start ->
                                if (viewModel.addToCart(item)) {
                                    cartFlyStart = start - pageRootOffset
                                }
                            },
                            onDishClick = { detailItem = it },
                            onManageMenuClick = onManageMenuClick,
                            bottomClearance = bottomClearance,
                            modifier = Modifier
                                .widthIn(max = 440.dp)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !uiState.cartState.isEmpty,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = CozyMotion.fadeUp(offset = 36, durationMillis = CozyMotion.Standard),
            exit = fadeOut(tween(CozyMotion.Quick, easing = FastOutSlowInEasing)) +
                slideOutVertically(tween(CozyMotion.Quick, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            CartFloatingBar(
                count = uiState.cartState.itemCount,
                totalPrice = uiState.cartState.totalPrice,
                bottomOffset = cartBottomOffset,
                onCartIconPositioned = { cartIconCenter = it - pageRootOffset },
                onCartClick = { showCartSheet = true },
                onCheckoutClick = {
                    if (uiState.isEater) onCheckoutClick()
                }
            )
        }

        val target = cartIconCenter
        if (cartFlyStart != null && target != null) {
            CartFlyToBasketAnimation(
                start = cartFlyStart!!,
                target = target,
                onFinished = { cartFlyStart = null }
            )
        }
    }
}

@Composable
private fun OrderingSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = cozyTextFieldColors(),
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = CozyRose, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "清空搜索", tint = CozyMuted, modifier = Modifier.size(18.dp))
                }
            }
        },
        placeholder = {
            Text("搜索我的店铺菜品", color = CozyMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ShopCard(
    shopName: String,
    bannerImageUrl: String,
    announcement: String,
    canManageShop: Boolean,
    onManageShopClick: () -> Unit
) {
    val displayShopName = shopName.trim().ifBlank { "我们的小饭桌" }
    val displayAnnouncement = announcement.trim().ifBlank { "今天也给你准备了好吃的" }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val expanded = maxWidth >= 600.dp
        val coverWidth = if (expanded) 96.dp else 68.dp
        val coverHeight = if (expanded) 72.dp else 62.dp
        SquishyOrderSurface(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .heightIn(min = if (expanded) 96.dp else 86.dp)
                .padding(horizontal = 20.dp, vertical = 4.dp),
            radius = 18,
            containerColor = Color(0xFFFFFCF8),
            borderColor = OrderingHandDrawnBorder.copy(alpha = 0.48f),
            onClick = null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CozyCherry.copy(alpha = 0.56f),
                    border = BorderStroke(1.dp, OrderingHandDrawnBorder.copy(alpha = 0.36f)),
                    modifier = Modifier.size(width = coverWidth, height = coverHeight)
                ) {
                    AsyncImage(
                        model = bannerImageUrl.takeIf { it.isNotBlank() } ?: R.drawable.shop_banner_stitch,
                        contentDescription = displayShopName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = displayShopName,
                        color = CozyCocoa,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = CozyRose,
                            modifier = Modifier.padding(top = 1.dp).size(15.dp)
                        )
                        Text(
                            text = displayAnnouncement,
                            color = OrderingHandDrawnBorder,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (canManageShop) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CozyCherry.copy(alpha = 0.72f)
                        ) {
                            Text(
                                text = "浏览模式",
                                color = CozyRose,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (canManageShop) {
                    IconButton(
                        onClick = onManageShopClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "管理店铺",
                            tint = CozyRose,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<MenuCategory>,
    selectedCategory: String,
    bottomClearance: Dp,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(96.dp)
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = 8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomClearance),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedCategory
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onSelect(category.id) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) CozyCherry else Color.Transparent,
                border = BorderStroke(1.dp, if (selected) CozyRose.copy(alpha = 0.24f) else Color.Transparent),
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = category.decoratedCategoryName(),
                        color = if (selected) CozyRose else CozyMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

private fun MenuCategory.decoratedCategoryName(): String {
    return when {
        id == ORDERING_HOT_CATEGORY_ID || name.contains("热") || name.contains("招牌") -> "热销"
        name.contains("主") || name.contains("披萨") || name.contains("饭") || name.contains("面") -> "主食"
        name.contains("饮") || name.contains("喝") || name.contains("咖啡") || name.contains("茶") -> "饮品"
        name.contains("甜") || name.contains("蛋糕") || name.contains("布丁") -> "甜点"
        else -> name.take(3)
    }
}

@Composable
private fun DishList(
    items: List<MenuItem>,
    canOrder: Boolean,
    showDescription: Boolean,
    onAdd: (MenuItem, Offset) -> Unit,
    onDishClick: (MenuItem) -> Unit,
    onManageMenuClick: () -> Unit,
    bottomClearance: Dp,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        EmptyMenu(onManageMenuClick = onManageMenuClick, bottomClearance = bottomClearance, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 0.dp, bottom = bottomClearance + 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(items, key = { it.id }) { item ->
            CozyMotionVisibility(delayMillis = (items.indexOf(item).coerceAtMost(4)) * 28) {
                SingleShopDishCard(
                    item = item,
                    canOrder = canOrder,
                    showDescription = showDescription,
                    onClick = { onDishClick(item) },
                    onAdd = { start -> onAdd(item, start) }
                )
            }
        }
    }
}

@Composable
private fun SingleShopDishCard(
    item: MenuItem,
    canOrder: Boolean,
    showDescription: Boolean,
    onClick: () -> Unit,
    onAdd: (Offset) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.68f),
        border = BorderStroke(2.dp, Color.White),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CozyCherry.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = CozyRose, modifier = Modifier.size(40.dp))
                        Text("暂无图片", color = CozyMuted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.name,
                    color = CozyCocoa,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (showDescription) {
                    Text(item.description.ifBlank { item.subtitle }.ifBlank { "今天也很适合点这一道" }, color = CozyMuted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    Text(priceYuanText(item.price), color = CozyRose, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    AddDishButton(enabled = canOrder, onClick = onAdd)
                }
            }
        }
    }
}

@Composable
private fun CartFloatingBar(
    count: Int,
    totalPrice: Double,
    modifier: Modifier = Modifier,
    bottomOffset: Dp,
    onCartIconPositioned: (Offset) -> Unit,
    onCartClick: () -> Unit,
    onCheckoutClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(start = 20.dp, end = 20.dp, bottom = bottomOffset)
            .fillMaxWidth()
            .height(FloatingCartHeight),
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFFFFCF8),
        border = BorderStroke(1.dp, Color(0xFFD6C1C5).copy(alpha = 0.82f)),
        shadowElevation = 0.dp,
        onClick = onCartClick
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(start = 26.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .onGloballyPositioned { coordinates ->
                        val topLeft = coordinates.positionInRoot()
                        onCartIconPositioned(
                            Offset(
                                x = topLeft.x + coordinates.size.width / 2f,
                                y = topLeft.y + coordinates.size.height / 2f
                            )
                        )
                    }
                    .cozyPulseOnChange(count, targetScale = 1.1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ShoppingBag, contentDescription = "购物篮", tint = CozyRose, modifier = Modifier.size(30.dp))
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp).size(20.dp).cozyPulseOnChange(count, targetScale = 1.16f),
                    shape = CircleShape,
                    color = CozyRose
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(count.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(priceYuanText(totalPrice), color = CozyCocoa, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f).padding(start = 14.dp))
            SquishyCheckoutButton(text = "去结算", onClick = onCheckoutClick)
        }
    }
}

@Composable
private fun SquishyCheckoutButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .height(54.dp)
            .scale(if (pressed) CozyMotion.PressedScale else 1f)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(999.dp),
            color = CozyRose
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 34.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
private fun EmptyMenu(onManageMenuClick: () -> Unit, bottomClearance: Dp, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = bottomClearance),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CozyIconBadge(Icons.Filled.Restaurant, background = CozyCherry, tint = CozyRose, modifier = Modifier.size(62.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("还没有菜品", color = CozyCocoa, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(6.dp))
        Text("先添加菜名、价格、图片和分类，然后就能开始下单。", color = CozyMuted, textAlign = TextAlign.Center, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        CozyPrimaryButton(text = "去设置菜品", onClick = onManageMenuClick, modifier = Modifier.fillMaxWidth())
    }
}

private fun priceYuanText(value: Double): String {
    val cents = kotlin.math.round(value * 100).toLong()
    return if (cents % 100L == 0L) {
        "¥ ${cents / 100L}"
    } else {
        "¥ %.2f".format(value)
    }
}

@Composable
private fun CartFlyToBasketAnimation(
    start: Offset,
    target: Offset,
    onFinished: () -> Unit
) {
    val x = remember(start) { Animatable(start.x) }
    val y = remember(start) { Animatable(start.y) }
    val scale = remember(start) { Animatable(1f) }
    val dotRadius = with(LocalDensity.current) { 14.dp.toPx() }
    val density = LocalDensity.current
    val jumpPeakY = start.y - with(density) { 72.dp.toPx() }
    val jumpPeakX = start.x - with(density) { 30.dp.toPx() }

    LaunchedEffect(start, target) {
        x.animateTo(jumpPeakX, animationSpec = tween(durationMillis = CozyMotion.Standard, easing = FastOutSlowInEasing))
        x.animateTo(target.x, animationSpec = tween(durationMillis = CozyMotion.CartFly, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(start, target) {
        y.animateTo(jumpPeakY, animationSpec = tween(durationMillis = CozyMotion.Standard, easing = FastOutSlowInEasing))
        y.animateTo(target.y, animationSpec = tween(durationMillis = CozyMotion.CartFly, easing = FastOutSlowInEasing))
        onFinished()
    }
    LaunchedEffect(start) {
        scale.animateTo(1.1f, animationSpec = tween(durationMillis = CozyMotion.Standard, easing = FastOutSlowInEasing))
        scale.animateTo(0.72f, animationSpec = tween(durationMillis = CozyMotion.CartFly, easing = FastOutSlowInEasing))
    }

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (x.value - dotRadius).roundToInt(),
                    y = (y.value - dotRadius).roundToInt()
                )
            }
            .size(28.dp)
            .scale(scale.value),
        shape = CircleShape,
        color = CozyRose,
        border = BorderStroke(2.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AddDishButton(enabled: Boolean, onClick: (Offset) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier.size(36.dp).scale(if (pressed) CozyMotion.PressedScale else 1f)) {
        Surface(
            modifier = Modifier
                .matchParentSize()
                .onGloballyPositioned { coordinates ->
                    val topLeft = coordinates.positionInRoot()
                    centerInRoot = Offset(
                        x = topLeft.x + coordinates.size.width / 2f,
                        y = topLeft.y + coordinates.size.height / 2f
                    )
                }
                .clickable(
                    enabled = enabled,
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onClick(centerInRoot) }
                ),
            shape = CircleShape,
            color = if (enabled) CozyRose else CozyMuted.copy(alpha = 0.42f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = "加入购物篮", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SquishyOrderSurface(
    modifier: Modifier = Modifier,
    radius: Int,
    containerColor: Color,
    borderColor: Color,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Surface(
        modifier = modifier
            .scale(if (pressed && onClick != null) CozyMotion.SoftPressedScale else 1f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(radius.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

@Composable
private fun OrderingDishDetailSheet(
    item: MenuItem,
    canOrder: Boolean,
    showDescription: Boolean,
    onAdd: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp)).background(CozyCherry),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(model = item.imageUrl, contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Restaurant, contentDescription = null, tint = CozyRose, modifier = Modifier.size(44.dp))
                    Text("暂无图片", color = CozyMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(item.name, color = CozyCocoa, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        if (showDescription) {
            Text(item.description.ifBlank { item.subtitle }.ifBlank { "暂无描述" }, color = CozyMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CozyPill(item.categoryId, color = CozyTerracotta)
            Text("小店在售", color = CozyMuted)
        }
        Text(priceYuanText(item.price), color = CozyRose, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("关闭", color = CozyMuted) }
            Button(
                onClick = onAdd,
                enabled = canOrder,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CozyRose, contentColor = Color.White)
            ) {
                Text(if (canOrder) "加入购物篮" else "吃货专属", fontWeight = FontWeight.Black)
            }
        }
    }
}
