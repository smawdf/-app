package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish

object TheMealDBMapper {

    fun fromMeal(meal: TheMealDBMeal): Dish = with(meal) {
        val steps = parseInstructions(strInstructions)
        Dish(
            id = "tdb_$idMeal",
            pairId = "",
            name = FoodTranslator.toChinese(strMeal.trim()),
            source = "external",
            externalId = idMeal,
            externalSource = "themealdb",
            category = FoodTranslator.toChinese(strCategory?.trim()?.ifBlank { "其他" } ?: "其他"),
            imageUrl = strMealThumb?.let { "$it/preview" },
            cookSteps = steps,
            ingredients = collectIngredients(),
            difficulty = estimateDifficulty(steps.size),
            cookTimeMin = 30,
            whoLikes = emptyList(),
            rating = 0f,
            notes = FoodTranslator.toChinese(strArea?.trim().orEmpty()),
            createdBy = "TheMealDB",
            createdAt = "",
            updatedAt = ""
        )
    }

    private fun TheMealDBMeal.collectIngredients(): List<String> {
        val pairs = listOf(
            strIngredient1 to strMeasure1,
            strIngredient2 to strMeasure2,
            strIngredient3 to strMeasure3,
            strIngredient4 to strMeasure4,
            strIngredient5 to strMeasure5,
            strIngredient6 to strMeasure6,
            strIngredient7 to strMeasure7,
            strIngredient8 to strMeasure8,
            strIngredient9 to strMeasure9,
            strIngredient10 to strMeasure10,
            strIngredient11 to strMeasure11,
            strIngredient12 to strMeasure12,
            strIngredient13 to strMeasure13,
            strIngredient14 to strMeasure14,
            strIngredient15 to strMeasure15,
            strIngredient16 to strMeasure16,
            strIngredient17 to strMeasure17,
            strIngredient18 to strMeasure18,
            strIngredient19 to strMeasure19,
            strIngredient20 to strMeasure20
        )
        return pairs
            .filter { (ingredient, _) -> !ingredient.isNullOrBlank() }
            .map { (ingredient, measure) ->
                val ing = ingredient?.trim().orEmpty()
                val msr = measure?.trim().orEmpty()
                if (msr.isNotBlank()) "$msr $ing" else ing
            }
            .map { FoodTranslator.toChinese(it) }
    }

    private fun parseInstructions(instructions: String?): List<CookStep> {
        if (instructions.isNullOrBlank()) return emptyList()
        val cleaned = instructions
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("\\r", "")
            .trim()
        val lines = cleaned.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return lines.mapIndexed { index, line ->
            CookStep(
                step = index + 1,
                description = line.trimEnd('\r')
            )
        }
    }

    private fun estimateDifficulty(stepCount: Int): Int {
        return when {
            stepCount <= 3 -> 1
            stepCount <= 5 -> 2
            stepCount <= 8 -> 3
            stepCount <= 12 -> 4
            else -> 5
        }
    }
}
