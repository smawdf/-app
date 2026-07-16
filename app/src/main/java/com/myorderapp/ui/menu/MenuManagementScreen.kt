package com.myorderapp.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.R
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.ui.components.CozyMainTopBar
import com.myorderapp.ui.components.ImageSourcePickerDialog
import com.myorderapp.ui.components.OrderGuidanceEmptyState
import com.myorderapp.ui.util.yuanText
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

private val PrimaryBlue = Color(0xFF894C5C)
private val SuccessGreen = Color(0xFFF4A7B9)
private val DangerRed = Color(0xFFF53F3F)
private val SignatureOrange = Color(0xFFF8A98E)
private val TextPrimary = Color(0xFF1D1B18)
private val TextSecondary = Color(0xFF524346)
private val PageBackground = Color(0xFFFEF8F2)
private val CardBackground = Color(0xFFFFFCF8)
private val HoverBackground = Color(0xFFFFD1DC)
private val BorderColor = Color(0xFFD6C1C5)
private const val SHOP_MENU_REFRESH_INTERVAL_MS = 10_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    viewModel: MenuManagementViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isCompact = LocalConfiguration.current.screenWidthDp < 720
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showShopSettingsDialog by remember { mutableStateOf(false) }
    var showDishImageSourcePicker by remember { mutableStateOf(false) }
    var showShopImageSourcePicker by remember { mutableStateOf(false) }
    var shopImagePreview by remember { mutableStateOf<String?>(null) }
    var categoryDeleteTarget by remember { mutableStateOf<String?>(null) }
    var dishDeleteTarget by remember { mutableStateOf<MenuDishEntity?>(null) }

    LaunchedEffect(viewModel) {
        while (true) {
            viewModel.refreshShopAndMenuFromCloud()
            delay(SHOP_MENU_REFRESH_INTERVAL_MS)
        }
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message == "已新增菜品") {
            delay(1200)
            viewModel.dismissMessage()
        }
    }

    ImageSourcePickerDialog(
        visible = showDishImageSourcePicker,
        title = "选择菜品图片",
        onDismiss = { showDishImageSourcePicker = false },
        onImageSelected = { viewModel.onImagePicked(context, it) }
    )
    ImageSourcePickerDialog(
        visible = showShopImageSourcePicker,
        title = "选择店铺封面",
        onDismiss = { showShopImageSourcePicker = false },
        onImageSelected = {
            shopImagePreview = it.toString()
            viewModel.updateShopImage(context, it)
        }
    )

    if (uiState.isEditing) {
        DishEditorDialog(
            state = uiState.editor,
            categories = uiState.categories,
            message = uiState.message,
            onNameChange = viewModel::onNameChange,
            onPriceChange = viewModel::onPriceChange,
            onOriginPriceChange = viewModel::onOriginPriceChange,
            onImageChange = viewModel::onImageChange,
            onCategoryChange = viewModel::onCategoryChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onStockChange = viewModel::onStockChange,
            onAvailabilityChange = viewModel::onEditorAvailabilityChange,
            onSignatureChange = viewModel::onEditorSignatureChange,
            onPickImage = { showDishImageSourcePicker = true },
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::saveDish,
            onCreateCategoryAndSave = viewModel::saveDishWithCategoryCreation
        )
    }

    if (showCategoryDialog) {
        NewCategoryDialog(
            onDismiss = { showCategoryDialog = false },
            onSave = { name ->
                viewModel.createCategory(name)
                showCategoryDialog = false
            }
        )
    }

    if (showCategoryManagerDialog) {
        CategoryManagerDialog(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onDismiss = { showCategoryManagerDialog = false },
            onCreate = viewModel::createCategory,
            onRename = viewModel::renameCategory,
            onDelete = { categoryDeleteTarget = it }
        )
    }

    categoryDeleteTarget?.let { category ->
        DeleteDishDialog(
            title = "删除分类？",
            body = "该分类下的菜品会移动到其他分类，不会删除菜品。",
            confirmText = "删除分类",
            onDismiss = { categoryDeleteTarget = null },
            onConfirm = {
                viewModel.deleteCategory(category)
                categoryDeleteTarget = null
            }
        )
    }

    if (showShopSettingsDialog && !showShopImageSourcePicker) {
        ShopSettingsDialog(
            shopName = uiState.shopNameDraft,
            shopImageUrl = shopImagePreview ?: uiState.shopImageUrl,
            announcement = uiState.shopAnnouncementDraft,
            onShopNameChange = viewModel::onShopNameChange,
            onAnnouncementChange = viewModel::onShopAnnouncementChange,
            onPickImage = { showShopImageSourcePicker = true },
            onDismiss = {
                viewModel.resetShopSettingsDrafts()
                shopImagePreview = null
                showShopSettingsDialog = false
            },
            onSave = {
                viewModel.saveShopSettings()
                shopImagePreview = null
                showShopSettingsDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StoreTopBar()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentPadding = PaddingValues(bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ShopSettingsStrip(
                        shopName = uiState.shopName,
                        shopImageUrl = uiState.shopImageUrl,
                        announcement = uiState.shopAnnouncement,
                        onEdit = {
                            viewModel.resetShopSettingsDrafts()
                            shopImagePreview = null
                            showShopSettingsDialog = true
                        }
                    )
                }
                item {
                    CategoryManagementBento(
                        categories = uiState.categories,
                        dishCountByCategory = uiState.dishes.groupingBy { it.category }.eachCount(),
                        onManageCategoriesClick = { showCategoryManagerDialog = true },
                        onCreateCategoryClick = { showCategoryDialog = true }
                    )
                }
                item {
                    DishManagementHeader(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = viewModel::selectFilter
                    )
                }
                item {
                    MenuContent(
                        state = uiState,
                        isCompact = isCompact,
                        onToggleSelection = viewModel::toggleDishSelection,
                        onToggleAvailability = viewModel::toggleDishAvailability,
                        onEdit = viewModel::editDish,
                        onDelete = { dishDeleteTarget = it },
                        onShowAddSheet = viewModel::newDish,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        FloatingAddDishButton(
            onClick = viewModel::newDish,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp)
        )
        if (uiState.message == "已新增菜品") {
            AddDishSuccessToast(modifier = Modifier.align(Alignment.Center))
        }
    }

    dishDeleteTarget?.let { dish ->
        DeleteDishDialog(
            title = "删除菜品？",
            body = "删除后会从我的店铺移除「${dish.name}」，购物车里对应菜品也会失效。这个操作不能撤回。",
            confirmText = "删除菜品",
            onDismiss = { dishDeleteTarget = null },
            onConfirm = {
                viewModel.deleteDish(dish.id)
                dishDeleteTarget = null
            }
        )
    }
}

@Composable
private fun AddDishSuccessToast(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Text("已新增菜品", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ShopSettingsStrip(
    shopName: String,
    shopImageUrl: String,
    announcement: String,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp)
            ) {
                if (shopImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = shopImageUrl,
                        contentDescription = shopName.ifBlank { "店铺图片" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.shop_banner_stitch),
                        contentDescription = shopName.ifBlank { "店铺图片" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = shopName.ifBlank { "我的小店" },
                        color = TextPrimary,
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        onClick = onEdit,
                        shape = RoundedCornerShape(999.dp),
                        color = PrimaryBlue.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(17.dp))
                            Text("编辑店铺", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF0F4),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Icon(
                            Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.padding(top = 1.dp).size(18.dp)
                        )
                        Text(
                            text = announcement.ifBlank { "欢迎光临" },
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreTopBar() {
    CozyMainTopBar(title = "我的店铺")
    return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "喜欢", tint = PrimaryBlue, modifier = Modifier.size(28.dp))
        }
        Text(
            text = "我的店铺",
            color = TextPrimary,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.NotificationsNone, contentDescription = "通知", tint = PrimaryBlue, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun CategoryManagementBento(
    categories: List<String>,
    dishCountByCategory: Map<String, Int>,
    onManageCategoriesClick: () -> Unit,
    onCreateCategoryClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "分类管理",
                color = TextPrimary,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onManageCategoriesClick) {
                Text("管理全部分类", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("›", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
        val visibleCards = categories.map { CategoryCardModel.Category(it) } + CategoryCardModel.Create
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(visibleCards, key = { card ->
                when (card) {
                    is CategoryCardModel.Category -> card.name
                    CategoryCardModel.Create -> "create-category"
                }
            }) { card ->
                CategoryBentoCard(
                    card = card,
                    dishCountByCategory = dishCountByCategory,
                    onCreateCategoryClick = onCreateCategoryClick,
                    modifier = Modifier.width(154.dp)
                )
            }
        }
    }
}

private sealed class CategoryCardModel {
    data class Category(val name: String) : CategoryCardModel()
    data object Create : CategoryCardModel()
}

@Composable
private fun CategoryBentoCard(
    card: CategoryCardModel,
    dishCountByCategory: Map<String, Int>,
    onCreateCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCreate = card is CategoryCardModel.Create
    val category = (card as? CategoryCardModel.Category)?.name.orEmpty()
    val shape = RoundedCornerShape(14.dp)
    val border = BorderStroke(
        if (isCreate) 1.dp else 2.dp,
        if (isCreate) BorderColor.copy(alpha = 0.72f) else PrimaryBlue.copy(alpha = 0.74f)
    )
    val content: @Composable () -> Unit = {
        CategoryBentoCardContent(
            isCreate = isCreate,
            category = category,
            dishCountByCategory = dishCountByCategory
        )
    }

    if (isCreate) {
        Surface(
            onClick = onCreateCategoryClick,
            shape = shape,
            color = CardBackground,
            border = null,
            modifier = modifier
                .height(152.dp)
                .dashedCategoryBorder(BorderColor.copy(alpha = 0.72f)),
            content = content
        )
    } else {
        Surface(
            shape = shape,
            color = CardBackground,
            border = border,
            modifier = modifier.height(152.dp),
            content = content
        )
    }
}

private fun Modifier.dashedCategoryBorder(color: Color): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    val inset = strokeWidth / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
        )
    )
}

@Composable
private fun CategoryBentoCardContent(
    isCreate: Boolean,
    category: String,
    dishCountByCategory: Map<String, Int>
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (isCreate) CardBackground else categoryAccent(category).copy(alpha = 0.72f),
            border = if (isCreate) BorderStroke(2.dp, PrimaryBlue) else null,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isCreate) Icons.Outlined.Add else categoryIcon(category),
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(if (isCreate) 26.dp else 23.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isCreate) "新增分类" else category,
            color = PrimaryBlue,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!isCreate) {
            Spacer(modifier = Modifier.height(3.dp))
            Text("${dishCountByCategory[category] ?: 0}款菜品", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

private fun categoryIcon(category: String): ImageVector = when {
    category.contains("披萨") || category.contains("主食") -> Icons.Outlined.LocalPizza
    category.contains("蛋糕") || category.contains("甜") -> Icons.Outlined.Cake
    category.contains("饮") || category.contains("咖啡") || category.contains("茶") -> Icons.Outlined.LocalCafe
    else -> Icons.Outlined.Restaurant
}

private fun categoryAccent(category: String): Color = when {
    category.contains("披萨") || category.contains("主食") -> SuccessGreen
    category.contains("蛋糕") || category.contains("甜") -> Color(0xFFFFC7D6)
    category.contains("饮") || category.contains("咖啡") || category.contains("茶") -> SignatureOrange
    else -> SuccessGreen
}

@Composable
private fun DishManagementHeader(
    selectedFilter: MenuFilter,
    onFilterSelected: (MenuFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "菜品管理",
                color = TextPrimary,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            items(
                listOf(
                    MenuFilter.All to "全部",
                    MenuFilter.Available to "已上架",
                    MenuFilter.Unavailable to "已下架"
                ),
                key = { it.first.name }
            ) { (filter, title) ->
                FilterChip(
                    title = title,
                    selected = filter == selectedFilter,
                    accent = if (filter == MenuFilter.Unavailable) TextSecondary else PrimaryBlue,
                    onClick = { onFilterSelected(filter) }
                )
            }
        }
    }
}

@Composable
private fun FloatingAddDishButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFFFF9FB7),
        border = BorderStroke(1.dp, SignatureOrange.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color(0xFFFAFCFF), modifier = Modifier.size(20.dp))
            Text("新增菜品", color = Color(0xFFFAFCFF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterChip(title: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent.copy(alpha = 0.12f) else CardBackground,
        border = BorderStroke(1.dp, if (selected) accent else BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).background(accent, CircleShape))
            Text(
                text = title,
                color = if (selected) accent else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MenuContent(
    state: MenuManagementUiState,
    isCompact: Boolean,
    onToggleSelection: (String) -> Unit,
    onToggleAvailability: (MenuDishEntity) -> Unit,
    onEdit: (MenuDishEntity) -> Unit,
    onDelete: (MenuDishEntity) -> Unit,
    onShowAddSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        if (state.isLoadingCategory) {
            SkeletonDishList()
        } else if (state.visibleDishes.isEmpty()) {
            EmptyMenuCard(onShowAddSheet = onShowAddSheet)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.visibleDishes.forEach { dish ->
                    DishManageCard(
                        dish = dish,
                        selected = dish.id in state.selectedDishIds,
                        isBatchMode = state.isBatchMode,
                        isCompact = isCompact,
                        onToggleSelection = { onToggleSelection(dish.id) },
                        onToggleAvailability = { onToggleAvailability(dish) },
                        onEdit = { onEdit(dish) },
                        onDelete = { onDelete(dish) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonDishList() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(HoverBackground))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxWidth(0.42f).height(16.dp).background(HoverBackground, RoundedCornerShape(6.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.30f).height(18.dp).background(HoverBackground, RoundedCornerShape(6.dp)))
                        Box(modifier = Modifier.fillMaxWidth(0.52f).height(12.dp).background(HoverBackground, RoundedCornerShape(6.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMenuCard(onShowAddSheet: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        OrderGuidanceEmptyState(
            title = "当前分类暂无菜品",
            subtitle = "点击添加菜品，补充菜名、价格、分类和图片后即可上架。",
            actionText = "+ 添加菜品",
            onAction = onShowAddSheet
        )
    }
}

@Composable
private fun DishManageCard(
    dish: MenuDishEntity,
    selected: Boolean,
    isBatchMode: Boolean,
    isCompact: Boolean,
    onToggleSelection: () -> Unit,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val activeColor = if (dish.isAvailable) PrimaryBlue else TextSecondary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (dish.isAvailable) CardBackground else CardBackground.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, if (dish.isAvailable) PrimaryBlue.copy(alpha = 0.20f) else BorderColor.copy(alpha = 0.54f)),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (selected) PrimaryBlue.copy(alpha = 0.06f) else CardBackground)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isBatchMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
            }
            DishImage(dish = dish)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dish.name,
                    color = activeColor,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusTag(text = dish.category, color = activeColor)
                    StatusTag(text = if (dish.isAvailable) "在售" else "已下架", color = activeColor)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(yuanText(dish.price), color = activeColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    dish.originPrice?.takeIf { it > dish.price }?.let {
                        Text(
                            yuanText(it),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
            DishActionArea(
                checked = dish.isAvailable,
                onToggleAvailability = onToggleAvailability,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun DishImage(dish: MenuDishEntity) {
    if (dish.imageUrl.isNotBlank()) {
        AsyncImage(
            model = dish.imageUrl,
            contentDescription = dish.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun StatusTag(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AvailabilitySwitch(checked: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (checked) SuccessGreen else Color(0xFFE5E6EB),
        modifier = Modifier.size(width = 38.dp, height = 22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(modifier = Modifier.size(16.dp).background(Color(0xFFFAFCFF), CircleShape))
        }
    }
}

@Composable
private fun DishActionArea(
    checked: Boolean,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AvailabilitySwitch(checked = checked, onClick = onToggleAvailability)
        Surface(
            onClick = onEdit,
            shape = CircleShape,
            color = Color(0xFFF8F3ED),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = "编辑菜品", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            }
        }
        Surface(
            onClick = onDelete,
            shape = CircleShape,
            color = DangerRed.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.22f)),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, contentDescription = "删除菜品", tint = DangerRed, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun ShopSettingsDialog(
    shopName: String,
    shopImageUrl: String,
    announcement: String,
    onShopNameChange: (String) -> Unit,
    onAnnouncementChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = CardBackground,
        title = {
            Text(
                "编辑店铺资料",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (shopImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = shopImageUrl,
                            contentDescription = "店铺封面预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.shop_banner_stitch),
                            contentDescription = "店铺封面预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(999.dp),
                        color = CardBackground.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            Text("更换封面", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                OutlinedTextField(
                    value = shopName,
                    onValueChange = onShopNameChange,
                    label = { Text("店铺名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = announcement,
                    onValueChange = onAnnouncementChange,
                    label = { Text("店铺公告") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = editorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = shopName.trim().isNotBlank(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFFFAFCFF))
            ) {
                Text("保存资料")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@Composable
private fun NewCategoryDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground,
        title = { Text("新建分类", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("分类名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(categoryName) },
                enabled = categoryName.isNotBlank(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFFFAFCFF))
            ) {
                Text("保存分类")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@Composable
private fun CategoryManagerDialog(
    categories: List<String>,
    selectedCategory: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var newCategory by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editingName by remember { mutableStateOf("") }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground,
        title = { Text("分类管理", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text("新分类") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = editorTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            enabled = newCategory.isNotBlank(),
                            onClick = {
                                onCreate(newCategory)
                                newCategory = ""
                            }
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "新增分类", tint = PrimaryBlue)
                        }
                    }
                }
                items(categories, key = { it }) { category ->
                    val isEditing = editingCategory == category
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (category == selectedCategory) PrimaryBlue.copy(alpha = 0.10f) else Color(0xFFF7FBFF),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editingName,
                                    onValueChange = { editingName = it },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = editorTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    onRename(category, editingName)
                                    editingCategory = null
                                }) {
                                    Icon(Icons.Outlined.Check, contentDescription = "保存分类", tint = PrimaryBlue)
                                }
                            } else {
                                Text(
                                    text = category,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = if (category == selectedCategory) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    editingCategory = category
                                    editingName = category
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "修改分类", tint = TextSecondary)
                                }
                                IconButton(onClick = { onDelete(category) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "删除分类", tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成", color = PrimaryBlue) }
        }
    )
}

@Composable
private fun DeleteDishDialog(
    title: String,
    body: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardBackground,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color(0xFFFAFCFF)),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DishEditorDialog(
    state: DishEditorState,
    categories: List<String>,
    message: String?,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onOriginPriceChange: (String) -> Unit,
    onImageChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onAvailabilityChange: (Boolean) -> Unit,
    onSignatureChange: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onCreateCategoryAndSave: (String) -> Unit
) {
    var pendingCategoryCreation by remember { mutableStateOf<String?>(null) }
    val requestSave = {
        val normalizedCategory = state.category.trim()
        val existingCategory = categories.firstOrNull { it.trim().equals(normalizedCategory, ignoreCase = true) }
        when {
            normalizedCategory.isBlank() -> onSave()
            existingCategory != null -> {
                if (existingCategory != state.category) onCategoryChange(existingCategory)
                onSave()
            }
            else -> pendingCategoryCreation = normalizedCategory
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PageBackground,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = BorderColor,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 10.dp)
                    .size(width = 56.dp, height = 6.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 24.dp, bottom = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.id == null) "新增菜品" else "编辑菜品",
                        color = PrimaryBlue,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("给你们的小饭桌添一道新菜", color = TextPrimary, fontSize = 18.sp, lineHeight = 26.sp)
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = CardBackground,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = TextPrimary, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderColor.copy(alpha = 0.60f))
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(214.dp)
                            .clickable(onClick = onPickImage),
                        shape = RoundedCornerShape(20.dp),
                        color = SuccessGreen.copy(alpha = 0.46f),
                        border = BorderStroke(2.dp, PrimaryBlue.copy(alpha = 0.28f))
                    ) {
                        if (state.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = state.imageUrl,
                                contentDescription = "菜品图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Surface(shape = CircleShape, color = CardBackground, modifier = Modifier.size(72.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(34.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("选择或拍摄菜品图", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item {
                    LabeledEditorField(label = "名称") {
                        EditorTextField(value = state.name, onValueChange = onNameChange, label = "比如 爱心披萨")
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledEditorField(label = "售价 (¥)", modifier = Modifier.weight(1f)) {
                            EditorTextField(
                                value = state.price,
                                onValueChange = onPriceChange,
                                label = "比如 28.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        LabeledEditorField(label = "原价 (可选)", modifier = Modifier.weight(1f)) {
                            EditorTextField(
                                value = state.originPrice,
                                onValueChange = onOriginPriceChange,
                                label = "可不填",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledEditorField(label = "分类", modifier = Modifier.weight(1.35f)) {
                            EditorTextField(
                                value = state.category,
                                onValueChange = onCategoryChange,
                                label = "选择或新建分类"
                            )
                        }
                        LabeledEditorField(label = "库存", modifier = Modifier.weight(0.65f)) {
                            EditorTextField(
                                value = state.stock,
                                onValueChange = onStockChange,
                                label = "32",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
                item {
                    val categoryOptions = remember(categories) {
                        (listOf("主食", "小吃", "饮品") + categories).distinct()
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryOptions, key = { it }) { category ->
                            val selected = category == state.category
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) PrimaryBlue else CardBackground,
                                border = BorderStroke(1.dp, if (selected) PrimaryBlue else BorderColor),
                                onClick = { onCategoryChange(category) }
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = if (selected) Color(0xFFFAFCFF) else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                item {
                    LabeledEditorField(label = "描述") {
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = onDescriptionChange,
                            label = null,
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = editorTextFieldColors()
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("是否上架", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("立即在小店展示", color = TextSecondary, fontSize = 13.sp)
                            }
                            Switch(checked = state.isAvailable, onCheckedChange = onAvailabilityChange)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("是否招牌", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("带有专属标识", color = TextSecondary, fontSize = 13.sp)
                            }
                            Switch(checked = state.isSignature, onCheckedChange = onSignatureChange)
                        }
                    }
                }
                if (message != null) {
                    item { Text(message, color = PrimaryBlue, style = MaterialTheme.typography.bodySmall) }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground.copy(alpha = 0.96f))
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    color = CardBackground,
                    border = BorderStroke(2.dp, PrimaryBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("取消", color = PrimaryBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    onClick = requestSave,
                    shape = RoundedCornerShape(999.dp),
                    color = PrimaryBlue,
                    modifier = Modifier
                        .weight(1.55f)
                        .height(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color(0xFFFAFCFF), modifier = Modifier.size(24.dp))
                            Text("保存菜品", color = Color(0xFFFAFCFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    pendingCategoryCreation?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingCategoryCreation = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = CardBackground,
            title = {
                Text("创建新分类？", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "“$category”还不是现有分类。确认后会自动创建该分类，并继续保存菜品。",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingCategoryCreation = null
                        onCreateCategoryAndSave(category)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color(0xFFFAFCFF)
                    )
                ) {
                    Text("创建并保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCategoryCreation = null }) {
                    Text("返回修改", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LabeledEditorField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = editorTextFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun editorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = BorderColor,
    focusedContainerColor = CardBackground,
    unfocusedContainerColor = CardBackground
)
