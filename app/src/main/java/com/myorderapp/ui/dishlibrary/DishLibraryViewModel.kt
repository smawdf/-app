package com.myorderapp.ui.dishlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DishLibraryUiState(
    val dishes: List<Dish> = emptyList(),
    val sourceFilter: String = "全部",
    val categoryFilter: String = "全部",
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class DishLibraryViewModel(
    private val dishRepository: DishRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DishLibraryUiState())
    val uiState: StateFlow<DishLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dishRepository.getAllDishes().collect { dishes ->
                applyFilters(dishes)
            }
        }
    }

    fun onSourceFilterChanged(source: String) {
        _uiState.value = _uiState.value.copy(sourceFilter = source)
        refresh()
    }

    fun onCategoryFilterChanged(category: String) {
        _uiState.value = _uiState.value.copy(categoryFilter = category)
        refresh()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            dishRepository.getAllDishes().collect { applyFilters(it) }
        }
    }

    fun deleteDish(dishId: String) {
        viewModelScope.launch {
            dishRepository.deleteDish(dishId)
        }
    }

    private fun applyFilters(allDishes: List<Dish>) {
        val state = _uiState.value
        var filtered = allDishes

        when (state.sourceFilter) {
            "自建" -> filtered = filtered.filter { it.source == "custom" }
            "收藏" -> filtered = filtered.filter { it.source == "external" }
        }
        if (state.categoryFilter != "全部") {
            filtered = filtered.filter { it.category == state.categoryFilter }
        }
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
        }

        _uiState.value = state.copy(dishes = filtered, isLoading = false)
    }
}
