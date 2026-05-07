package com.myorderapp.data.repository

import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish
import com.myorderapp.domain.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryDishRepository(initialDishes: List<Dish> = emptyList()) : DishRepository {

    private val _dishes = MutableStateFlow(
        if (initialDishes.isNotEmpty()) initialDishes else sampleDishes
    )

    override fun getAllDishes(): Flow<List<Dish>> = _dishes

    override fun getDishesByCategory(category: String): Flow<List<Dish>> =
        _dishes.map { list -> list.filter { it.category == category } }

    override fun searchDishes(query: String): Flow<List<Dish>> =
        _dishes.map { list ->
            if (query.isBlank()) list
            else list.filter { it.name.contains(query, ignoreCase = true) }
        }

    override suspend fun getDishById(id: String): Dish? =
        _dishes.value.find { it.id == id }

    override suspend fun cacheSearchResult(dish: Dish) {
        val existing = _dishes.value.find { it.id == dish.id }
        if (existing == null) {
            _dishes.value = _dishes.value + dish
        } else {
            _dishes.value = _dishes.value.map { if (it.id == dish.id) dish else it }
        }
    }

    override suspend fun addDish(dish: Dish): String {
        val id = "dish_${System.currentTimeMillis()}"
        val newDish = dish.copy(id = id)
        _dishes.value = _dishes.value + newDish
        return id
    }

    override suspend fun updateDish(dish: Dish) {
        _dishes.value = _dishes.value.map { if (it.id == dish.id) dish else it }
    }

    override suspend fun deleteDish(id: String) {
        _dishes.value = _dishes.value.filter { it.id != id }
    }

    override fun getRecentDishes(limit: Int): Flow<List<Dish>> =
        _dishes.map { list -> list.take(limit) }

    companion object {
        val sampleDishes = listOf(
            Dish(
                id = "1", name = "宫保鸡丁", source = "custom", category = "中餐",
                cookTimeMin = 30, difficulty = 2, ingredients = listOf("鸡胸肉 300g", "花生米 50g", "干辣椒 8个"),
                cookSteps = listOf(
                    CookStep(1, "鸡胸肉切丁，加料酒、生抽腌制15分钟", tip = "加少许淀粉让肉更嫩"),
                    CookStep(2, "调碗汁：醋、生抽、白糖、淀粉、水拌匀", tip = "糖醋比例 1:1"),
                    CookStep(3, "热油炒鸡丁至变色盛出，爆香辣椒花椒"),
                    CookStep(4, "回锅鸡丁，倒入碗汁翻炒，加花生米")
                ),
                notes = "她不太能吃辣，少放干辣椒",
                createdBy = "你创建", createdAt = "2026-04-28",
                whoLikes = listOf("你", "她")
            ),
            Dish(
                id = "2", name = "番茄炒蛋", source = "custom", category = "中餐",
                cookTimeMin = 15, difficulty = 1, ingredients = listOf("番茄 2个", "鸡蛋 3个", "葱花 适量"),
                cookSteps = listOf(
                    CookStep(1, "番茄切块，鸡蛋打散加盐"),
                    CookStep(2, "热油炒鸡蛋至凝固盛出"),
                    CookStep(3, "炒番茄出汁，加糖调味"),
                    CookStep(4, "倒回鸡蛋翻炒均匀")
                ),
                createdBy = "你创建", createdAt = "2026-04-25",
                whoLikes = listOf("你", "她")
            ),
            Dish(
                id = "3", name = "红烧排骨", source = "custom", category = "中餐",
                cookTimeMin = 60, difficulty = 3, ingredients = listOf("排骨 500g", "生抽", "老抽", "冰糖", "八角"),
                cookSteps = listOf(
                    CookStep(1, "排骨焯水去血沫"),
                    CookStep(2, "炒糖色，下排骨翻炒"),
                    CookStep(3, "加开水没过排骨，小火炖40分钟"),
                    CookStep(4, "大火收汁")
                ),
                createdBy = "她创建", createdAt = "2026-04-20",
                whoLikes = listOf("你")
            ),
            Dish(
                id = "4", name = "意大利面", source = "custom", category = "西餐",
                cookTimeMin = 25, difficulty = 2, ingredients = listOf("意面 200g", "番茄酱", "肉末", "洋葱"),
                cookSteps = listOf(
                    CookStep(1, "煮意面至弹牙"),
                    CookStep(2, "炒香洋葱和肉末"),
                    CookStep(3, "加番茄酱煮成酱汁"),
                    CookStep(4, "拌入意面")
                ),
                createdBy = "她创建", createdAt = "2026-04-18",
                whoLikes = listOf("她")
            ),
            Dish(
                id = "5", name = "焦糖布丁", source = "custom", category = "甜品",
                cookTimeMin = 45, difficulty = 3, ingredients = listOf("鸡蛋 2个", "牛奶 250ml", "白糖", "香草精"),
                cookSteps = listOf(
                    CookStep(1, "制作焦糖糖浆"),
                    CookStep(2, "牛奶加热加糖搅拌"),
                    CookStep(3, "蛋液与牛奶混合过滤"),
                    CookStep(4, "水浴法烤制")
                ),
                createdBy = "你创建", createdAt = "2026-04-15",
                whoLikes = listOf("你", "她")
            ),
            Dish(
                id = "s1", name = "红烧肉", source = "external", externalSource = "Spoonacular",
                category = "中餐", cookTimeMin = 60, difficulty = 3,                 ingredients = listOf("五花肉 500g", "生抽", "老抽", "冰糖", "八角"),
                cookSteps = listOf(
                    CookStep(1, "五花肉切块焯水"),
                    CookStep(2, "炒糖色放入肉块"),
                    CookStep(3, "加水炖煮40分钟")
                ),
                createdBy = "Spoonacular"
            ),
            Dish(
                id = "s2", name = "凯撒沙拉", source = "external", externalSource = "Spoonacular",
                category = "西餐", cookTimeMin = 15, difficulty = 1,
                ingredients = listOf("生菜", "鸡胸肉", "培根", "帕玛森芝士", "凯撒酱"),
                cookSteps = listOf(
                    CookStep(1, "鸡胸肉煎熟切条"),
                    CookStep(2, "生菜洗净撕碎"),
                    CookStep(3, "培根煎脆切碎"),
                    CookStep(4, "所有材料混合淋酱")
                ),
                createdBy = "Spoonacular"
            ),
            Dish(
                id = "s3", name = "抹茶拿铁", source = "external", externalSource = "Spoonacular",
                category = "饮品", cookTimeMin = 10, difficulty = 1,
                ingredients = listOf("抹茶粉", "牛奶", "糖浆"),
                cookSteps = listOf(
                    CookStep(1, "抹茶粉过筛加少量热水搅拌"),
                    CookStep(2, "牛奶加热至微沸"),
                    CookStep(3, "将抹茶液倒入杯中，加入热牛奶")
                ),
                createdBy = "Spoonacular"
            ),
            Dish(
                id = "s4", name = "芒果糯米饭", source = "external", externalSource = "Spoonacular",
                category = "甜品", cookTimeMin = 40, difficulty = 2,
                ingredients = listOf("糯米", "芒果", "椰浆", "糖", "盐"),
                cookSteps = listOf(
                    CookStep(1, "糯米浸泡4小时后蒸熟"),
                    CookStep(2, "椰浆加糖盐加热"),
                    CookStep(3, "椰浆拌入糯米饭"),
                    CookStep(4, "芒果切片摆盘")
                ),
                createdBy = "Spoonacular"
            )
        )
    }
}
