package com.myorderapp.data.repository

import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.domain.repository.MenuRepository
import com.myorderapp.ui.search.SearchableMenuItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SampleMenuRepository : MenuRepository {

    private val shopNames = mapOf(
        "shop_sunset_bbq" to "落日炭火烤肉",
        "shop_orange_bowl" to "橙意盖饭屋",
        "shop_coast_noodle" to "海岸面馆",
        "shop_honey_cafe" to "蜜糖街咖啡",
        "shop_spicy_lane" to "辣巷小火锅"
    )

    private val categoriesByShop = mapOf(
        "shop_sunset_bbq" to listOf(
            MenuCategory("cat_grill", "shop_sunset_bbq", "炭烤招牌", 0),
            MenuCategory("cat_bento", "shop_sunset_bbq", "烤肉盖饭", 1),
            MenuCategory("cat_drink", "shop_sunset_bbq", "冰饮", 2)
        ),
        "shop_orange_bowl" to listOf(
            MenuCategory("cat_signature", "shop_orange_bowl", "招牌盖饭", 0),
            MenuCategory("cat_curry", "shop_orange_bowl", "咖喱饭", 1),
            MenuCategory("cat_snack", "shop_orange_bowl", "小食", 2)
        ),
        "shop_coast_noodle" to listOf(
            MenuCategory("cat_broth", "shop_coast_noodle", "汤面", 0),
            MenuCategory("cat_dry", "shop_coast_noodle", "拌面", 1),
            MenuCategory("cat_small", "shop_coast_noodle", "小吃", 2)
        ),
        "shop_honey_cafe" to listOf(
            MenuCategory("cat_toast", "shop_honey_cafe", "吐司早午餐", 0),
            MenuCategory("cat_pasta", "shop_honey_cafe", "意面", 1),
            MenuCategory("cat_sweets", "shop_honey_cafe", "甜品", 2)
        ),
        "shop_spicy_lane" to listOf(
            MenuCategory("cat_combo", "shop_spicy_lane", "火锅套餐", 0),
            MenuCategory("cat_skewer", "shop_spicy_lane", "麻辣串串", 1),
            MenuCategory("cat_addon", "shop_spicy_lane", "加料", 2)
        )
    )

    private val itemsByShop = mapOf(
        "shop_sunset_bbq" to listOf(
            MenuItem(
                id = "item_bbq_1",
                shopId = "shop_sunset_bbq",
                categoryId = "cat_grill",
                name = "炭烤牛肉串盒",
                subtitle = "12 串牛肉，橙香酱汁",
                description = "炭火烤牛肉串，配酸洋葱和微辣干碟。",
                imageUrl = "https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?auto=format&fit=crop&w=900&q=80",
                price = 16.9,
                originPrice = 19.9,
                monthlySales = 620,
                tags = listOf("微辣", "热销")
            ),
            MenuItem(
                id = "item_bbq_2",
                shopId = "shop_sunset_bbq",
                categoryId = "cat_bento",
                name = "烟熏鸡腿盖饭",
                subtitle = "烤鸡腿、玉米、溏心蛋",
                description = "蒜香米饭铺上烤鸡腿肉，搭配烤彩椒。",
                imageUrl = "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80",
                price = 13.5,
                originPrice = 15.8,
                monthlySales = 410,
                tags = listOf("套餐")
            ),
            MenuItem(
                id = "item_bbq_3",
                shopId = "shop_sunset_bbq",
                categoryId = "cat_drink",
                name = "柚香气泡茶",
                subtitle = "柚子、茉莉、气泡水",
                description = "冷萃茉莉茶加入柚子果香和气泡水。",
                imageUrl = "https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=900&q=80",
                price = 4.8,
                monthlySales = 260,
                tags = listOf("清爽")
            )
        ),
        "shop_orange_bowl" to listOf(
            MenuItem(
                id = "item_orange_1",
                shopId = "shop_orange_bowl",
                categoryId = "cat_signature",
                name = "橙椒牛肉盖饭",
                subtitle = "店内招牌，热米饭打底",
                description = "嫩牛肉、橙椒酱、西兰花和芝麻，咸甜微辣。",
                imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80",
                price = 14.9,
                originPrice = 17.5,
                monthlySales = 880,
                tags = listOf("招牌")
            ),
            MenuItem(
                id = "item_orange_2",
                shopId = "shop_orange_bowl",
                categoryId = "cat_curry",
                name = "金汤咖喱鸡饭",
                subtitle = "浓郁咖喱配烤蔬菜",
                description = "鸡肉咖喱搭配南瓜、土豆和溏心蛋。",
                imageUrl = "https://images.unsplash.com/photo-1604908176997-431543be0a0d?auto=format&fit=crop&w=900&q=80",
                price = 13.2,
                monthlySales = 670,
                tags = listOf("暖胃")
            ),
            MenuItem(
                id = "item_orange_3",
                shopId = "shop_orange_bowl",
                categoryId = "cat_snack",
                name = "香脆鸡块",
                subtitle = "外脆里嫩，配柠檬蛋黄酱",
                description = "炸鸡块配清爽蘸酱，适合加购分享。",
                imageUrl = "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=900&q=80",
                price = 6.9,
                monthlySales = 540,
                tags = listOf("小食")
            )
        ),
        "shop_coast_noodle" to listOf(
            MenuItem(
                id = "item_noodle_1",
                shopId = "shop_coast_noodle",
                categoryId = "cat_broth",
                name = "海盐牛肉汤面",
                subtitle = "清汤牛肉，手工面",
                description = "清爽牛肉汤底，搭配牛腩和手工拉面。",
                imageUrl = "https://images.unsplash.com/photo-1617093727343-374698b1b08d?auto=format&fit=crop&w=900&q=80",
                price = 15.6,
                monthlySales = 490,
                tags = listOf("热汤")
            ),
            MenuItem(
                id = "item_noodle_2",
                shopId = "shop_coast_noodle",
                categoryId = "cat_dry",
                name = "芝麻辣油拌面",
                subtitle = "芝麻酱、辣油、卤肉",
                description = "拌面裹满芝麻酱和辣油，配慢炖卤肉。",
                imageUrl = "https://images.unsplash.com/photo-1618889482923-38250401a84e?auto=format&fit=crop&w=900&q=80",
                price = 12.8,
                monthlySales = 420,
                tags = listOf("微辣")
            ),
            MenuItem(
                id = "item_noodle_3",
                shopId = "shop_coast_noodle",
                categoryId = "cat_small",
                name = "葱油饼卷",
                subtitle = "酥脆多层，配蘸汁",
                description = "葱油饼卷成小份，配酱油醋蘸汁。",
                imageUrl = "https://images.unsplash.com/photo-1517244683847-7456b63c5969?auto=format&fit=crop&w=900&q=80",
                price = 5.5,
                monthlySales = 210,
                tags = listOf("小吃")
            )
        ),
        "shop_honey_cafe" to listOf(
            MenuItem(
                id = "item_cafe_1",
                shopId = "shop_honey_cafe",
                categoryId = "cat_toast",
                name = "蜂蜜黄油法式吐司",
                subtitle = "莓果、奶油、糖浆",
                description = "厚切布里欧吐司，配蜂蜜黄油、莓果和香草奶油。",
                imageUrl = "https://images.unsplash.com/photo-1484723091739-30a097e8f929?auto=format&fit=crop&w=900&q=80",
                price = 11.8,
                monthlySales = 300,
                tags = listOf("甜口")
            ),
            MenuItem(
                id = "item_cafe_2",
                shopId = "shop_honey_cafe",
                categoryId = "cat_pasta",
                name = "烤番茄奶油意面",
                subtitle = "顺滑酱汁，帕玛森芝士",
                description = "烤番茄奶油酱包裹螺旋面，撒帕玛森芝士。",
                imageUrl = "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?auto=format&fit=crop&w=900&q=80",
                price = 13.9,
                monthlySales = 190,
                tags = listOf("轻食")
            ),
            MenuItem(
                id = "item_cafe_3",
                shopId = "shop_honey_cafe",
                categoryId = "cat_sweets",
                name = "焦香巴斯克芝士蛋糕",
                subtitle = "焦糖表层，内芯绵密",
                description = "经典巴斯克芝士蛋糕，焦香表层配柔软内芯。",
                imageUrl = "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=900&q=80",
                price = 7.4,
                monthlySales = 240,
                tags = listOf("甜品")
            )
        ),
        "shop_spicy_lane" to listOf(
            MenuItem(
                id = "item_hotpot_1",
                shopId = "shop_spicy_lane",
                categoryId = "cat_combo",
                name = "双椒小火锅套餐",
                subtitle = "牛肉、豆腐、藕片、粉面",
                description = "双人小火锅组合，双汤底加丰富配菜。",
                imageUrl = "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=900&q=80",
                price = 24.9,
                originPrice = 29.9,
                monthlySales = 710,
                tags = listOf("共享", "热销")
            ),
            MenuItem(
                id = "item_hotpot_2",
                shopId = "shop_spicy_lane",
                categoryId = "cat_skewer",
                name = "麻辣牛肉串包",
                subtitle = "10 串麻辣牛肉",
                description = "牛肉串裹麻辣调味，带孜然香气。",
                imageUrl = "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=900&q=80",
                price = 12.6,
                monthlySales = 500,
                tags = listOf("麻辣")
            ),
            MenuItem(
                id = "item_hotpot_3",
                shopId = "shop_spicy_lane",
                categoryId = "cat_addon",
                name = "手打虾滑",
                subtitle = "新鲜虾滑，适合下锅",
                description = "细腻虾滑，入锅后弹嫩鲜甜。",
                imageUrl = "https://images.unsplash.com/photo-1511690743698-d9d85f2fbf38?auto=format&fit=crop&w=900&q=80",
                price = 8.5,
                monthlySales = 285,
                tags = listOf("加料")
            )
        )
    )

    override fun getMenuCategories(shopId: String): Flow<List<MenuCategory>> {
        return flowOf(categoriesByShop[shopId].orEmpty().sortedBy { it.sortOrder })
    }

    override fun getMenuItems(shopId: String): Flow<List<MenuItem>> {
        return flowOf(itemsByShop[shopId].orEmpty())
    }

    override fun searchMenuItems(query: String): Flow<List<SearchableMenuItem>> {
        val normalizedQuery = query.trim().lowercase()
        val results = itemsByShop.entries.flatMap { (shopId, items) ->
            items.filter { item ->
                normalizedQuery.isBlank() ||
                    item.name.lowercase().contains(normalizedQuery) ||
                    item.subtitle.lowercase().contains(normalizedQuery) ||
                    item.description.lowercase().contains(normalizedQuery) ||
                    item.tags.any { it.lowercase().contains(normalizedQuery) }
            }.map { item ->
                SearchableMenuItem(
                    shopId = shopId,
                    shopName = shopNames[shopId] ?: shopId,
                    menuItem = item
                )
            }
        }
        return flowOf(results)
    }
}
