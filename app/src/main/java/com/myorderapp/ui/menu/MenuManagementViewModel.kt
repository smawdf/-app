package com.myorderapp.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.data.repository.MenuDishDraft
import com.myorderapp.data.repository.RoomMenuRepository
import com.myorderapp.data.repository.SingleShopRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MenuFilter(val title: String) {
    All("全部"),
    Available("在售"),
    Unavailable("下架"),
    Signature("招牌")
}

enum class MenuSortMode(val title: String) {
    SalesDesc("销量从高到低"),
    PriceAsc("价格升序"),
    Newest("上新时间")
}

data class DishEditorState(
    val id: String? = null,
    val name: String = "",
    val price: String = "",
    val originPrice: String = "",
    val imageUrl: String = "",
    val category: String = "招牌必吃",
    val description: String = "",
    val stock: String = "32",
    val isAvailable: Boolean = true,
    val isSignature: Boolean = false
)

data class MenuManagementUiState(
    val shopName: String = "我的小店",
    val shopNameDraft: String = "我的小店",
    val shopImageUrl: String = "",
    val dishes: List<MenuDishEntity> = emptyList(),
    val visibleDishes: List<MenuDishEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "",
    val selectedFilter: MenuFilter = MenuFilter.All,
    val sortMode: MenuSortMode = MenuSortMode.Newest,
    val searchQuery: String = "",
    val isBatchMode: Boolean = false,
    val selectedDishIds: Set<String> = emptySet(),
    val isLoadingCategory: Boolean = false,
    val editor: DishEditorState = DishEditorState(),
    val isEditing: Boolean = false,
    val message: String? = null
) {
    val availableCount: Int = dishes.count { it.isAvailable }
    val unavailableCount: Int = dishes.count { !it.isAvailable }
}

class MenuManagementViewModel(
    private val menuRepository: RoomMenuRepository,
    private val singleShopRepository: SingleShopRepository
) : ViewModel() {

    private var categoryLoadingJob: Job? = null

    private val _uiState = MutableStateFlow(
        MenuManagementUiState(
            shopName = singleShopRepository.getShopName(),
            shopNameDraft = singleShopRepository.getShopName(),
            shopImageUrl = singleShopRepository.getShopImageUrl(),
            categories = singleShopRepository.getCategoryNames().normalizedMenuCategories()
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            singleShopRepository.ensureSeedMenu()
            menuRepository.observeMenuDishes().collect { dishes ->
                val categories = (singleShopRepository.getCategoryNames() + dishes.map { it.category }).normalizedMenuCategories()
                val selected = _uiState.value.selectedCategory.takeIf { it in categories } ?: categories.firstOrNull().orEmpty()
                updateState {
                    copy(
                        dishes = dishes,
                        categories = categories,
                        selectedCategory = selected
                    ).withVisibleDishes()
                }
            }
        }
    }

    fun onSearchQueryChange(value: String) {
        updateState { copy(searchQuery = value).withVisibleDishes() }
    }

    fun onShopNameChange(value: String) {
        updateState { copy(shopNameDraft = value, message = null) }
    }

    fun saveShopName() {
        val name = _uiState.value.shopNameDraft.trim().ifBlank { "我的小店" }
        singleShopRepository.updateShopName(name)
        updateState { copy(shopName = name, shopNameDraft = name, message = "店名已保存") }
    }

    fun resetShopNameDraft() {
        updateState { copy(shopNameDraft = shopName, message = null) }
    }

    fun updateShopImage(imageUrl: String) {
        singleShopRepository.updateShopImageUrl(imageUrl)
        updateState { copy(shopImageUrl = imageUrl, message = "店铺图片已更新") }
    }

    fun selectCategory(category: String) {
        if (category == _uiState.value.selectedCategory) return
        categoryLoadingJob?.cancel()
        updateState {
            copy(
                selectedCategory = category,
                selectedDishIds = emptySet(),
                isLoadingCategory = true
            ).withVisibleDishes()
        }
        categoryLoadingJob = viewModelScope.launch {
            delay(1500)
            updateState { copy(isLoadingCategory = false) }
        }
    }

    fun selectFilter(filter: MenuFilter) {
        updateState { copy(selectedFilter = filter, selectedDishIds = emptySet()).withVisibleDishes() }
    }

    fun toggleBatchMode() {
        updateState {
            copy(
                isBatchMode = !isBatchMode,
                selectedDishIds = emptySet()
            )
        }
    }

    fun toggleDishSelection(id: String) {
        updateState {
            val nextSelection = if (id in selectedDishIds) selectedDishIds - id else selectedDishIds + id
            copy(selectedDishIds = nextSelection)
        }
    }

    fun setSortMode(mode: MenuSortMode) {
        updateState { copy(sortMode = mode).withVisibleDishes() }
    }

    fun createCategory(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            updateState { copy(message = "请填写分类名称") }
            return
        }
        val nextCategories = (_uiState.value.categories + normalizedName).distinct()
        singleShopRepository.saveCategoryNames(nextCategories)
        updateState {
            copy(
                categories = nextCategories,
                selectedCategory = normalizedName,
                message = "分类已创建"
            ).withVisibleDishes()
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        val oldCategory = oldName.trim()
        val newCategory = newName.trim()
        if (oldCategory.isBlank() || newCategory.isBlank()) {
            updateState { copy(message = "请填写分类名称") }
            return
        }
        if (oldCategory == newCategory) return
        val nextCategories = _uiState.value.categories
            .map { if (it == oldCategory) newCategory else it }
            .distinct()
        singleShopRepository.saveCategoryNames(nextCategories)
        viewModelScope.launch {
            menuRepository.renameCategory(oldCategory, newCategory)
            updateState {
                copy(
                    categories = nextCategories,
                    selectedCategory = if (selectedCategory == oldCategory) newCategory else selectedCategory,
                    message = "分类已修改"
                ).withVisibleDishes()
            }
        }
    }

    fun deleteCategory(category: String) {
        val target = category.trim()
        if (target.isBlank()) return
        val nextCategories = _uiState.value.categories.filterNot { it == target }
        val fallback = nextCategories.firstOrNull() ?: "未分类"
        val savedCategories = nextCategories.ifEmpty { listOf(fallback) }
        singleShopRepository.saveCategoryNames(savedCategories)
        viewModelScope.launch {
            val ids = _uiState.value.dishes.filter { it.category == target }.map { it.id }
            menuRepository.moveToCategory(ids, fallback)
            updateState {
                copy(
                    categories = savedCategories,
                    selectedCategory = if (selectedCategory == target) fallback else selectedCategory,
                    message = "分类已删除"
                ).withVisibleDishes()
            }
        }
    }

    fun newDish() {
        val category = _uiState.value.selectedCategory
            .takeIf { it.isNotBlank() }
            ?: _uiState.value.categories.firstOrNull()
            ?: "招牌必吃"
        updateState {
            copy(
                editor = DishEditorState(category = category),
                isEditing = true,
                message = null
            )
        }
    }

    fun editDish(dish: MenuDishEntity) {
        updateState {
            copy(
                editor = DishEditorState(
                    id = dish.id,
                    name = dish.name,
                    price = dish.price.toString(),
                    originPrice = dish.originPrice?.toString().orEmpty(),
                    imageUrl = dish.imageUrl,
                    category = dish.category,
                    description = dish.description,
                    stock = dish.stock.toString(),
                    isAvailable = dish.isAvailable,
                    isSignature = dish.isSignature
                ),
                isEditing = true,
                message = null
            )
        }
    }

    fun closeEditor() {
        updateState { copy(isEditing = false, message = null) }
    }

    fun onNameChange(value: String) = updateEditor { copy(name = value) }

    fun onPriceChange(value: String) = updateEditor { copy(price = value.filterDecimal()) }

    fun onOriginPriceChange(value: String) = updateEditor { copy(originPrice = value.filterDecimal()) }

    fun onImageChange(value: String) = updateEditor { copy(imageUrl = value) }

    fun onCategoryChange(value: String) = updateEditor { copy(category = value) }

    fun onDescriptionChange(value: String) = updateEditor { copy(description = value) }

    fun onStockChange(value: String) = updateEditor { copy(stock = value.filter { it.isDigit() }) }

    fun onEditorAvailabilityChange(value: Boolean) = updateEditor { copy(isAvailable = value) }

    fun onEditorSignatureChange(value: Boolean) = updateEditor { copy(isSignature = value) }

    fun saveDish() {
        val editor = _uiState.value.editor
        val price = editor.price.toDoubleOrNull()
        if (editor.name.isBlank() || price == null || price <= 0.0) {
            updateState { copy(message = "请填写菜名和有效价格") }
            return
        }

        viewModelScope.launch {
            menuRepository.saveDish(
                MenuDishDraft(
                    id = editor.id,
                    name = editor.name,
                    price = price,
                    originPrice = editor.originPrice.toDoubleOrNull(),
                    imageUrl = editor.imageUrl,
                    category = editor.category,
                    description = editor.description,
                    stock = editor.stock.toIntOrNull() ?: 0,
                    isAvailable = editor.isAvailable,
                    isSignature = editor.isSignature
                )
            )
            updateState {
                copy(
                    isEditing = false,
                    editor = DishEditorState(category = editor.category),
                    message = "菜品已保存"
                ).withVisibleDishes()
            }
        }
    }

    fun deleteDish(id: String) {
        viewModelScope.launch {
            menuRepository.deleteDish(id)
            updateState {
                copy(
                    dishes = dishes.filterNot { it.id == id },
                    message = "菜品已删除",
                    selectedDishIds = selectedDishIds - id
                ).withVisibleDishes()
            }
        }
    }

    fun toggleDishAvailability(dish: MenuDishEntity) {
        viewModelScope.launch {
            menuRepository.setAvailability(dish.id, !dish.isAvailable)
            updateState { copy(message = if (dish.isAvailable) "已下架" else "已上架") }
        }
    }

    fun batchSetAvailability(isAvailable: Boolean) {
        val ids = _uiState.value.selectedDishIds.toList()
        viewModelScope.launch {
            menuRepository.setAvailability(ids, isAvailable)
            updateState {
                copy(
                    selectedDishIds = emptySet(),
                    message = if (isAvailable) "已批量上架" else "已批量下架"
                ).withVisibleDishes()
            }
        }
    }

    fun batchMoveToCategory(category: String) {
        val ids = _uiState.value.selectedDishIds.toList()
        viewModelScope.launch {
            menuRepository.moveToCategory(ids, category)
            updateState {
                copy(selectedDishIds = emptySet(), selectedCategory = category, message = "已移动分类").withVisibleDishes()
            }
        }
    }

    fun batchDeleteSelected() {
        val ids = _uiState.value.selectedDishIds.toList()
        viewModelScope.launch {
            menuRepository.deleteDishes(ids)
            updateState { copy(selectedDishIds = emptySet(), message = "已批量删除").withVisibleDishes() }
        }
    }

    private fun updateEditor(block: DishEditorState.() -> DishEditorState) {
        updateState { copy(editor = editor.block(), message = null) }
    }

    private fun updateState(block: MenuManagementUiState.() -> MenuManagementUiState) {
        _uiState.value = _uiState.value.block()
    }

    private fun MenuManagementUiState.withVisibleDishes(): MenuManagementUiState {
        val query = searchQuery.trim()
        val filtered = dishes
            .asSequence()
            .filter { selectedCategory.isBlank() || it.category == selectedCategory }
            .filter {
                when (selectedFilter) {
                    MenuFilter.All -> true
                    MenuFilter.Available -> it.isAvailable
                    MenuFilter.Unavailable -> !it.isAvailable
                    MenuFilter.Signature -> it.isSignature
                }
            }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .let { sequence ->
                when (sortMode) {
                    MenuSortMode.SalesDesc -> sequence.sortedByDescending { it.monthlySales }
                    MenuSortMode.PriceAsc -> sequence.sortedBy { it.price }
                    MenuSortMode.Newest -> sequence.sortedByDescending { it.updatedAt }
                }
            }
            .toList()
        return copy(visibleDishes = filtered)
    }

    private fun List<String>.normalizedMenuCategories(): List<String> {
        return this
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun String.filterDecimal(): String = filter { it.isDigit() || it == '.' }
}
