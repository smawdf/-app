package com.myorderapp.data.repository

import android.content.Context
import com.myorderapp.data.local.dao.MenuDishDao
import com.myorderapp.data.local.entity.MenuDishEntity
import com.myorderapp.data.remote.supabase.CloudErrorLogger
import com.myorderapp.data.remote.supabase.SessionManager
import com.myorderapp.data.remote.supabase.SupabaseClientProvider
import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.domain.model.Shop
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.domain.repository.ShopRepository
import com.myorderapp.ui.search.SearchableMenuItem
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val SHOP_PREFS = "single_shop_settings"
private const val KEY_SHOP_NAME = "shop_name"
private const val KEY_SHOP_IMAGE_URL = "shop_image_url"
private const val KEY_SHOP_ANNOUNCEMENT = "shop_announcement"
private const val KEY_CATEGORIES = "categories"

class SingleShopRepository(
    private val context: Context,
    private val menuDishDao: MenuDishDao,
    private val session: SessionManager,
    private val cloudErrorLogger: CloudErrorLogger
) : ShopRepository, MenuRepository {

    private val client by lazy { SupabaseClientProvider.client }
    private val prefs = context.getSharedPreferences(SHOP_PREFS, Context.MODE_PRIVATE)
    private val shopState = MutableStateFlow(buildShop())
    private val categoryState = MutableStateFlow(getCategoryNames())

    fun observeShopName(): Flow<String> = observeSingleShop().map { it.name }

    fun getShopName(): String = prefs.getString(KEY_SHOP_NAME, null)?.takeIf { it.isNotBlank() } ?: "我的小店"

    fun updateShopName(name: String) {
        prefs.edit().putString(KEY_SHOP_NAME, name.trim().ifBlank { "我的小店" }).apply()
        refreshShopState()
        syncShopSettingsToCloud()
    }

    fun getShopImageUrl(): String = prefs.getString(KEY_SHOP_IMAGE_URL, null).orEmpty().takeIfCloudImageUrl().orEmpty()

    fun updateShopImageUrl(imageUrl: String) {
        prefs.edit().putString(KEY_SHOP_IMAGE_URL, imageUrl.takeIfCloudImageUrl().orEmpty()).apply()
        refreshShopState()
        syncShopSettingsToCloud()
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
        syncShopSettingsToCloud()
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
        syncShopSettingsToCloud()
    }

    suspend fun loadFromCloud() {
        val pairId = activePairId() ?: return
        try {
            val remote = client.from("shop_settings").select {
                filter { eq("pair_id", pairId) }
            }.decodeList<RemoteShopSettings>().firstOrNull()
            if (remote != null) {
                prefs.edit()
                    .putString(KEY_SHOP_NAME, remote.name)
                    .putString(KEY_SHOP_IMAGE_URL, remote.imageUrl.takeIfCloudImageUrl().orEmpty())
                    .putString(KEY_SHOP_ANNOUNCEMENT, remote.announcement)
                    .putString(KEY_CATEGORIES, remote.categories.normalizedShopCategories().joinToString("|"))
                    .apply()
                refreshShopState()
                categoryState.value = getCategoryNames()
            } else {
                syncShopSettingsToCloud()
            }
        } catch (e: Exception) {
            cloudErrorLogger.log("shop", "load_settings", e, "pairId=$pairId")
        }
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
        return combine(categoryState.asStateFlow(), menuDishDao.observeByPair(localPairId())) { savedCategories, dishes ->
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
        return menuDishDao.observeByPair(localPairId()).map { dishes ->
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

    private fun syncShopSettingsToCloud() {
        val pairId = activePairId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.from("shop_settings").upsert(
                    RemoteShopSettings(
                        pairId = pairId,
                        name = getShopName(),
                        imageUrl = getShopImageUrl(),
                        announcement = getShopAnnouncement(),
                        categories = getCategoryNames(),
                        updatedAt = Instant.now().toString()
                    )
                ) { select() }
            } catch (e: Exception) {
                cloudErrorLogger.log("shop", "sync_settings", e, "pairId=$pairId")
            }
        }
    }

    private fun activePairId(): String? {
        return session.currentPairId
            .takeIf { it.isNotBlank() && it != DEFAULT_PAIR_ID }
            ?: session.currentUserId.takeIf { it.isNotBlank() }?.let { "user:$it" }
    }

    private fun localPairId(): String = activePairId().orEmpty()

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

    private fun String.takeIfCloudImageUrl(): String? {
        return trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun removedDiscoverCategoryName(): String {
        return listOf('发', '现', '菜', '品').joinToString("")
    }

    private companion object {
        const val DEFAULT_PAIR_ID = "00000000-0000-0000-0000-000000000000"
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

@Serializable
private data class RemoteShopSettings(
    @SerialName("pair_id") val pairId: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String,
    val announcement: String,
    val categories: List<String> = emptyList(),
    @SerialName("updated_at") val updatedAt: String
)
