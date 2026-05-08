package com.myorderapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.usecase.DualRecipeSearchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedSource: String = "全部",
    val results: List<Dish> = emptyList(),
    val availableSources: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val sources: List<String> = emptyList(),
    val errorMessage: String? = null
)

class SearchViewModel(
    private val dishRepository: DishRepository,
    private val dualSearch: DualRecipeSearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query, errorMessage = null, sources = emptyList())
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            performUnifiedSearch()
        }
    }

    private var allResults = mutableListOf<Dish>()

    fun onSourceSelected(source: String) {
        _uiState.value = _uiState.value.copy(
            selectedSource = source,
            results = applySourceFilter(allResults, source)
        )
    }

    fun cacheClickedDish(dishId: String) {
        val dish = _uiState.value.results.find { it.id == dishId } ?: return
        viewModelScope.launch {
            dishRepository.cacheSearchResult(dish)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private suspend fun performUnifiedSearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(isSearching = true)

        // 本地搜索 + 双 API 在线搜索 同时启动
        val (localResults, onlineResult) = coroutineScope {
            val localDeferred = async { dishRepository.searchDishes(query).first() }
            val onlineDeferred = async { dualSearch.search(query) }
            localDeferred.await() to onlineDeferred.await()
        }

        // 缓存所有在线搜索结果到本地仓库（下次搜同样的词就不需要调API了）
        onlineResult.dishes.forEach { dishRepository.cacheSearchResult(it) }

        // 合并结果：本地优先，在线补充（去重）
        val mergedResults = mergeResults(localResults, onlineResult.dishes)
        allResults.clear()
        allResults.addAll(mergedResults)
        val filtered = applySourceFilter(mergedResults, _uiState.value.selectedSource)

        val sourceLabels = mergedResults.map { resolveSourceLabel(it) }.distinct()
        val tabs = listOf("全部") + sourceLabels

        val sources = mutableListOf<String>()
        if (localResults.isNotEmpty()) sources.add("内置")
        sources.addAll(onlineResult.sources)

        _uiState.value = _uiState.value.copy(
            results = filtered,
            availableSources = tabs,
            isSearching = false,
            sources = sources
        )

        if (onlineResult.errors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = onlineResult.errors.joinToString("\n")
            )
        }
    }

    private fun mergeResults(local: List<Dish>, online: List<Dish>): List<Dish> {
        val result = mutableListOf<Dish>()
        val seen = mutableSetOf<String>()

        for (dish in local) {
            val key = dish.name.lowercase().trim()
            if (seen.add(key)) result.add(dish)
        }
        for (dish in online) {
            val key = dish.name.lowercase().trim()
            if (seen.add(key) && !result.any { it.name.equals(dish.name, ignoreCase = true) }) {
                result.add(dish)
            }
        }
        return result
    }

    private fun applySourceFilter(dishes: List<Dish>, source: String): List<Dish> {
        return if (source == "全部") dishes
        else dishes.filter { resolveSourceLabel(it) == source }
    }

    private fun resolveSourceLabel(dish: Dish): String {
        return when (dish.externalSource) {
            "juhe" -> "聚合数据"
            "tianapi" -> "天行数据"
            "jisuapi" -> "极速数据"
            else -> if (dish.source == "custom") "我的菜单" else "本地"
        }
    }
}
