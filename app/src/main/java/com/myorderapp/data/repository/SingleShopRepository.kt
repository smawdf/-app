package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.domain.model.Shop
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.domain.repository.ShopRepository
import com.myorderapp.ui.search.SearchableMenuItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private const val SHOP_PREFS = "single_shop_settings"
private const val KEY_SHOP_NAME = "shop_name"
private const val KEY_SHOP_IMAGE_URL = "shop_image_url"
private const val KEY_SHOP_ANNOUNCEMENT = "shop_announcement"
private const val KEY_CATEGORIES = "categories"

class SingleShopRepository(
    private val context: Context,
    private val menuDishDao: MenuDishDao
) : ShopRepository, MenuRepository {

    private val prefs = context.getSharedPreferences(SHOP_PREFS, Context.MODE_PRIVATE)
    private val shopState = MutableStateFlow(buildShop())
    private val categoryState = MutableStateFlow(getCategoryNames())

    fun observeShopName(): Flow<String> = observeSingleShop().map { it.name }

    fun getShopName(): String = prefs.getString(KEY_SHOP_NAME, null)?.takeIf { it.isNotBlank() } ?: "我的小店"

    fun updateShopName(name: String) {
        prefs.edit().putString(KEY_SHOP_NAME, name.trim().ifBlank { "我的小店" }).apply()
        refreshShopState()
    }

    fun getShopImageUrl(): String = prefs.getString(KEY_SHOP_IMAGE_URL, null).orEmpty()

    fun updateShopImageUrl(imageUrl: String) {
        prefs.edit().putString(KEY_SHOP_IMAGE_URL, imageUrl.trim()).apply()
        refreshShopState()
    }

    fun getShopAnnouncement(): String {
        return prefs.getString(KEY_SHOP_ANNOUNCEMENT, null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_SHOP_ANNOUNCEMENT
    }

    fun updateShopAnnouncement(announcement: String) {
        prefs.edit()
            .putString(KEY_SHOP_ANNOUNCEMENT, announcement.trim().ifBlank { DEFAULT_SHOP_ANNOUNCEMENT })
            .apply()
        refreshShopState()
    }

    fun getCategoryNames(): List<String> {
        val saved = prefs.getString(KEY_CATEGORIES, null)
        return saved
            ?.split("|")
            ?.normalizedShopCategories()
            ?.takeIf { it.isNotEmpty() }
            ?: defaultCategories
    }

    fun saveCategoryNames(categories: List<String>) {
        val normalized = categories.normalizedShopCategories()
        prefs.edit().putString(KEY_CATEGORIES, normalized.ifEmpty { defaultCategories }.joinToString("|")).apply()
        categoryState.value = getCategoryNames()
    }

    suspend fun removeBundledDemoMenu() {
        menuDishDao.deleteByIds(bundledDemoDishIds)
    }

    fun bundledDemoMenuIds(): Set<String> = bundledDemoDishIds.toSet()

    override fun getFeaturedShops(): Flow<List<Shop>> = observeSingleShop().map { listOf(it) }

    override fun getNearbyShops(): Flow<List<Shop>> = getFeaturedShops()

    override fun searchShops(query: String): Flow<List<Shop>> = observeSingleShop().map { shop ->
        if (query.isBlank() || shop.name.contains(query, ignoreCase = true)) listOf(shop) else emptyList()
    }

    override fun getShopById(shopId: String): Flow<Shop?> = observeSingleShop().map { it }

    override fun getMenuCategories(shopId: String): Flow<List<MenuCategory>> {
        return combine(categoryState.asStateFlow(), menuDishDao.observeAll()) { savedCategories, dishes ->
            val fromDishes = dishes.map { it.category.ifBlank { "其他" } }
            (savedCategories + fromDishes)
                .normalizedShopCategories()
                .mapIndexed { index, category ->
                    MenuCategory(
                        id = category,
                        shopId = SINGLE_SHOP_ID,
                        name = category,
                        sortOrder = index
                    )
                }
        }
    }

    override fun getMenuItems(shopId: String): Flow<List<MenuItem>> {
        return menuDishDao.observeAll().map { dishes ->
            dishes
                .filter { it.isAvailable }
                .map { it.toMenuItem() }
        }
    }

    override fun searchMenuItems(query: String): Flow<List<SearchableMenuItem>> {
        val shopName = getShopName()
        return getMenuItems(SINGLE_SHOP_ID).map { items ->
            items
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                .map { item ->
                    SearchableMenuItem(
                        shopId = SINGLE_SHOP_ID,
                        shopName = shopName,
                        menuItem = item
                    )
                }
        }
    }

    private fun observeSingleShop(): Flow<Shop> = shopState.asStateFlow()

    private fun buildShop(): Shop {
        val imageUrl = getShopImageUrl()
        return Shop(
            id = SINGLE_SHOP_ID,
            name = getShopName(),
            logoUrl = imageUrl,
            coverUrl = imageUrl,
            rating = 4.9,
            monthlySales = 0,
            deliveryFee = 0.0,
            minOrderPrice = 0.0,
            avgDeliveryMinutes = 15,
            announcement = getShopAnnouncement(),
            tags = listOf("本店点菜", "现点现做"),
            status = "营业中",
            distanceKm = 0.0,
            promoText = "支持本地点菜管理"
        )
    }

    private fun refreshShopState() {
        shopState.value = buildShop()
    }

    private fun MenuDishEntity.toMenuItem(): MenuItem = MenuItem(
        id = id,
        shopId = SINGLE_SHOP_ID,
        categoryId = category.ifBlank { "其他" },
        name = name,
        subtitle = category.ifBlank { "其他" },
        description = description.ifBlank { "本店自制菜品" },
        imageUrl = imageUrl,
        price = price,
        originPrice = originPrice,
        monthlySales = monthlySales,
        tags = listOfNotNull(
            "在售".takeIf { isAvailable },
            "招牌".takeIf { isSignature }
        )
    )

    private fun List<String>.normalizedShopCategories(): List<String> {
        return map { it.trim() }
            .filter { it.isNotBlank() && it != removedDiscoverCategoryName() }
            .distinct()
    }

    private fun removedDiscoverCategoryName(): String {
        return listOf('发', '现', '菜', '品').joinToString("")
    }

    private companion object {
        const val DEFAULT_SHOP_ANNOUNCEMENT = "欢迎来到我们的温馨小店！今天有新鲜出炉的心形披萨哦~ 🐾"
        val defaultCategories = listOf("主食披萨", "甜点蛋糕", "特调饮品")
        val bundledDemoDishIds = listOf(
            "dish_beef_rice",
            "dish_pork_rice",
            "dish_tomato_noodle",
            "dish_chicken_snack",
            "dish_orange_tea",
            "dish_sweet_potato",
            "dish_couple_combo"
        )
    }
}
