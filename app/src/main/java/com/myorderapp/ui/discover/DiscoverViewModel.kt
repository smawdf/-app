package com.myorderapp.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.remote.recipe.JisuRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JisuResult
import com.myorderapp.data.remote.recipe.JuheRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.JuheResult
import com.myorderapp.data.remote.recipe.TianRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.TianResult
import com.myorderapp.data.repository.MenuDishDraft
import com.myorderapp.data.repository.RoomMenuRepository
import com.myorderapp.data.repository.SINGLE_SHOP_ID
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.ui.search.ExternalDishImageSearchResult
import com.myorderapp.ui.search.ExternalDishImageSource
import com.myorderapp.ui.search.SearchableMenuItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<DiscoverDishSearchItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val addedMenuItemIds: Set<String> = emptySet(),
    val addedMenuItemNames: Set<String> = emptySet(),
    val pendingAddItem: DiscoverDishSearchItem? = null,
    val addPrice: String = "",
    val addCategory: String = "",
    val message: String? = null,
    val errorMessage: String? = null
)

data class DiscoverDishSearchItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val imageUrl: String?,
    val sourceLabel: String,
    val category: String = "",
    val price: Double = 12.0,
    val isAdded: Boolean = false
)

@OptIn(FlowPreview::class)
class DiscoverViewModel(
    private val dishRepository: DishRepository,
    private val menuRepository: MenuRepository,
    private val roomMenuRepository: RoomMenuRepository,
    private val externalDishImageSource: ExternalDishImageSource,
    private val tianRecipeRemoteDataSource: TianRecipeRemoteDataSource,
    private val juheRecipeRemoteDataSource: JuheRecipeRemoteDataSource,
    private val jisuRecipeRemoteDataSource: JisuRecipeRemoteDataSource,
    private val searchDebounceMs: Long = 300L,
    searchScope: CoroutineScope? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val queryChanges = MutableStateFlow("")
    private val scope = searchScope ?: viewModelScope

    init {
        scope.launch {
            menuRepository.getMenuCategories(SINGLE_SHOP_ID).collectLatest { categories ->
                val names = categories.map { it.name }.filter { it.isNotBlank() }
                _uiState.update { state ->
                    state.copy(
                        categories = names,
                        addCategory = state.addCategory.ifBlank { names.firstOrNull().orEmpty() }
                    )
                }
            }
        }
        scope.launch {
            queryChanges
                .debounce(searchDebounceMs)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val trimmedQuery = query.trim()
                    if (trimmedQuery.isBlank()) {
                        clearResults()
                    } else {
                        performSearch(trimmedQuery)
                    }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                errorMessage = null
            )
        }
        queryChanges.value = query
    }

    fun addToMenu(item: DiscoverDishSearchItem) {
        val normalizedName = item.name.trim().lowercase()
        if (
            _uiState.value.addedMenuItemIds.contains(item.id) ||
            _uiState.value.addedMenuItemNames.contains(normalizedName) ||
            item.isAdded
        ) {
            _uiState.update { it.copy(message = "已在我的小店：${item.name}", errorMessage = null) }
            return
        }

        _uiState.update { state ->
            state.copy(
                pendingAddItem = item,
                addPrice = "%.2f".format(item.price.takeIf { it > 0.0 } ?: 12.0),
                addCategory = item.category.ifBlank { state.addCategory.ifBlank { state.categories.firstOrNull().orEmpty() } },
                errorMessage = null
            )
        }
    }

    fun onAddPriceChanged(value: String) {
        _uiState.update { it.copy(addPrice = value.filter { char -> char.isDigit() || char == '.' }, errorMessage = null) }
    }

    fun onAddCategoryChanged(value: String) {
        _uiState.update { it.copy(addCategory = value, errorMessage = null) }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(pendingAddItem = null, addPrice = "", errorMessage = null) }
    }

    fun confirmAddToMenu() {
        val state = _uiState.value
        val item = state.pendingAddItem ?: return
        val price = state.addPrice.toDoubleOrNull()
        val category = state.addCategory.trim()

        if (price == null || price <= 0.0) {
            _uiState.update { it.copy(errorMessage = "请填写有效价格") }
            return
        }
        if (category.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请选择或填写分类") }
            return
        }

        scope.launch {
            val normalizedName = item.name.trim().lowercase()
            val existing = roomMenuRepository.observeMenuDishes()
                .first()
                .any { it.name.equals(item.name, ignoreCase = true) }

            if (!existing) {
                roomMenuRepository.saveDish(
                    MenuDishDraft(
                        name = item.name,
                        price = price,
                        imageUrl = item.imageUrl.orEmpty(),
                        category = category,
                        description = item.subtitle
                    )
                )
            } else {
                _uiState.update { it.copy(message = "已在我的小店：${item.name}", pendingAddItem = null, errorMessage = null) }
                return@launch
            }

            _uiState.update { state ->
                val addedIds = state.addedMenuItemIds + item.id
                val addedNames = state.addedMenuItemNames + normalizedName
                state.copy(
                    addedMenuItemIds = addedIds,
                    addedMenuItemNames = addedNames,
                    results = state.results.map { result ->
                        if (
                            result.id == item.id ||
                            result.name.trim().equals(item.name.trim(), ignoreCase = true)
                        ) {
                            result.copy(isAdded = true)
                        } else {
                            result
                        }
                    },
                    pendingAddItem = null,
                    addPrice = "",
                    addCategory = category,
                    message = "已加入我的小店：${item.name}",
                    errorMessage = null
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun clearResults() {
        _uiState.update {
            it.copy(
                isSearching = false,
                results = emptyList(),
                errorMessage = null
            )
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update {
            it.copy(
                isSearching = true,
                errorMessage = null
            )
        }

        var partialNetworkUnavailable = false

        try {
            val localDishes = runCatching {
                dishRepository.searchDishes(query).first()
            }.getOrElse {
                partialNetworkUnavailable = true
                emptyList()
            }

            val menuItems = runCatching {
                menuRepository.searchMenuItems(query).first()
            }.getOrElse {
                partialNetworkUnavailable = true
                emptyList()
            }

            val externalImages = runCatching {
                externalDishImageSource.search(query)
            }.getOrElse {
                partialNetworkUnavailable = true
                ExternalDishImageSearchResult()
            }

            val tianDishes = when (val result = tianRecipeRemoteDataSource.searchRecipes(query, num = 8)) {
                is TianResult.Success -> result.dishes
                is TianResult.NoKey -> emptyList()
                is TianResult.ApiError -> {
                    partialNetworkUnavailable = true
                    emptyList()
                }
                is TianResult.NetworkError -> {
                    partialNetworkUnavailable = true
                    emptyList()
                }
            }
            val juheDishes = when (val result = juheRecipeRemoteDataSource.searchRecipes(query, num = 8)) {
                is JuheResult.Success -> result.dishes
                is JuheResult.NoKey -> emptyList()
                is JuheResult.ApiError -> {
                    partialNetworkUnavailable = true
                    emptyList()
                }
                is JuheResult.NetworkError -> {
                    partialNetworkUnavailable = true
                    emptyList()
                }
            }
            val jisuDishes = when (val result = jisuRecipeRemoteDataSource.searchRecipes(query, num = 8)) {
                is JisuResult.Success -> result.dishes
                is JisuResult.NoKey -> emptyList()
                is JisuResult.ApiError -> {
                    partialNetworkUnavailable = true
                    emptyList()
                }
                is JisuResult.NetworkError -> {
                    partialNetworkUnavailable = true
                    emptyList()
                }
            }

            if (_uiState.value.query.trim() != query) return

            val addedMenuItemIds = _uiState.value.addedMenuItemIds
            val existingMenuItemNames = menuItems.map { it.menuItem.name.trim().lowercase() }.toSet()
            val addedMenuItemNames = _uiState.value.addedMenuItemNames + existingMenuItemNames
            val results = buildList {
                addAll(localDishes.map(::toLocalResult))
                addAll(menuItems.map(::toMenuResult))
                addAll(tianDishes.mapIndexed { index, dish -> toRecipeSourceResult(index, dish, "tian", "天行菜谱") })
                addAll(juheDishes.mapIndexed { index, dish -> toRecipeSourceResult(index, dish, "juhe", "聚合菜谱") })
                addAll(jisuDishes.mapIndexed { index, dish -> toRecipeSourceResult(index, dish, "jisu", "极速菜谱") })
                addAll((externalImages.primary.ifEmpty { externalImages.fallback }).map(::toExternalResult))
            }
                .distinctBy { "${it.sourceLabel}:${it.name}" }
                .map {
                    it.copy(
                        isAdded = addedMenuItemIds.contains(it.id) ||
                            addedMenuItemNames.contains(it.name.trim().lowercase())
                    )
                }
                .sortedByDescending { !it.imageUrl.isNullOrBlank() }
                .take(30)

            _uiState.update {
                it.copy(
                    isSearching = false,
                    results = results,
                    errorMessage = if (partialNetworkUnavailable) "部分网络结果暂不可用" else null
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (_uiState.value.query.trim() == query) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "部分网络结果暂不可用"
                    )
                }
            }
        }
    }

    private fun toLocalResult(dish: Dish): DiscoverDishSearchItem {
        val subtitle = listOfNotNull(
            dish.category.takeIf { it.isNotBlank() },
            dish.notes.takeIf { it.isNotBlank() },
            dish.ingredients.take(3).joinToString("、").takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        return DiscoverDishSearchItem(
            id = "local:${dish.id}",
            name = dish.name,
            subtitle = subtitle.ifBlank { "我的菜品库" },
            imageUrl = dish.imageUrl,
            sourceLabel = "local"
        )
    }

    private fun toMenuResult(result: SearchableMenuItem): DiscoverDishSearchItem {
        return DiscoverDishSearchItem(
            id = "menu:${result.shopId}:${result.menuItem.id}",
            name = result.menuItem.name,
            subtitle = "${result.shopName} · ${result.menuItem.subtitle.ifBlank { result.menuItem.description }}",
            imageUrl = result.menuItem.imageUrl,
            sourceLabel = "menu",
            isAdded = true
        )
    }

    private fun toRecipeSourceResult(
        index: Int,
        dish: Dish,
        sourceLabel: String,
        fallbackSubtitle: String
    ): DiscoverDishSearchItem {
        val subtitle = listOfNotNull(
            dish.category.takeIf { it.isNotBlank() },
            dish.ingredients.take(3).joinToString("、").takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        return DiscoverDishSearchItem(
            id = "$sourceLabel:${dish.externalId ?: dish.id.ifBlank { "${dish.name}:$index" }}",
            name = dish.name,
            subtitle = subtitle.ifBlank { fallbackSubtitle },
            imageUrl = dish.imageUrl,
            sourceLabel = sourceLabel
        )
    }

    private fun toExternalResult(result: com.myorderapp.ui.search.ExternalDishImageResult): DiscoverDishSearchItem {
        return DiscoverDishSearchItem(
            id = "external:${result.source}:${result.id}",
            name = result.name,
            subtitle = result.subtitle.ifBlank { result.description.ifBlank { result.category } },
            imageUrl = result.imageUrl,
            sourceLabel = "external"
        )
    }
}
