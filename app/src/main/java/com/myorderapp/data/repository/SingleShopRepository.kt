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
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private const val SHOP_PREFS = "single_shop_settings"
private const val KEY_SHOP_NAME = "shop_name"
private const val KEY_SHOP_IMAGE_URL = "shop_image_url"
private const val KEY_CATEGORIES = "categories"

class SingleShopRepository(
    private val context: Context,
    private val menuDishDao: MenuDishDao
) : ShopRepository, MenuRepository {

    private val prefs = context.getSharedPreferences(SHOP_PREFS, Context.MODE_PRIVATE)
    private val shopState = MutableStateFlow(buildShop())

    fun observeShopName(): Flow<String> = flow {
        emit(getShopName())
    }

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
    }

    suspend fun ensureSeedMenu() {
        if (menuDishDao.observeAll().first().isNotEmpty()) return
        val now = Instant.now().toString()
        defaultDishes.forEachIndexed { index, dish ->
            menuDishDao.upsert(dish.copy(sortOrder = index, updatedAt = now))
        }
    }

    override fun getFeaturedShops(): Flow<List<Shop>> = observeSingleShop().map { listOf(it) }

    override fun getNearbyShops(): Flow<List<Shop>> = getFeaturedShops()

    override fun searchShops(query: String): Flow<List<Shop>> = observeSingleShop().map { shop ->
        if (query.isBlank() || shop.name.contains(query, ignoreCase = true)) listOf(shop) else emptyList()
    }

    override fun getShopById(shopId: String): Flow<Shop?> = observeSingleShop().map { it }

    override fun getMenuCategories(shopId: String): Flow<List<MenuCategory>> {
        return menuDishDao.observeAll().map { dishes ->
            val fromDishes = dishes.map { it.category.ifBlank { "其他" } }
            (getCategoryNames() + fromDishes)
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
        return menuDishDao.observeAll().map { dishes -> dishes.map { it.toMenuItem() } }
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
            announcement = "欢迎点餐，所有菜品由本店维护。",
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
        val defaultCategories = listOf("热销", "主食", "小吃", "饮品", "甜品", "套餐", "加料")

        val defaultDishes = listOf(
            MenuDishEntity(
                id = "dish_beef_rice",
                name = "招牌牛肉饭",
                price = 26.8,
                originPrice = 32.0,
                imageUrl = "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80",
                category = "招牌必吃",
                description = "嫩牛肉配热米饭和时蔬，适合午晚餐。",
                sortOrder = 0,
                monthlySales = 126,
                stock = 32,
                isAvailable = true,
                isSignature = true,
                updatedAt = ""
            ),
            MenuDishEntity(
                id = "dish_pork_rice",
                name = "蒜香排骨饭",
                price = 29.9,
                originPrice = 35.0,
                imageUrl = "https://images.unsplash.com/photo-1563379926898-05f4575a45d8?auto=format&fit=crop&w=900&q=80",
                category = "招牌必吃",
                description = "排骨外香里嫩，配米饭和清爽小菜。",
                sortOrder = 1,
                monthlySales = 86,
                stock = 16,
                isAvailable = true,
                isSignature = false,
                updatedAt = ""
            ),
            MenuDishEntity(
                id = "dish_tomato_noodle",
                name = "番茄牛腩面",
                price = 24.8,
                originPrice = 29.0,
                imageUrl = "https://images.unsplash.com/photo-1552611052-33e04de081de?auto=format&fit=crop&w=900&q=80",
                category = "自选套餐",
                description = "番茄汤底酸甜浓郁，牛腩软烂入味。",
                sortOrder = 2,
                monthlySales = 64,
                stock = 24,
                isAvailable = true,
                isSignature = false,
                updatedAt = ""
            ),
            MenuDishEntity(
                id = "dish_chicken_snack",
                name = "香脆鸡块",
                price = 16.0,
                originPrice = 18.0,
                imageUrl = "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=900&q=80",
                category = "炸鸡小吃",
                description = "外脆里嫩，适合加购分享。",
                sortOrder = 3,
                monthlySales = 53,
                stock = 42,
                isAvailable = true,
                isSignature = false,
                updatedAt = ""
            ),
            MenuDishEntity(
                id = "dish_orange_tea",
                name = "橙香气泡茶",
                price = 9.9,
                originPrice = 12.0,
                imageUrl = "https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=900&q=80",
                category = "绝搭饮品",
                description = "清爽橙香搭配气泡口感。",
                sortOrder = 4,
                monthlySales = 91,
                stock = 58,
                isAvailable = true,
                isSignature = false,
                updatedAt = ""
            ),
            MenuDishEntity(
                id = "dish_sweet_potato",
                name = "芝士烤红薯",
                price = 12.9,
                originPrice = 15.0,
                imageUrl = "https://images.unsplash.com/photo-1518977956812-cd3dbadaaf31?auto=format&fit=crop&w=900&q=80",
                category = "素菜上新",
                description = "热烤红薯搭配芝士，香甜绵密。",
                sortOrder = 5,
                monthlySales = 28,
                stock = 0,
                isAvailable = false,
                isSignature = false,
                updatedAt = ""
            ),
            MenuDishEntity(
                id = "dish_couple_combo",
                name = "双人招牌套餐",
                price = 58.8,
                originPrice = 68.0,
                imageUrl = "https://images.unsplash.com/photo-1543353071-10c8ba85a904?auto=format&fit=crop&w=900&q=80",
                category = "多人套餐",
                description = "两份主食、一份小吃和两杯饮品。",
                sortOrder = 6,
                monthlySales = 37,
                stock = 12,
                isAvailable = false,
                isSignature = true,
                updatedAt = ""
            )
        )
    }
}
