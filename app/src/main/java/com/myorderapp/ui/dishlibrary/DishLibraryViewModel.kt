package com.myorderapp.ui.dishlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.myorderapp.data.repository.HybridDishRepository
import com.myorderapp.data.repository.RoomPagingDishRepository
import com.myorderapp.domain.model.Dish
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class DishLibraryUiState(
    val sourceFilter: String = "全部",
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class DishLibraryViewModel(
    private val dishRepo: HybridDishRepository,
    private val pagingRepo: RoomPagingDishRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DishLibraryUiState())
    val uiState: StateFlow<DishLibraryUiState> = _uiState.asStateFlow()

    val pagedDishes: Flow<PagingData<Dish>> = _uiState
        .flatMapLatest { state ->
            pagingRepo.getDishesPaged(
                query = state.searchQuery,
                source = sourceFilterToSource(state.sourceFilter)
            )
        }
        .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            dishRepo.syncFromCloud()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onSourceFilterChanged(source: String) {
        _uiState.value = _uiState.value.copy(sourceFilter = source)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun deleteDish(dishId: String) {
        viewModelScope.launch {
            dishRepo.deleteDish(dishId)
        }
    }

    private fun sourceFilterToSource(source: String): String? = when (source) {
        "我的菜单" -> "custom"
        "收藏" -> "external"
        else -> null
    }
}
