package com.myorderapp.ui.random

import android.content.Context
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
    val currentName: String = "",
    val isFromApi: Boolean = false,
    // 用户自定义筛选
    val categoryFilter: String = "",
    val maxTimeFilter: String = "",
    val difficultyFilter: Int = 0,  // 0=不限
    // 不重复
    val shownDishIds: Set<String> = emptySet(),
    val allShown: Boolean = false
)

class RandomViewModel(
    private val dishRepository: DishRepository,
    private val dualSearch: DualRecipeSearchUseCase,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(RandomUiState())
    val uiState: StateFlow<RandomUiState> = _uiState.asStateFlow()

    private val randomQueries = listOf(
        "鸡肉", "牛肉", "猪肉", "虾", "鱼", "豆腐", "鸡蛋", "土豆",
        "茄子", "排骨", "面条", "汤", "蛋糕", "沙拉", "炒菜", "红烧",
        "清蒸", "麻辣", "火锅", "凉拌"
    )

    private val prefs = appContext.getSharedPreferences("random_prefs", Context.MODE_PRIVATE)

    init {
        loadShownDishes()
        loadCandidates()
    }

    // ── 筛选条件 ──
    fun onCategoryFilterChanged(v: String) {
        if (v.length <= 10) _uiState.value = _uiState.value.copy(categoryFilter = v)
    }
    fun onMaxTimeChanged(v: String) {
        _uiState.value = _uiState.value.copy(maxTimeFilter = v.filter { it.isDigit() })
    }
    fun onDifficultyChanged(v: Int) {
        _uiState.value = _uiState.value.copy(difficultyFilter = v)
    }

    fun resetShown() {
        prefs.edit().remove("shown_ids").apply()
        _uiState.value = _uiState.value.copy(shownDishIds = emptySet(), allShown = false)
    }

    // ── 不重复机制 ──
    private fun loadShownDishes() {
        val ids = prefs.getString("shown_ids", "")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        _uiState.value = _uiState.value.copy(shownDishIds = ids)
    }

    private fun markShown(dishId: String) {
        val ids = (_uiState.value.shownDishIds + dishId).toMutableSet()
        prefs.edit().putString("shown_ids", ids.joinToString(",")).apply()
        _uiState.value = _uiState.value.copy(shownDishIds = ids)
    }

    private fun loadCandidates() {
        viewModelScope.launch {
            val all = dishRepository.getAllDishes().first()
            _uiState.value = _uiState.value.copy(candidates = all)
        }
    }

    fun cacheClickedDish(dishId: String) {
        val dish = _uiState.value.candidates.find { it.id == dishId }
            ?: _uiState.value.selectedDish?.takeIf { it.id == dishId }
            ?: return
        viewModelScope.launch { dishRepository.cacheSearchResult(dish) }
    }

    fun spin() {
        if (_uiState.value.isSpinning) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSpinning = true, isFromApi = false)

                // 1. 查本地/Supabase 数据库
                var allDishes = dishRepository.getAllDishes().first()
                val totalLocal = allDishes.size

                // 2. 库为空 (< 20) → 调 Juhe API 3 次补库
                if (totalLocal < 20) {
                    _uiState.value = _uiState.value.copy(isFromApi = true)
                    val queries = randomQueries.shuffled(Random).take(3)
                    for (q in queries) {
                        val result = dualSearch.search(q, numPerApi = 10)
                        result.dishes.forEach { dishRepository.cacheSearchResult(it) }
                    }
                    allDishes = dishRepository.getAllDishes().first()
                }

                if (allDishes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSpinning = false, currentName = "暂无菜品"
                    )
                    return@launch
                }

                // 3. 应用筛选条件
                val state = _uiState.value
                var filtered = allDishes.toList()

                // 分类筛选
                if (state.categoryFilter.isNotBlank()) {
                    filtered = filtered.filter {
                        it.category.contains(state.categoryFilter, ignoreCase = true) ||
                        it.name.contains(state.categoryFilter, ignoreCase = true)
                    }
                }
                // 时间筛选
                val maxTime = state.maxTimeFilter.toIntOrNull()
                if (maxTime != null && maxTime > 0) {
                    filtered = filtered.filter { it.cookTimeMin in 1..maxTime }
                }
                // 难度筛选
                if (state.difficultyFilter > 0) {
                    filtered = filtered.filter { it.difficulty == state.difficultyFilter }
                }
                // 排除已展示的（不重复）
                if (state.shownDishIds.isNotEmpty()) {
                    val remaining = filtered.filter { it.id !in state.shownDishIds }
                    if (remaining.isNotEmpty()) {
                        filtered = remaining
                    } else {
                        // 全部轮完，重置
                        _uiState.value = _uiState.value.copy(shownDishIds = emptySet(), allShown = true)
                        prefs.edit().remove("shown_ids").apply()
                    }
                }

                if (filtered.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSpinning = false,
                        currentName = "没有符合条件的菜品，请调整筛选"
                    )
                    return@launch
                }

                // 4. 随机抽取（取 8 个候选）
                val shuffled = filtered.shuffled(Random).take(8)
                _uiState.value = _uiState.value.copy(candidates = shuffled)

                // 5. 转盘动画
                val spins = 12 + Random.nextInt(8)
                for (i in 0 until spins) {
                    val name = shuffled[Random.nextInt(shuffled.size)].name
                    _uiState.value = _uiState.value.copy(currentName = name)
                    delay(50 + i * 15L)
                }

                // 6. 最终抽取
                val winner = shuffled.random()
                markShown(winner.id)
                _uiState.value = _uiState.value.copy(
                    selectedDish = winner,
                    isSpinning = false,
                    currentName = winner.name,
                    spinCount = _uiState.value.spinCount + 1
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSpinning = false, currentName = "出错了，请重试"
                )
            }
        }
    }
}
