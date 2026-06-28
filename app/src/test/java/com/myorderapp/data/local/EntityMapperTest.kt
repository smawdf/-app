package com.myorderapp.data.local

import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.EntityMapper.toEntity
import com.myorderapp.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class EntityMapperTest {

    // ── Dish round-trip ──────────────────────────────────────────────

    @Test
    fun `dish round-trip preserves all fields`() {
        val dish = Dish(
            id = "dish-1",
            pairId = "pair-1",
            name = "宫保鸡丁",
            source = "custom",
            externalId = null,
            externalSource = null,
            category = "中餐",
            imageUrl = "https://example.com/img.jpg",
            cookSteps = listOf(
                CookStep(step = 1, description = "切丁", tip = "大小均匀"),
                CookStep(step = 2, description = "炒制", tip = null, imageUrl = null)
            ),
            ingredients = listOf("鸡胸肉", "花生米", "干辣椒"),
            difficulty = 3,
            cookTimeMin = 30,
            whoLikes = listOf("你", "她"),
            rating = 4.5f,
            notes = "经典川菜",
            createdBy = "user-1",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-02T00:00:00Z"
        )

        val entity = dish.toEntity()
        val restored = entity.toDomain()

        assertEquals(dish.id, restored.id)
        assertEquals(dish.pairId, restored.pairId)
        assertEquals(dish.name, restored.name)
        assertEquals(dish.source, restored.source)
        assertEquals(dish.externalId, restored.externalId)
        assertEquals(dish.externalSource, restored.externalSource)
        assertEquals(dish.category, restored.category)
        assertEquals(dish.imageUrl, restored.imageUrl)
        assertEquals(dish.difficulty, restored.difficulty)
        assertEquals(dish.cookTimeMin, restored.cookTimeMin)
        assertEquals(dish.rating, restored.rating, 0.001f)
        assertEquals(dish.notes, restored.notes)
        assertEquals(dish.createdBy, restored.createdBy)
        assertEquals(dish.createdAt, restored.createdAt)
        assertEquals(dish.updatedAt, restored.updatedAt)
    }

    @Test
    fun `dish round-trip preserves cookSteps JSON serialization`() {
        val dish = Dish(
            id = "dish-2",
            cookSteps = listOf(
                CookStep(step = 1, description = "切菜"),
                CookStep(step = 2, description = "热油", tip = "大火"),
                CookStep(step = 3, description = "翻炒", tip = null, imageUrl = "https://img.com/step3.jpg")
            )
        )

        val restored = dish.toEntity().toDomain()

        assertEquals(3, restored.cookSteps.size)
        assertEquals(1, restored.cookSteps[0].step)
        assertEquals("切菜", restored.cookSteps[0].description)
        assertEquals("大火", restored.cookSteps[1].tip)
        assertEquals("https://img.com/step3.jpg", restored.cookSteps[2].imageUrl)
    }

    @Test
    fun `dish round-trip preserves ingredients JSON serialization`() {
        val dish = Dish(
            id = "dish-3",
            ingredients = listOf("盐", "酱油", "料酒", "淀粉")
        )

        val restored = dish.toEntity().toDomain()

        assertEquals(4, restored.ingredients.size)
        assertEquals(listOf("盐", "酱油", "料酒", "淀粉"), restored.ingredients)
    }

    @Test
    fun `dish round-trip handles empty lists`() {
        val dish = Dish(
            id = "dish-empty",
            cookSteps = emptyList(),
            ingredients = emptyList(),
            whoLikes = emptyList()
        )

        val entity = dish.toEntity()

        assertEquals("[]", entity.cookStepsJson)
        assertEquals("[]", entity.ingredientsJson)
        assertEquals("[]", entity.whoLikesJson)

        val restored = entity.toDomain()
        assertTrue(restored.cookSteps.isEmpty())
        assertTrue(restored.ingredients.isEmpty())
        assertTrue(restored.whoLikes.isEmpty())
    }

    @Test
    fun `dish round-trip preserves whoLikes JSON serialization`() {
        val dish = Dish(
            id = "dish-4",
            whoLikes = listOf("小明", "小红", "小刚")
        )

        val restored = dish.toEntity().toDomain()
        assertEquals(listOf("小明", "小红", "小刚"), restored.whoLikes)
    }

    @Test
    fun `dish round-trip preserves null fields`() {
        val dish = Dish(
            id = "dish-null",
            externalId = null,
            externalSource = null,
            imageUrl = null
        )

        val restored = dish.toEntity().toDomain()

        assertNull(restored.externalId)
        assertNull(restored.externalSource)
        assertNull(restored.imageUrl)
    }

    // ── Meal round-trip ──────────────────────────────────────────────

    @Test
    fun `meal round-trip preserves all fields`() {
        val meal = Meal(
            id = "meal-1",
            pairId = "pair-1",
            mealType = "dinner",
            date = "2025-06-15",
            status = "confirmed",
            createdBy = "user-1",
            confirmedAt = "2025-06-15T18:00:00Z",
            createdAt = "2025-06-15T17:00:00Z",
            updatedAt = "2025-06-15T18:00:00Z"
        )

        val entity = meal.toEntity()
        val restored = entity.toDomain()

        assertEquals(meal.id, restored.id)
        assertEquals(meal.pairId, restored.pairId)
        assertEquals(meal.mealType, restored.mealType)
        assertEquals(meal.date, restored.date)
        assertEquals(meal.status, restored.status)
        assertEquals(meal.createdBy, restored.createdBy)
        assertEquals(meal.confirmedAt, restored.confirmedAt)
        assertEquals(meal.createdAt, restored.createdAt)
        assertEquals(meal.updatedAt, restored.updatedAt)
    }

    @Test
    fun `meal round-trip handles null confirmedAt`() {
        val meal = Meal(
            id = "meal-2",
            pairId = "pair-1",
            confirmedAt = null
        )

        val restored = meal.toEntity().toDomain()
        assertNull(restored.confirmedAt)
    }

    @Test
    fun `meal entity defaults match domain defaults`() {
        val entity = com.myorderapp.data.local.entity.MealEntity(
            id = "meal-default",
            pairId = "pair-1"
        )
        val domain = entity.toDomain()

        assertEquals("lunch", domain.mealType)
        assertEquals("ordering", domain.status)
    }

    // ── Profile round-trip ───────────────────────────────────────────

    @Test
    fun `profile round-trip preserves all fields`() {
        val profile = Profile(
            id = "",
            userId = "user-1",
            pairId = "pair-1",
            nickname = "测试用户",
            avatarUrl = "https://example.com/avatar.png",
            tastePrefs = DietaryPreference(
                spicy = true,
                sweet = false,
                sour = true,
                salty = false,
                light = true,
                heavy = false,
                custom = listOf("不吃辣", "少油")
            ),
            allergies = listOf("花生", "海鲜"),
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-06-01T00:00:00Z"
        )

        val entity = profile.toEntity()
        val restored = entity.toDomain()

        assertEquals(profile.userId, restored.userId)
        assertEquals(profile.pairId, restored.pairId)
        assertEquals(profile.nickname, restored.nickname)
        assertEquals(profile.avatarUrl, restored.avatarUrl)
        assertEquals(profile.tastePrefs.spicy, restored.tastePrefs.spicy)
        assertEquals(profile.tastePrefs.sweet, restored.tastePrefs.sweet)
        assertEquals(profile.tastePrefs.sour, restored.tastePrefs.sour)
        assertEquals(profile.tastePrefs.salty, restored.tastePrefs.salty)
        assertEquals(profile.tastePrefs.light, restored.tastePrefs.light)
        assertEquals(profile.tastePrefs.heavy, restored.tastePrefs.heavy)
        assertEquals(profile.tastePrefs.custom, restored.tastePrefs.custom)
        assertEquals(profile.allergies, restored.allergies)
        assertEquals(profile.createdAt, restored.createdAt)
        assertEquals(profile.updatedAt, restored.updatedAt)
    }

    @Test
    fun `profile round-trip serializes tastePrefs JSON correctly`() {
        val profile = Profile(
            userId = "user-2",
            tastePrefs = DietaryPreference(
                spicy = true,
                sweet = false,
                sour = false,
                salty = true,
                light = false,
                heavy = true,
                custom = listOf("素食日")
            )
        )

        val entity = profile.toEntity()

        // Verify the JSON string contains expected fields
        assertTrue(entity.tastePrefsJson.contains("\"spicy\":true"))
        assertTrue(entity.tastePrefsJson.contains("\"salty\":true"))
        assertTrue(entity.tastePrefsJson.contains("\"heavy\":true"))
        assertTrue(entity.tastePrefsJson.contains("素食日"))

        val restored = entity.toDomain()
        assertTrue(restored.tastePrefs.spicy)
        assertTrue(restored.tastePrefs.salty)
        assertTrue(restored.tastePrefs.heavy)
        assertFalse(restored.tastePrefs.sweet)
    }

    @Test
    fun `profile round-trip serializes allergies JSON correctly`() {
        val profile = Profile(
            userId = "user-3",
            allergies = listOf("花生", "牛奶", "麸质")
        )

        val entity = profile.toEntity()

        assertTrue(entity.allergiesJson.contains("花生"))
        assertTrue(entity.allergiesJson.contains("牛奶"))
        assertTrue(entity.allergiesJson.contains("麸质"))

        val restored = entity.toDomain()
        assertEquals(3, restored.allergies.size)
        assertEquals(listOf("花生", "牛奶", "麸质"), restored.allergies)
    }

    @Test
    fun `profile round-trip handles empty tastePrefs and allergies`() {
        val profile = Profile(
            userId = "user-empty",
            tastePrefs = DietaryPreference(),
            allergies = emptyList()
        )

        val entity = profile.toEntity()
        val restored = entity.toDomain()

        assertFalse(restored.tastePrefs.spicy)
        assertTrue(restored.tastePrefs.custom.isEmpty())
        assertTrue(restored.allergies.isEmpty())
    }

    @Test
    fun `profile round-trip handles null avatarUrl`() {
        val profile = Profile(
            userId = "user-4",
            avatarUrl = null
        )

        val restored = profile.toEntity().toDomain()
        assertNull(restored.avatarUrl)
    }

    @Test
    fun `wishlist round-trip preserves all fields`() {
        val item = WishlistItem(
            id = "wish-1",
            pairId = "pair-1",
            dishId = "dish-1",
            dishName = "Mapo tofu",
            dishCategory = "Sichuan",
            dishImageUrl = "https://example.com/mapo.jpg",
            externalSource = "juhe",
            addedBy = "user-1",
            addedByName = "Ada",
            status = "pending",
            notes = "Try this weekend",
            createdAt = "2026-06-26T12:00:00Z"
        )

        val entity = item.toEntity()
        val restored = entity.toDomain()

        assertEquals(item.id, restored.id)
        assertEquals(item.pairId, restored.pairId)
        assertEquals(item.dishId, restored.dishId)
        assertEquals(item.dishName, restored.dishName)
        assertEquals(item.dishCategory, restored.dishCategory)
        assertEquals(item.dishImageUrl, restored.dishImageUrl)
        assertEquals(item.externalSource, restored.externalSource)
        assertEquals(item.addedBy, restored.addedBy)
        assertEquals(item.addedByName, restored.addedByName)
        assertEquals(item.status, restored.status)
        assertEquals(item.notes, restored.notes)
        assertEquals(item.createdAt, restored.createdAt)
    }
}
