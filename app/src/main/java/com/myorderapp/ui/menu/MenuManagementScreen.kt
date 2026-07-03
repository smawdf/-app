package com.myorderapp.ui.menu

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.ui.components.OrderGuidanceEmptyState
import com.myorderapp.ui.components.OrderSearchField
import com.myorderapp.ui.util.yuanText
import org.koin.androidx.compose.koinViewModel

private val PrimaryBlue = Color(0xFF5F95B5)
private val SuccessGreen = Color(0xFF36CFC9)
private val DangerRed = Color(0xFFF53F3F)
private val SignatureOrange = Color(0xFFFF7D00)
private val TextPrimary = Color(0xFF1F2933)
private val TextSecondary = Color(0xFF6B7885)
private val PageBackground = Color(0xFFF7F9FB)
private val CardBackground = Color(0xFFFBFCFD)
private val HoverBackground = Color(0xFFEFF4F8)
private val BorderColor = Color(0xFFE7EEF3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    viewModel: MenuManagementViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isCompact = LocalConfiguration.current.screenWidthDp < 720
    var categoryRailCollapsed by remember(isCompact) { mutableStateOf(isCompact) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showShopNameDialog by remember { mutableStateOf(false) }
    var categoryEditTarget by remember { mutableStateOf<String?>(null) }
    var categoryNameDraft by remember { mutableStateOf("") }
    var categoryDeleteTarget by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<MenuDishEntity?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // 有些相册不会授予持久权限，仍保留当前 URI 供本次使用。
            }
            viewModel.onImageChange(uri.toString())
        }
    }
    val shopImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // 有些相册不会授予持久权限，仍保留当前 URI 供本次使用。
            }
            viewModel.updateShopImage(uri.toString())
        }
    }

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
            onPickImage = { imagePicker.launch(arrayOf("image/*")) },
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::saveDish
        )
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            AddRecipeSheet(
                onManualAdd = {
                    showAddSheet = false
                    viewModel.newDish()
                },
                onClose = { showAddSheet = false }
            )
        }
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

    categoryEditTarget?.let { category ->
        CategoryRenameDialog(
            value = categoryNameDraft,
            onValueChange = { categoryNameDraft = it },
            onDismiss = {
                categoryEditTarget = null
                categoryNameDraft = ""
            },
            onSave = {
                viewModel.renameCategory(category, categoryNameDraft)
                categoryEditTarget = null
                categoryNameDraft = ""
            }
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

    if (showShopNameDialog) {
        ShopNameDialog(
            value = uiState.shopNameDraft,
            onValueChange = viewModel::onShopNameChange,
            onDismiss = {
                viewModel.resetShopNameDraft()
                showShopNameDialog = false
            },
            onSave = {
                viewModel.saveShopName()
                showShopNameDialog = false
            }
        )
    }

    deleteTarget?.let { dish ->
        DeleteDishDialog(
            title = "确认删除该菜品？",
            body = "删除后菜品图片、价格、订单历史关联数据无法恢复",
            confirmText = "确认删除",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.deleteDish(dish.id)
                deleteTarget = null
            }
        )
    }

    if (showBatchDeleteDialog) {
        DeleteDishDialog(
            title = "确认批量删除菜品？",
            body = "已选中的菜品将被删除，图片、价格和订单历史关联数据无法恢复",
            confirmText = "批量删除",
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = {
                viewModel.batchDeleteSelected()
                showBatchDeleteDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            MenuTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange
            )
            ShopSettingsStrip(
                shopName = uiState.shopName,
                shopImageUrl = uiState.shopImageUrl,
                totalCount = uiState.dishes.size,
                availableCount = uiState.availableCount,
                onEditShopName = { showShopNameDialog = true },
                onEditShopImage = { shopImagePicker.launch(arrayOf("image/*")) }
            )
            BulkToolbar(
                totalCount = uiState.dishes.size,
                availableCount = uiState.availableCount,
                unavailableCount = uiState.unavailableCount,
                selectedCount = uiState.selectedDishIds.size,
                selectedFilter = uiState.selectedFilter,
                isBatchMode = uiState.isBatchMode,
                sortMode = uiState.sortMode,
                categories = uiState.categories,
                onFilterSelected = viewModel::selectFilter,
                onToggleBatch = viewModel::toggleBatchMode,
                onSelectAllVisible = viewModel::selectAllVisibleDishes,
                onBatchAvailable = { viewModel.batchSetAvailability(true) },
                onBatchUnavailable = { viewModel.batchSetAvailability(false) },
                onBatchDelete = { showBatchDeleteDialog = true },
                onBatchMove = viewModel::batchMoveToCategory,
                onSortSelected = viewModel::setSortMode,
                onAddDish = { showAddSheet = true }
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 16.dp)
            ) {
                CategoryRail(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    isCompact = isCompact,
                    collapsed = categoryRailCollapsed,
                    onToggleCollapsed = { categoryRailCollapsed = !categoryRailCollapsed },
                    onCategoryClick = viewModel::selectCategory,
                    onCategoryRenameClick = { category ->
                        categoryEditTarget = category
                        categoryNameDraft = category
                    },
                    onCategoryDeleteClick = { categoryDeleteTarget = it },
                    onCreateCategoryClick = { showCategoryDialog = true }
                )
                MenuContent(
                    state = uiState,
                    isCompact = isCompact,
                    onToggleSelection = viewModel::toggleDishSelection,
                    onToggleAvailability = viewModel::toggleDishAvailability,
                    onEdit = viewModel::editDish,
                    onDelete = { deleteTarget = it },
                    onShowAddSheet = { showAddSheet = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ShopSettingsStrip(
    shopName: String,
    shopImageUrl: String,
    totalCount: Int,
    availableCount: Int,
    onEditShopName: () -> Unit,
    onEditShopImage: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PrimaryBlue.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .size(58.dp)
                    .clickable(onClick = onEditShopImage)
            ) {
                if (shopImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = shopImageUrl,
                        contentDescription = "店铺图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        Text("图片", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("店铺资料", color = TextSecondary, fontSize = 12.sp)
                Text(shopName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("共 $totalCount 个菜品，在售 $availableCount 个", color = TextSecondary, fontSize = 12.sp)
                Text("点击左侧图片可添加或更换店铺封面", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onEditShopName) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("修改店名", color = PrimaryBlue, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun MenuTopBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "我的店铺",
            color = TextPrimary,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.3f)
        )
        OrderSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "搜索菜品名称",
            modifier = Modifier
                .weight(0.7f)
                .height(52.dp)
        )
    }
}

@Composable
private fun BulkToolbar(
    totalCount: Int,
    availableCount: Int,
    unavailableCount: Int,
    selectedCount: Int,
    selectedFilter: MenuFilter,
    isBatchMode: Boolean,
    sortMode: MenuSortMode,
    categories: List<String>,
    onFilterSelected: (MenuFilter) -> Unit,
    onToggleBatch: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onBatchAvailable: () -> Unit,
    onBatchUnavailable: () -> Unit,
    onBatchDelete: () -> Unit,
    onBatchMove: (String) -> Unit,
    onSortSelected: (MenuSortMode) -> Unit,
    onAddDish: () -> Unit
) {
    var batchMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "共${totalCount}个菜品｜在售${availableCount}｜下架${unavailableCount}",
            color = TextPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(MenuFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    title = filter.title,
                    selected = filter == selectedFilter,
                    accent = when (filter) {
                        MenuFilter.All -> PrimaryBlue
                        MenuFilter.Available -> SuccessGreen
                        MenuFilter.Unavailable -> TextSecondary
                        MenuFilter.Signature -> SignatureOrange
                    },
                    onClick = { onFilterSelected(filter) }
                )
            }
        }
        Box {
            SecondaryButton(
                text = if (isBatchMode) "取消批量" else "批量操作",
                icon = Icons.Outlined.Tune,
                onClick = {
                    val shouldOpenMenu = !isBatchMode
                    onToggleBatch()
                    batchMenuExpanded = shouldOpenMenu
                },
                showArrow = true
            )
            DropdownMenu(expanded = batchMenuExpanded, onDismissRequest = { batchMenuExpanded = false }) {
                DropdownMenuItem(text = { Text("批量上架") }, onClick = { batchMenuExpanded = false; onBatchAvailable() }, enabled = selectedCount > 0)
                DropdownMenuItem(text = { Text("批量下架") }, onClick = { batchMenuExpanded = false; onBatchUnavailable() }, enabled = selectedCount > 0)
                DropdownMenuItem(text = { Text("批量删除") }, onClick = { batchMenuExpanded = false; onBatchDelete() }, enabled = selectedCount > 0)
                DropdownMenuItem(text = { Text("选择当前列表全部") }, onClick = { batchMenuExpanded = false; onSelectAllVisible() })
                DropdownMenuItem(text = { Text("批量移动分类") }, onClick = { categoryMenuExpanded = true }, enabled = selectedCount > 0)
            }
            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            categoryMenuExpanded = false
                            batchMenuExpanded = false
                            onBatchMove(category)
                        }
                    )
                }
            }
        }
        Box {
            IconOnlyButton(
                icon = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = "排序",
                onClick = { sortMenuExpanded = true }
            )
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                MenuSortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.title) },
                        onClick = {
                            sortMenuExpanded = false
                            onSortSelected(mode)
                        },
                        leadingIcon = if (mode == sortMode) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, tint = PrimaryBlue) }
                        } else null
                    )
                }
            }
        }
        Button(
            onClick = onAddDish,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFFFAFCFF)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("添加菜品", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
private fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit, showArrow: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            Text(text, color = TextPrimary, fontSize = 14.sp)
            if (showArrow) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun IconOnlyButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<String>,
    selectedCategory: String,
    isCompact: Boolean,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onCategoryRenameClick: (String) -> Unit,
    onCategoryDeleteClick: (String) -> Unit,
    onCreateCategoryClick: () -> Unit
) {
    val railWidth = when {
        !isCompact -> 200.dp
        collapsed -> 56.dp
        else -> 128.dp
    }
    var activeCategoryAction by remember(categories) { mutableStateOf<String?>(null) }
    Surface(
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CategoryRailTitle("分类")
            if (isCompact) {
                Surface(
                    onClick = {
                        activeCategoryAction = null
                        onToggleCollapsed()
                    },
                    color = Color(0xFFF7FBFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (collapsed) "分类" else "收起",
                            color = PrimaryBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(categories, key = { it }) { category ->
                    if (collapsed) {
                        CollapsedCategoryItem(
                            title = category,
                            selected = category == selectedCategory,
                            onClick = {
                                activeCategoryAction = null
                                onCategoryClick(category)
                            }
                        )
                    } else {
                        CategoryItem(
                            title = category,
                            selected = category == selectedCategory,
                            actionsVisible = activeCategoryAction == category,
                            onClick = {
                                activeCategoryAction = null
                                onCategoryClick(category)
                            },
                            onCategoryLongClick = { activeCategoryAction = category },
                            onRenameClick = {
                                activeCategoryAction = null
                                onCategoryRenameClick(category)
                            },
                            onDeleteClick = {
                                activeCategoryAction = null
                                onCategoryDeleteClick(category)
                            }
                        )
                    }
                }
            }
            Surface(
                onClick = {
                    activeCategoryAction = null
                    onCreateCategoryClick()
                },
                color = Color(0xFFF7FBFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Text("新建分类", color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CategoryRailTitle(title: String) {
    // Static title is intentionally hidden; the compact toggle is the visible "分类" control.
}

@Composable
private fun CollapsedCategoryItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (selected) PrimaryBlue.copy(alpha = 0.10f) else CardBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.trim().take(2).ifBlank { "未分" },
            color = if (selected) PrimaryBlue else TextPrimary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryItem(
    title: String,
    selected: Boolean,
    actionsVisible: Boolean,
    onClick: () -> Unit,
    onCategoryLongClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (selected) PrimaryBlue.copy(alpha = 0.10f) else CardBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onCategoryLongClick
            )
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (selected) PrimaryBlue else Color.Transparent)
        )
        Text(
            text = title,
            color = if (selected) PrimaryBlue else TextPrimary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 4.dp)
        )
        if (actionsVisible) {
            IconButton(onClick = onRenameClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "修改分类", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "删除分类", tint = DangerRed, modifier = Modifier.size(16.dp))
            }
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
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        if (state.isLoadingCategory) {
            SkeletonDishList()
        } else if (state.visibleDishes.isEmpty()) {
            EmptyMenuCard(onShowAddSheet = onShowAddSheet)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.visibleDishes, key = { it.id }) { dish ->
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(4) {
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isCompact) 132.dp else 116.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (selected) PrimaryBlue.copy(alpha = 0.06f) else CardBackground)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isBatchMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
            }
            DishImage(dish = dish)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "菜名：${dish.name}",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusTag(text = if (dish.isAvailable) "在售" else "下架", color = if (dish.isAvailable) SuccessGreen else TextSecondary)
                    if (dish.isSignature) StatusTag(text = "招牌", color = SignatureOrange)
                    AvailabilitySwitch(checked = dish.isAvailable, onClick = onToggleAvailability)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(yuanText(dish.price), color = PrimaryBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    dish.originPrice?.takeIf { it > dish.price }?.let {
                        Text(
                            yuanText(it),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Text(
                    text = "库存：${dish.stock}份 ｜月销：${dish.monthlySales}单",
                    color = if (dish.stock <= 0) DangerRed else TextSecondary,
                    fontSize = 12.sp
                )
                if (isCompact) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        DishActionArea(onEdit = onEdit, onDelete = onDelete)
                    }
                }
            }
            if (!isCompact) {
                DishActionArea(onEdit = onEdit, onDelete = onDelete)
            }
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
                .size(68.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(68.dp)
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
private fun DishActionArea(onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconLabelButton(icon = Icons.Outlined.Edit, text = "编辑", tint = TextSecondary, onClick = onEdit)
        IconLabelButton(icon = Icons.Outlined.Delete, text = "删除", tint = DangerRed, onClick = onDelete)
    }
}

@Composable
private fun IconLabelButton(icon: ImageVector, text: String, tint: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, tint.copy(alpha = 0.18f)),
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, color = tint, fontSize = 11.sp)
    }
}

@Composable
private fun ShopNameDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = CardBackground,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("修改店名", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("用于点菜页、订单和购物车显示", color = TextSecondary, fontSize = 12.sp)
            }
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("店铺名称") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = editorTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFFFAFCFF))
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

@Composable
private fun AddRecipeSheet(onManualAdd: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clickable(onClick = onManualAdd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = PrimaryBlue.copy(alpha = 0.12f), modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(modifier = Modifier.width(22.dp))
            Column {
                Text("手动添加", color = TextPrimary, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("详细记录菜名、价格和图片", color = TextSecondary, fontSize = 16.sp, lineHeight = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = TextSecondary)
        }
    }
}

@Composable
private fun NewCategoryDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                shape = RoundedCornerShape(10.dp),
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
private fun CategoryRenameDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = CardBackground,
        title = { Text("修改分类", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("分类名称") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = editorTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFFFAFCFF))
            ) {
                Text("保存")
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
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = CardBackground,
        title = { Text("分类管理", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
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
                        IconOnlyButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = "新增分类",
                            onClick = {
                                onCreate(newCategory)
                                newCategory = ""
                            }
                        )
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
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color(0xFFFAFCFF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
}

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
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = PrimaryBlue.copy(alpha = 0.10f), modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(if (state.id == null) "添加菜品" else "编辑菜品", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("维护名称、价格、分类和售卖状态", color = TextSecondary, fontSize = 12.sp)
                }
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(158.dp)
                            .clickable(onClick = onPickImage),
                        shape = RoundedCornerShape(14.dp),
                        color = PrimaryBlue.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, BorderColor)
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
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("选择菜品图片", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                                Text("建议使用正方形或横图", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { Text("基础信息", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                item {
                    EditorTextField(value = state.name, onValueChange = onNameChange, label = "菜名")
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        EditorTextField(
                            value = state.price,
                            onValueChange = onPriceChange,
                            label = "现价",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        EditorTextField(
                            value = state.originPrice,
                            onValueChange = onOriginPriceChange,
                            label = "原价",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    EditorTextField(
                        value = state.stock,
                        onValueChange = onStockChange,
                        label = "库存",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("分类", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it }) { category ->
                            val selected = category == state.category
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { onCategoryChange(category) }
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = if (selected) Color(0xFFFAFCFF) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.category,
                        onValueChange = onCategoryChange,
                        label = { Text("自定义分类") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = editorTextFieldColors()
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF7FBFF),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("售卖状态", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("在售", color = TextPrimary)
                                Switch(checked = state.isAvailable, onCheckedChange = onAvailabilityChange)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("招牌", color = TextPrimary)
                                Switch(checked = state.isSignature, onCheckedChange = onSignatureChange)
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        label = { Text("描述") },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = editorTextFieldColors()
                    )
                }
                if (message != null) {
                    item { Text(message, color = PrimaryBlue, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color(0xFFFAFCFF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        }
    )
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
