package com.myorderapp.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myorderapp.data.local.BimissingRecipeAssetSource
import com.myorderapp.data.remote.recipe.TianRecipeRemoteDataSource
import com.myorderapp.data.remote.recipe.TianResult
import com.myorderapp.data.remote.recipe.XiachufangRecipeSearchSource
import com.myorderapp.data.repository.MenuDishDraft
import com.myorderapp.data.repository.RoomMenuRepository
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.ui.search.ExternalDishImageSource
import com.myorderapp.ui.search.SearchableMenuItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DISCOVER_SINGLE_SHOP_ID = "single_shop"
private const val DISCOVER_RECOMMENDATION_FIRST_FRAME_DELAY_MS = 280L

data class DiscoverUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<DiscoverDishSearchItem> = emptyList(),
    val recommendations: List<DiscoverRecommendationItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val addedMenuItemIds: Set<String> = emptySet(),
    val addedMenuItemNames: Set<String> = emptySet(),
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

data class DiscoverRecommendationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val item: DiscoverDishSearchItem
)

@OptIn(FlowPreview::class)
class DiscoverViewModel(
    private val dishRepository: DishRepository,
    private val menuRepository: MenuRepository,
    private val roomMenuRepository: RoomMenuRepository,
    private val bimissingRecipeAssetSource: BimissingRecipeAssetSource,
    private val externalDishImageSource: ExternalDishImageSource,
    private val xiachufangRecipeSearchSource: XiachufangRecipeSearchSource,
    private val tianRecipeRemoteDataSource: TianRecipeRemoteDataSource,
    private val searchDebounceMs: Long = 300L,
    searchScope: CoroutineScope? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val queryChanges = MutableStateFlow("")
    private val scope = searchScope ?: viewModelScope
    private val searchCache = DiscoverSearchMemoryCache()
    private val addToMenuMutex = Mutex()
    private val imageRequestMutex = Mutex()
    private val imageRequests = mutableMapOf<String, Deferred<String?>>()

    init {
        scope.launch {
            menuRepository.getMenuCategories(DISCOVER_SINGLE_SHOP_ID).collectLatest { categories ->
                val names = categories.map { it.name }.filter { it.isNotBlank() }
                _uiState.update { it.copy(categories = names) }
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
        scope.launch {
            delay(DISCOVER_RECOMMENDATION_FIRST_FRAME_DELAY_MS)
            loadRecommendations()
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
        val normalizedName = item.name.normalizedMenuName()
        if (
            _uiState.value.addedMenuItemIds.contains(item.id) ||
            _uiState.value.addedMenuItemNames.contains(normalizedName) ||
            item.isAdded
        ) {
            _uiState.update { it.copy(message = "已在我的小店：${item.name}", errorMessage = null) }
            return
        }

        scope.launch {
            addToMenuMutex.withLock {
                val normalizedName = item.name.normalizedMenuName()
                val price = item.price.takeIf { it > 0.0 } ?: 12.0
                val category = item.category
                    .takeIf { it.isNotBlank() && !it.endsWith("推荐") }
                    ?: _uiState.value.categories.firstOrNull().orEmpty().ifBlank { "主食" }
                val resolvedImageUrl = item.imageUrl?.takeIf { it.isNotBlank() && !it.isLegacyRecipeImageUrl() }
                    ?: findImageForDishName(item.name, excludedImageUrl = item.imageUrl)
                val existing = roomMenuRepository.observeMenuDishes()
                    .first()
                    .any { it.name.normalizedMenuName() == normalizedName }

                if (!existing) {
                    roomMenuRepository.saveDish(
                        MenuDishDraft(
                            name = item.name,
                            price = price,
                            imageUrl = resolvedImageUrl.orEmpty(),
                            category = category,
                            description = item.subtitle
                        )
                    )
                } else {
                    markResultAsAdded(item, resolvedImageUrl)
                    _uiState.update {
                        it.copy(
                            addedMenuItemNames = it.addedMenuItemNames + normalizedName,
                            message = "已在我的小店：${item.name}",
                            errorMessage = null
                        )
                    }
                    return@withLock
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
                                result.copy(isAdded = true, imageUrl = resolvedImageUrl)
                            } else {
                                result
                            }
                        },
                        recommendations = state.recommendations.markRecommendationAdded(item, resolvedImageUrl),
                        message = "已加入我的小店：${item.name}",
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun ensureImageFor(item: DiscoverDishSearchItem) {
        if (!item.imageUrl.isNullOrBlank() && !item.imageUrl.isLegacyRecipeImageUrl()) return

        scope.launch {
            val imageUrl = findImageForDishName(item.name, excludedImageUrl = item.imageUrl)
            if (imageUrl.isNullOrBlank()) {
                logDebug(
                    level = "warn",
                    event = "ensure_image_empty",
                    query = item.name,
                    source = item.sourceLabel,
                    message = item.name
                )
                return@launch
            }
            logDebug(
                event = "ensure_image_success",
                query = item.name,
                source = item.sourceLabel,
                message = item.name,
                details = mapOf("imageUrl" to imageUrl)
            )
            replaceResultImage(item, imageUrl)
        }
    }

    fun recoverRecommendationImageFor(item: DiscoverDishSearchItem) {
        if (!item.imageUrl.isNullOrBlank() && !item.imageUrl.isLegacyRecipeImageUrl()) return
        scope.launch {
            val imageUrl = findImageForDishName(item.name, excludedImageUrl = item.imageUrl)
            if (!imageUrl.isNullOrBlank()) {
                replaceRecommendationImage(item, imageUrl)
            }
        }
    }

    private suspend fun loadRecommendations() {
        val daily = buildRecommendation(
            id = "daily",
            title = "今日推荐",
            subtitle = "每日随机更新",
            item = bimissingRecipeAssetSource.dailyRecommendation()
        )
        val fatLoss = buildRecommendation(
            id = "fat_loss",
            title = "减脂推荐",
            subtitle = "轻一点，也很好吃",
            item = bimissingRecipeAssetSource.fatLossRecommendation()
        )
        val addedNames = currentShopDishNames()
        _uiState.update { state ->
            state.copy(
                recommendations = listOfNotNull(daily, fatLoss).map { recommendation ->
                    recommendation.copy(
                        item = recommendation.item.copy(
                            isAdded = addedNames.contains(recommendation.item.name.normalizedMenuName())
                        )
                    )
                },
                addedMenuItemNames = state.addedMenuItemNames + addedNames
            )
        }
    }

    private suspend fun buildRecommendation(
        id: String,
        title: String,
        subtitle: String,
        item: com.myorderapp.ui.search.ExternalDishImageResult?
    ): DiscoverRecommendationItem? {
        item ?: return null
        val dish = toExternalResult(item).copy(
            id = "recommend:$id:${item.id.ifBlank { item.name }}",
            category = title,
            price = if (id == "fat_loss") 16.0 else 18.0
        )
        val imageUrl = findImageForDishName(
            name = dish.name,
            preferredQuery = dish.name,
            excludedImageUrl = dish.imageUrl
        ) ?: dish.imageUrl?.takeIf { it.isNotBlank() && !it.isLegacyRecipeImageUrl() }
        return DiscoverRecommendationItem(
            id = id,
            title = title,
            subtitle = subtitle,
            item = dish.copy(imageUrl = imageUrl ?: dish.imageUrl)
        )
    }

    private fun replaceRecommendationImage(item: DiscoverDishSearchItem, imageUrl: String) {
        _uiState.update { state ->
            state.copy(
                recommendations = state.recommendations.map { recommendation ->
                    if (
                        recommendation.item.id == item.id ||
                        recommendation.item.name.trim().equals(item.name.trim(), ignoreCase = true)
                    ) {
                        recommendation.copy(item = recommendation.item.copy(imageUrl = imageUrl))
                    } else {
                        recommendation
                    }
                }
            )
        }
    }

    private fun markResultAsAdded(item: DiscoverDishSearchItem, resolvedImageUrl: String? = item.imageUrl) {
        _uiState.update { state ->
            state.copy(
                results = state.results.map { result ->
                    if (
                        result.id == item.id ||
                        result.name.trim().equals(item.name.trim(), ignoreCase = true)
                    ) {
                        result.copy(isAdded = true, imageUrl = resolvedImageUrl)
                    } else {
                        result
                    }
                },
                recommendations = state.recommendations.markRecommendationAdded(item, resolvedImageUrl)
            )
        }
    }

    private fun List<DiscoverRecommendationItem>.markRecommendationAdded(
        item: DiscoverDishSearchItem,
        resolvedImageUrl: String?
    ): List<DiscoverRecommendationItem> = map { recommendation ->
        if (
            recommendation.item.id == item.id ||
            recommendation.item.name.normalizedMenuName() == item.name.normalizedMenuName()
        ) {
            recommendation.copy(item = recommendation.item.copy(isAdded = true, imageUrl = resolvedImageUrl))
        } else {
            recommendation
        }
    }

    fun recoverImageFor(item: DiscoverDishSearchItem) {
        scope.launch {
            val imageUrl = findImageForDishName(item.name, excludedImageUrl = item.imageUrl)
            if (imageUrl.isNullOrBlank() || imageUrl == item.imageUrl) {
                logDebug(
                    level = "warn",
                    event = "recover_image_empty",
                    query = item.name,
                    source = item.sourceLabel,
                    message = item.name,
                    details = mapOf("currentImageUrl" to item.imageUrl.orEmpty())
                )
                return@launch
            }
            logDebug(
                event = "recover_image_success",
                query = item.name,
                source = item.sourceLabel,
                message = item.name,
                details = mapOf("imageUrl" to imageUrl)
            )
            replaceResultImage(item, imageUrl)
        }
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
        if (restoreCachedSearch(query)) return
        _uiState.update {
            it.copy(
                isSearching = true,
                errorMessage = null
            )
        }
        logDebug(
            event = "search_start",
            query = query,
            message = "Discover search started"
        )

        var partialNetworkUnavailable = false

        try {
            var fuzzyFallbackResults: List<DiscoverDishSearchItem> = emptyList()
            var fuzzyFallbackMenuItems: List<SearchableMenuItem> = emptyList()

            suspend fun publishExactOrRememberFuzzy(
                results: List<DiscoverDishSearchItem>,
                menuItems: List<SearchableMenuItem> = emptyList()
            ): Boolean {
                val exactResults = results.exactNameMatches(query)
                if (shouldPreferEarlierImageResult(exactResults, fuzzyFallbackResults)) {
                    if (
                        publishSearchResults(
                            query = query,
                            candidateResults = fuzzyFallbackResults,
                            partialNetworkUnavailable = partialNetworkUnavailable,
                            menuItems = fuzzyFallbackMenuItems,
                            fallbackImageQuery = query
                        )
                    ) {
                        return true
                    }
                }
                if (
                    publishSearchResults(
                        query = query,
                        candidateResults = exactResults,
                        partialNetworkUnavailable = partialNetworkUnavailable,
                        menuItems = menuItems,
                        fallbackImageQuery = query
                    )
                ) {
                    return true
                }

                if (fuzzyFallbackResults.isEmpty()) {
                    val fuzzyResults = results.fuzzyNameMatches(query)
                    if (fuzzyResults.isNotEmpty()) {
                        fuzzyFallbackResults = fuzzyResults
                        fuzzyFallbackMenuItems = menuItems
                    }
                }
                return false
            }

            val xiachufangResults = try {
                xiachufangRecipeSearchSource.search(query, limit = 20)
                    .map(::toExternalResult)
                    .also { results ->
                        logSourceResults(query, "xiachufang", results)
                    }
            } catch (e: Exception) {
                partialNetworkUnavailable = true
                logSourceError(query, "xiachufang", e)
                emptyList()
            }
            if (publishExactOrRememberFuzzy(xiachufangResults)) return

            val bimissingResults = try {
                bimissingRecipeAssetSource.search(query, limit = 30)
                    .map(::toExternalResult)
                    .also { results ->
                        logSourceResults(query, "bimissing", results)
                    }
            } catch (e: Exception) {
                logSourceError(query, "bimissing", e)
                emptyList()
            }
            if (publishExactOrRememberFuzzy(bimissingResults)) return

            val localDishes = try {
                dishRepository.searchDishes(query).first()
                    .also { dishes ->
                        logDebug(
                            event = "source_result",
                            query = query,
                            source = "local_dishes",
                            message = "Local dish search returned ${dishes.size} results"
                        )
                    }
            } catch (e: Exception) {
                partialNetworkUnavailable = true
                logSourceError(query, "local_dishes", e)
                emptyList()
            }

            val menuItems = try {
                menuRepository.searchMenuItems(query).first()
                    .also { items ->
                        logDebug(
                            event = "source_result",
                            query = query,
                            source = "menu",
                            message = "Menu search returned ${items.size} results"
                        )
                    }
            } catch (e: Exception) {
                partialNetworkUnavailable = true
                logSourceError(query, "menu", e)
                emptyList()
            }

            val localAndMenuResults = localDishes.map(::toLocalResult) + menuItems.map(::toMenuResult)
            if (publishExactOrRememberFuzzy(localAndMenuResults, menuItems)) return

            val tianDishes = when (val result = tianRecipeRemoteDataSource.searchRecipes(query, num = 8)) {
                is TianResult.Success -> result.dishes.also { dishes ->
                    logDebug(
                        event = "source_result",
                        query = query,
                        source = "tian",
                        message = "Tian search returned ${dishes.size} results",
                        details = mapOf(
                            "total" to result.total.toString(),
                            "page" to result.page.toString(),
                            "names" to dishes.joinToString("|") { it.name },
                            "imageCount" to dishes.count { !it.imageUrl.isNullOrBlank() }.toString()
                        )
                    )
                }
                is TianResult.NoKey -> {
                    logDebug(
                        level = "warn",
                        event = "source_skip",
                        query = query,
                        source = "tian",
                        message = "Tian API key is missing"
                    )
                    emptyList()
                }
                is TianResult.ApiError -> {
                    partialNetworkUnavailable = true
                    logDebug(
                        level = "warn",
                        event = "source_error",
                        query = query,
                        source = "tian",
                        message = result.message,
                        details = mapOf("errorCode" to result.errorCode.toString())
                    )
                    emptyList()
                }
                is TianResult.NetworkError -> {
                    partialNetworkUnavailable = true
                    logDebug(
                        level = "warn",
                        event = "source_error",
                        query = query,
                        source = "tian",
                        message = result.message
                    )
                    emptyList()
                }
            }
            val tianResults = tianDishes
                .mapIndexed { index, dish -> toRecipeSourceResult(index, dish, "tian", "天行") }
            if (publishExactOrRememberFuzzy(tianResults, menuItems)) return

            publishSearchResults(
                query = query,
                candidateResults = fuzzyFallbackResults,
                partialNetworkUnavailable = partialNetworkUnavailable,
                menuItems = fuzzyFallbackMenuItems,
                fallbackImageQuery = query,
                allowEmpty = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logDebug(
                level = "error",
                event = "search_crash",
                query = query,
                message = e.toLogMessage()
            )
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

    private suspend fun publishSearchResults(
        query: String,
        candidateResults: List<DiscoverDishSearchItem>,
        partialNetworkUnavailable: Boolean,
        menuItems: List<SearchableMenuItem> = emptyList(),
        fallbackImageQuery: String = query,
        allowEmpty: Boolean = false
    ): Boolean {
        if (_uiState.value.query.trim() != query) return true
        val chineseCandidateResults = candidateResults.filter { it.name.hasChineseText() }
        val publishableResults = when {
            chineseCandidateResults.isNotEmpty() -> chineseCandidateResults
            allowEmpty -> emptyList()
            else -> return false
        }

        val addedMenuItemIds = _uiState.value.addedMenuItemIds
        val existingMenuItemNames = currentShopDishNames() +
            menuItems.map { it.menuItem.name.normalizedMenuName() }.toSet()
        val addedMenuItemNames = _uiState.value.addedMenuItemNames + existingMenuItemNames
        val results = publishableResults
            .distinctBy { "${it.sourceLabel}:${it.name}" }
            .map {
                it.copy(
                    isAdded = addedMenuItemIds.contains(it.id) ||
                        addedMenuItemNames.contains(it.name.normalizedMenuName())
                )
            }
            .sortedByDescending { !it.imageUrl.isNullOrBlank() && !it.imageUrl.isLegacyRecipeImageUrl() }
            .take(30)
            .withBackfilledImages(fallbackImageQuery = fallbackImageQuery)

        logDebug(
            event = "publish_results",
            query = query,
            source = results.firstOrNull()?.sourceLabel.orEmpty(),
            message = "Published ${results.size} discover results",
            details = mapOf(
                "sources" to results.groupingBy { it.sourceLabel }.eachCount()
                    .entries.joinToString("|") { "${it.key}:${it.value}" },
                "names" to results.joinToString("|") { it.name },
                "imageCount" to results.count { !it.imageUrl.isNullOrBlank() }.toString(),
                "missingImages" to results.filter { it.imageUrl.isNullOrBlank() }.joinToString("|") { it.name }
            )
        )

        _uiState.update {
            it.copy(
                isSearching = false,
                results = results,
                errorMessage = if (partialNetworkUnavailable) "部分网络结果暂不可用" else null
            )
        }
        searchCache.put(
            query,
            CachedDiscoverSearch(
                results = results.map { it.copy(isAdded = false) },
                partialNetworkUnavailable = partialNetworkUnavailable
            )
        )
        return true
    }

    private suspend fun restoreCachedSearch(query: String): Boolean {
        val cached = searchCache.get(query) ?: return false
        val addedNames = _uiState.value.addedMenuItemNames + currentShopDishNames()
        _uiState.update { state ->
            state.copy(
                isSearching = false,
                results = cached.results.map { item ->
                    item.copy(
                        isAdded = state.addedMenuItemIds.contains(item.id) ||
                            addedNames.contains(item.name.normalizedMenuName())
                    )
                },
                errorMessage = if (cached.partialNetworkUnavailable) "部分网络结果暂不可用" else null
            )
        }
        return true
    }

    private suspend fun currentShopDishNames(): Set<String> {
        return runCatching {
            roomMenuRepository.observeMenuDishes()
                .first()
                .map { it.name.normalizedMenuName() }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private suspend fun List<DiscoverDishSearchItem>.withBackfilledImages(
        fallbackImageQuery: String
    ): List<DiscoverDishSearchItem> {
        if (none { it.needsCrawledImage() }) return this

        val imageUrlByName = mutableMapOf<String, String?>()
        return map { item ->
            val cacheKey = item.name.normalizedSearchText()
            val imageUrl = if (item.needsCrawledImage()) {
                imageUrlByName.getOrPut(cacheKey) {
                    findImageForDishName(
                        name = item.name,
                        excludedImageUrl = item.imageUrl,
                        preferredQuery = item.name
                    ).also { imageUrl ->
                        logDebug(
                            level = if (imageUrl.isNullOrBlank()) "warn" else "debug",
                            event = if (imageUrl.isNullOrBlank()) "image_backfill_empty" else "image_backfill_success",
                            query = item.name,
                            source = item.sourceLabel,
                            message = item.name,
                            details = mapOf(
                                "originalQuery" to fallbackImageQuery,
                                "resultImageUrl" to imageUrl.orEmpty(),
                                "excludedImageUrl" to item.imageUrl.orEmpty()
                            )
                        )
                    }
                }
            } else {
                item.imageUrl
            }
            item.copy(imageUrl = imageUrl)
        }
    }

    private fun DiscoverDishSearchItem.needsCrawledImage(): Boolean {
        return imageUrl.isNullOrBlank() || sourceLabel == "bimissing" || imageUrl.isLegacyRecipeImageUrl()
    }

    private fun shouldPreferEarlierImageResult(
        exactResults: List<DiscoverDishSearchItem>,
        fuzzyFallbackResults: List<DiscoverDishSearchItem>
    ): Boolean {
        return exactResults.isNotEmpty() &&
            exactResults.all { it.needsCrawledImage() } &&
            fuzzyFallbackResults.any {
                it.sourceLabel == "xiachufang" &&
                    !it.imageUrl.isNullOrBlank() &&
                    !it.imageUrl.isLegacyRecipeImageUrl()
            }
    }

    private fun replaceResultImage(item: DiscoverDishSearchItem, imageUrl: String) {
        _uiState.update { state ->
            state.copy(
                results = state.results.map { result ->
                    if (
                        result.id == item.id ||
                        result.name.trim().equals(item.name.trim(), ignoreCase = true)
                    ) {
                        result.copy(imageUrl = imageUrl)
                    } else {
                        result
                    }
                }
            )
        }
    }

    private suspend fun findImageForDishName(
        name: String,
        excludedImageUrl: String? = null,
        preferredQuery: String? = null
    ): String? {
        val normalizedName = name.normalizedSearchText()
        if (normalizedName.isBlank()) return null

        val requestKey = listOf(
            normalizedName,
            preferredQuery.orEmpty().normalizedSearchText(),
            excludedImageUrl.orEmpty()
        ).joinToString("|")
        val deferred = imageRequestMutex.withLock {
            imageRequests[requestKey] ?: scope.async {
                fetchImageForDishName(name, excludedImageUrl, preferredQuery)
            }.also { imageRequests[requestKey] = it }
        }
        return try {
            deferred.await()
        } finally {
            imageRequestMutex.withLock {
                if (imageRequests[requestKey] === deferred) imageRequests.remove(requestKey)
            }
        }
    }

    private suspend fun fetchImageForDishName(
        name: String,
        excludedImageUrl: String?,
        preferredQuery: String?
    ): String? {
        val normalizedName = name.normalizedSearchText()

        val searchQueries = imageSearchQueriesForDishName(name, preferredQuery)
        for (query in searchQueries) {
            val xiachufangImages = runCatching {
                xiachufangRecipeSearchSource.search(query, limit = 12)
            }.getOrNull().orEmpty()
            val xiachufangImageUrl = xiachufangImages.bestImageUrlForName(
                originalName = normalizedName,
                query = query,
                excludedImageUrl = excludedImageUrl
            )
            if (!xiachufangImageUrl.isNullOrBlank()) return xiachufangImageUrl
        }

        for (query in searchQueries) {
            val imageUrl = runCatching {
                val imageResults = externalDishImageSource.search(query)
                val candidates = imageResults.primary + imageResults.fallback
                candidates.bestImageUrlForName(
                    originalName = normalizedName,
                    query = query,
                    excludedImageUrl = excludedImageUrl
                )
            }.getOrNull()
            if (!imageUrl.isNullOrBlank()) return imageUrl
        }

        return null
    }

    private suspend fun logSourceResults(
        query: String,
        source: String,
        results: List<DiscoverDishSearchItem>
    ) {
        logDebug(
            event = "source_result",
            query = query,
            source = source,
            message = "$source returned ${results.size} results",
            details = mapOf(
                "names" to results.joinToString("|") { it.name },
                "imageCount" to results.count { !it.imageUrl.isNullOrBlank() }.toString(),
                "imageUrls" to results.mapNotNull { it.imageUrl }.take(5).joinToString("|")
            )
        )
    }

    private suspend fun logSourceError(
        query: String,
        source: String,
        error: Exception
    ) {
        logDebug(
            level = "warn",
            event = "source_error",
            query = query,
            source = source,
            message = error.toLogMessage()
        )
    }

    private suspend fun logDebug(
        level: String = "debug",
        event: String,
        query: String = "",
        source: String = "",
        message: String = "",
        details: Map<String, String> = emptyMap()
    ) {
        // Search debug logging is intentionally disabled for release-like debug builds.
    }

    private fun Throwable.toLogMessage(): String {
        return "${javaClass.simpleName}: ${message.orEmpty()}"
    }

    private fun List<com.myorderapp.ui.search.ExternalDishImageResult>.bestImageUrlForName(
        originalName: String,
        query: String,
        excludedImageUrl: String?
    ): String? {
        val normalizedQuery = query.normalizedSearchText()
        val matchTokens = originalName.imageMatchTokens()
        val exactMatch = firstOrNull {
            it.hasUsableChineseImage(excludedImageUrl) &&
                it.name.normalizedSearchText() == originalName
        }
        val queryExactMatch = firstOrNull {
            it.hasUsableChineseImage(excludedImageUrl) &&
                it.name.normalizedSearchText() == normalizedQuery
        }
        val fuzzyMatch = firstOrNull {
            val candidateName = it.name.normalizedSearchText()
            it.hasUsableChineseImage(excludedImageUrl) &&
                (candidateName.contains(originalName) || originalName.contains(candidateName))
        }
        val queryTokenMatch = firstOrNull {
            val candidateName = it.name.normalizedSearchText()
            it.hasUsableChineseImage(excludedImageUrl) &&
                matchTokens.any { token -> candidateName.contains(token) } &&
                (candidateName.contains(normalizedQuery) || normalizedQuery.contains(candidateName))
        }

        return (exactMatch ?: queryExactMatch ?: fuzzyMatch ?: queryTokenMatch)?.imageUrl
    }

    private fun com.myorderapp.ui.search.ExternalDishImageResult.hasUsableChineseImage(
        excludedImageUrl: String?
    ): Boolean {
        val url = imageUrl?.takeIf { it.isNotBlank() } ?: return false
        return name.hasChineseText() && url != excludedImageUrl && !url.isLegacyRecipeImageUrl()
    }

    private fun imageSearchQueriesForDishName(name: String, preferredQuery: String? = null): List<String> {
        val normalizedName = name.normalizedSearchText()
        if (normalizedName.isBlank()) return emptyList()

        val queries = mutableListOf<String>()
        preferredQuery
            ?.normalizedSearchText()
            ?.takeIf { it.isNotBlank() }
            ?.let { queries += it }
        queries += normalizedName
        val cookingMethod = CookingMethods.firstOrNull { normalizedName.startsWith(it) }
        val tokens = normalizedName.imageMatchTokens()
        val mainIngredient = tokens.firstOrNull { it.length >= 2 }
        if (!mainIngredient.isNullOrBlank()) {
            queries += mainIngredient
            if (!cookingMethod.isNullOrBlank()) {
                queries += "$cookingMethod$mainIngredient"
                val genericIngredient = mainIngredient.genericIngredientNameAsciiSafe()
                if (genericIngredient != mainIngredient) {
                    queries += "$cookingMethod$genericIngredient"
                }
            }
        }

        return queries.distinct()
    }

    private fun String.imageMatchTokens(): List<String> {
        val normalizedValue = normalizedSearchText()
        val tokens = KnownFoodImageTokens
            .filter { normalizedValue.contains(it) }
            .sortedByDescending { it.length }
        if (tokens.isNotEmpty()) return tokens

        val cookingMethod = CookingMethods.firstOrNull { normalizedValue.startsWith(it) }
        val withoutMethod = cookingMethod?.let { normalizedValue.removePrefix(it) }.orEmpty()
        return withoutMethod.takeIf { it.length >= 2 }?.let { listOf(it) }.orEmpty()
    }

    private fun String.genericIngredientName(): String {
        return when {
            endsWith("鱼") -> "鱼"
            endsWith("虾") -> "虾"
            endsWith("蟹") -> "蟹"
            else -> this
        }
    }

    private fun String.genericIngredientNameAsciiSafe(): String {
        return when {
            endsWith("\u9c7c") -> "\u9c7c"
            endsWith("\u867e") -> "\u867e"
            endsWith("\u87f9") -> "\u87f9"
            else -> this
        }
    }

    private fun String?.isLegacyRecipeImageUrl(): Boolean {
        val value = this?.lowercase().orEmpty()
        return value.contains("res.hoto.cn") || value.contains("hoto.cn/")
    }

    private fun List<DiscoverDishSearchItem>.exactNameMatches(query: String): List<DiscoverDishSearchItem> {
        val normalizedQuery = query.normalizedSearchText()
        if (normalizedQuery.isBlank()) return emptyList()

        return filter { it.name.normalizedSearchText() == normalizedQuery }
    }

    private fun List<DiscoverDishSearchItem>.fuzzyNameMatches(query: String): List<DiscoverDishSearchItem> {
        val normalizedQuery = query.normalizedSearchText()
        if (normalizedQuery.isBlank()) return emptyList()

        return mapNotNull { item ->
            val score = item.name.fuzzyDishMatchScore(normalizedQuery)
            if (score > 0) item to score else null
        }
            .sortedWith(
                compareByDescending<Pair<DiscoverDishSearchItem, Int>> { it.second }
                    .thenByDescending { !it.first.imageUrl.isNullOrBlank() && !it.first.imageUrl.isLegacyRecipeImageUrl() }
            )
            .map { it.first }
    }

    private fun String.fuzzyDishMatchScore(normalizedQuery: String): Int {
        val normalizedName = normalizedSearchText()
        if (normalizedName.isBlank()) return 0
        if (normalizedName.contains(normalizedQuery)) return 100 + normalizedQuery.length

        val tokenGroups = normalizedQuery.dishMatchTokenGroups()
        if (tokenGroups.isEmpty()) return 0

        val matchedGroups = tokenGroups.count { group ->
            group.any { token -> normalizedName.contains(token) }
        }
        val requiredGroups = if (tokenGroups.size <= 2) tokenGroups.size else tokenGroups.size - 1
        return if (matchedGroups >= requiredGroups && matchedGroups >= 2) {
            60 + matchedGroups
        } else {
            0
        }
    }

    private fun String.dishMatchTokenGroups(): List<List<String>> {
        val normalizedValue = normalizedSearchText()
        if (normalizedValue.isBlank()) return emptyList()

        val groups = mutableListOf<List<String>>()
        CookingMethods
            .filter { normalizedValue.contains(it) }
            .forEach { groups += listOf(it) }
        DishSynonymTokenGroups
            .filter { group -> group.any { normalizedValue.contains(it) } }
            .forEach { groups += it }
        KnownFoodImageTokens
            .filter { token ->
                normalizedValue.contains(token) &&
                    groups.none { group -> group.any { it.contains(token) || token.contains(it) } }
            }
            .forEach { groups += listOf(it) }

        return groups.distinctBy { it.joinToString("|") }
    }

    private fun String.normalizedSearchText(): String {
        return trim()
            .lowercase()
            .replace(Regex("\\s+"), "")
    }

    private fun String.normalizedMenuName(): String = normalizedSearchText()

    private fun String.hasChineseText(): Boolean = any { it in '\u4e00'..'\u9fff' }

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
            sourceLabel = dish.source.takeIf { it == "builtin" } ?: "local"
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
            sourceLabel = result.source.ifBlank { "external" }
        )
    }

    private companion object {
        val CookingMethods = listOf(
            "\u7ea2\u70e7",
            "\u6e05\u84b8",
            "\u7cd6\u918b",
            "\u9178\u83dc",
            "\u6c34\u716e",
            "\u9999\u714e",
            "\u6cb9\u7116",
            "\u9ec4\u7116",
            "\u51c9\u62cc",
            "\u7096",
            "\u7092"
        )

        val KnownFoodImageTokens = listOf(
            "\u9ca4\u9c7c",
            "\u8349\u9c7c",
            "\u9cab\u9c7c",
            "\u9c88\u9c7c",
            "\u9ec4\u9c7c",
            "\u5e26\u9c7c",
            "\u9cca\u9c7c",
            "\u9cb3\u9c7c",
            "\u9752\u9c7c",
            "\u9ed1\u9c7c",
            "\u9c7c",
            "\u6392\u9aa8",
            "\u9e21\u86cb",
            "\u9e21\u7fc5",
            "\u725b\u8089",
            "\u7f8a\u8089",
            "\u732a\u8089",
            "\u4e94\u82b1\u8089",
            "\u9e21",
            "\u9e2d",
            "\u9e45",
            "\u867e",
            "\u87f9",
            "\u8c46\u8150",
            "\u571f\u8c46",
            "\u8304\u5b50",
            "\u767d\u83dc",
            "\u756a\u8304",
            "\u897f\u7ea2\u67ff"
        )

        val DishSynonymTokenGroups = listOf(
            listOf("\u756a\u8304", "\u897f\u7ea2\u67ff"),
            listOf("\u9e21\u86cb", "\u7092\u86cb", "\u86cb"),
            listOf("\u6392\u9aa8", "\u5c0f\u6392"),
            listOf("\u9ca4\u9c7c", "\u9c7c")
        )
    }
}
