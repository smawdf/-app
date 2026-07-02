package com.myorderapp.data.remote.recipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExternalDishImageMapperTest {

    @Test
    fun `spoonacular external result uses Chinese visible labels`() {
        val result = SpoonacularMapper.toExternalResult(
            SpoonacularRecipe(
                id = 1,
                title = "Tomato Pasta",
                image = "https://example.com/image-312x231.jpg",
                imageType = "jpg"
            ),
            query = "番茄面"
        )

        assertEquals("番茄面", result.name)
        assertEquals("外部图片", result.source)
        assertEquals("图片参考", result.subtitle)
        assertEquals("网络菜品图片参考", result.description)
        assertFalse(result.source.contains("Spoonacular", ignoreCase = true))
        assertFalse(result.subtitle.contains("Spoonacular", ignoreCase = true))
        assertFalse(result.description.contains("Image search result", ignoreCase = true))
    }

    @Test
    fun `mealdb external result uses Chinese visible labels`() {
        val result = TheMealDbMapper.toExternalResult(
            TheMealDbMeal(
                idMeal = "2",
                strMeal = "Beef Stew",
                strCategory = "Beef",
                strInstructions = "Cook the beef until tender.",
                strMealThumb = "https://example.com/beef.jpg"
            ),
            query = "炖牛肉"
        )

        assertEquals("炖牛肉", result.name)
        assertEquals("外部图片", result.source)
        assertEquals("图片参考", result.subtitle)
        assertEquals("网络菜品图片参考", result.description)
        assertFalse(result.source.contains("TheMealDB", ignoreCase = true))
        assertFalse(result.subtitle.contains("TheMealDB", ignoreCase = true))
        assertFalse(result.description.contains("Cook", ignoreCase = true))
    }
}
