package com.myorderapp.data.repository

import com.myorderapp.domain.model.Shop
import com.myorderapp.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SampleShopRepository : ShopRepository {

    private val shops = listOf(
        Shop(
            id = "shop_sunset_bbq",
            name = "落日炭火烤肉",
            logoUrl = "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=160&q=80",
            coverUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80",
            rating = 4.8,
            monthlySales = 1820,
            deliveryFee = 3.5,
            minOrderPrice = 18.0,
            avgDeliveryMinutes = 26,
            announcement = "炭火烤串、盖饭和夜宵套餐，现点现烤，适合两个人一起吃。",
            tags = listOf("烤肉", "夜宵", "热销"),
            distanceKm = 1.2,
            promoText = "满 25 减 6"
        ),
        Shop(
            id = "shop_orange_bowl",
            name = "橙意盖饭屋",
            logoUrl = "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=160&q=80",
            coverUrl = "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=1200&q=80",
            rating = 4.7,
            monthlySales = 2360,
            deliveryFee = 2.0,
            minOrderPrice = 15.0,
            avgDeliveryMinutes = 22,
            announcement = "招牌盖饭、咖喱饭和小食组合，出餐快，口味稳定。",
            tags = listOf("盖饭", "简餐", "出餐快"),
            distanceKm = 0.9,
            promoText = "套餐赠饮"
        ),
        Shop(
            id = "shop_coast_noodle",
            name = "海岸面馆",
            logoUrl = "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=160&q=80",
            coverUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=1200&q=80",
            rating = 4.6,
            monthlySales = 1540,
            deliveryFee = 1.5,
            minOrderPrice = 12.0,
            avgDeliveryMinutes = 19,
            announcement = "手工面、清汤面和拌面，小份小吃可加购。",
            tags = listOf("面食", "热汤", "人气"),
            distanceKm = 2.3,
            promoText = "晚八点后特价"
        ),
        Shop(
            id = "shop_honey_cafe",
            name = "蜜糖街咖啡",
            logoUrl = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=160&q=80",
            coverUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80",
            rating = 4.5,
            monthlySales = 980,
            deliveryFee = 2.5,
            minOrderPrice = 10.0,
            avgDeliveryMinutes = 28,
            announcement = "早午餐、意面、甜品和咖啡，适合轻食或下午茶。",
            tags = listOf("咖啡", "早午餐", "甜品"),
            distanceKm = 3.1,
            promoText = "吐司第二份半价"
        ),
        Shop(
            id = "shop_spicy_lane",
            name = "辣巷小火锅",
            logoUrl = "https://images.unsplash.com/photo-1516684732162-798a0062be99?auto=format&fit=crop&w=160&q=80",
            coverUrl = "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=80",
            rating = 4.9,
            monthlySales = 2050,
            deliveryFee = 4.0,
            minOrderPrice = 24.0,
            avgDeliveryMinutes = 32,
            announcement = "小火锅套餐、麻辣串串和加料拼盘，适合共享。",
            tags = listOf("火锅", "麻辣", "双人餐"),
            distanceKm = 1.8,
            promoText = "第二锅底半价"
        )
    )

    override fun getFeaturedShops(): Flow<List<Shop>> = flowOf(shops.take(3))

    override fun getNearbyShops(): Flow<List<Shop>> = flowOf(shops.sortedBy { it.distanceKm })

    override fun searchShops(query: String): Flow<List<Shop>> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            return flowOf(shops)
        }

        return flowOf(
            shops.filter { shop ->
                shop.name.lowercase().contains(normalizedQuery) ||
                    shop.tags.any { it.lowercase().contains(normalizedQuery) }
            }
        )
    }

    override fun getShopById(shopId: String): Flow<Shop?> = flowOf(shops.find { it.id == shopId })
}
