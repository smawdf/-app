package com.myorderapp.ui.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.usecase.DualRecipeSearchUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

data class RandomUiState(
    val candidates: List<Dish> = emptyList(),
    val selectedDish: Dish? = null,
    val isSpinning: Boolean = false,
    val spinCount: Int = 0,
    val selectedCategory: String = "全部",
    val currentName: String = "",
    val isFromApi: Boolean = false
)

class RandomViewModel(
    private val dishRepository: DishRepository,
    private val dualSearch: DualRecipeSearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RandomUiState())
    val uiState: StateFlow<RandomUiState> = _uiState.asStateFlow()

    // 随机搜索关键词
    private val randomQueries = listOf(
        "鸡肉", "牛肉", "猪肉", "虾", "鱼", "豆腐", "鸡蛋", "土豆",
        "茄子", "排骨", "面条", "汤", "蛋糕", "沙拉", "pasta", "chicken",
        "beef", "soup", "salad", "dessert"
    )

    init {
        loadCandidates()
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadCandidates()
    }

    private fun loadCandidates() {
        viewModelScope.launch {
            val allDishes = dishRepository.getAllDishes().first()
            val filtered = if (_uiState.value.selectedCategory == "全部") allDishes
                else allDishes.filter { it.category == _uiState.value.selectedCategory }
            // 随机取8个候选
            val shuffled = filtered.shuffled(Random).take(8)
            _uiState.value = _uiState.value.copy(candidates = shuffled.ifEmpty { allDishes.take(8) })
        }
    }

    fun cacheClickedDish(dishId: String) {
        val dish = _uiState.value.candidates.find { it.id == dishId }
            ?: _uiState.value.selectedDish?.takeIf { it.id == dishId }
            ?: return
        viewModelScope.launch {
            dishRepository.cacheSearchResult(dish)
        }
    }

    fun spin() {
        if (_uiState.value.isSpinning) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpinning = true, isFromApi = false)

            // 混合本地+API：50%概率从API随机抽取
            val useApi = Random.nextFloat() < 0.5f
            var candidates = _uiState.value.candidates.toList()

            if (useApi) {
                val query = randomQueries.random()
                val result = dualSearch.search(query, numPerApi = 5)
                if (result.dishes.isNotEmpty()) {
                    candidates = result.dishes.shuffled(Random).take(8)
                    // 缓存API结果
                    result.dishes.forEach { dishRepository.cacheSearchResult(it) }
                    _uiState.value = _uiState.value.copy(isFromApi = true)
                }
            }

            if (candidates.isEmpty()) {
                candidates = dishRepository.getAllDishes().first().shuffled(Random).take(8)
            }

            _uiState.value = _uiState.value.copy(candidates = candidates)

            // 旋转动画：快速切换候选名称
            val spins = 15 + Random.nextInt(10)
            for (i in 0 until spins) {
                val name = candidates[Random.nextInt(candidates.size)].name
                _uiState.value = _uiState.value.copy(currentName = name)
                delay(50 + i * 15L)
            }

            // 最终结果
            val winner = candidates.random()
            _uiState.value = _uiState.value.copy(
                selectedDish = winner,
                isSpinning = false,
                currentName = winner.name,
                spinCount = _uiState.value.spinCount + 1
            )
        }
    }
}
