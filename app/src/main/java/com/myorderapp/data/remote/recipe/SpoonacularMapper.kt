package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish

object SpoonacularMapper {

    fun fromSearchResult(recipe: SpoonacularRecipe): Dish = with(recipe) {
        Dish(
            id = "sp_$id",
            pairId = "",
            name = title.trim(),
            source = "external",
            externalId = id.toString(),
            externalSource = "spoonacular",
            category = mapCategory(cuisines, dishTypes),
            imageUrl = mapImageUrl(image),
            cookSteps = parseSteps(analyzedInstructions),
            ingredients = extendedIngredients.map { it.original.ifBlank { it.name } },
            difficulty = estimateDifficulty(readyInMinutes, analyzedInstructions),
            cookTimeMin = readyInMinutes,
            whoLikes = emptyList(),
            rating = (spoonacularScore / 20f).coerceIn(0f, 5f),
            notes = cleanSummary(summary),
            createdBy = "Spoonacular",
            createdAt = "",
            updatedAt = ""
        )
    }

    fun fromDetail(detail: SpoonacularRecipeDetail): Dish = with(detail) {
        Dish(
            id = "sp_$id",
            pairId = "",
            name = title.trim(),
            source = "external",
            externalId = id.toString(),
            externalSource = "spoonacular",
            category = mapCategory(cuisines, dishTypes),
            imageUrl = mapImageUrl(image),
            cookSteps = parseSteps(analyzedInstructions),
            ingredients = extendedIngredients.map { it.original.ifBlank { it.name } },
            difficulty = estimateDifficulty(readyInMinutes, analyzedInstructions),
            cookTimeMin = readyInMinutes,
            whoLikes = emptyList(),
            rating = 0f,
            notes = cleanSummary(summary),
            createdBy = "Spoonacular",
            createdAt = "",
            updatedAt = ""
        )
    }

    // 将默认 312x231 图片替换为更大尺寸 636x393
    private fun mapImageUrl(image: String?): String? {
        if (image.isNullOrBlank()) return null
        return image.replace("-312x231.", "-636x393.")
    }

    private fun parseSteps(instructions: List<AnalyzedInstruction>): List<CookStep> {
        return instructions.flatMap { instruction ->
            instruction.steps.map { step ->
                CookStep(
                    step = step.number,
                    description = step.step.cleanHtml(),
                    tip = null,
                    imageUrl = step.equipment.firstOrNull()?.image
                )
            }
        }
    }

    // 直接使用 Spoonacular 返回的 cuisine/dishType 作为分类
    private fun mapCategory(cuisines: List<String>, dishTypes: List<String>): String {
        val cuisine = cuisines.firstOrNull()
        if (!cuisine.isNullOrBlank()) return cuisine.replaceFirstChar { it.uppercase() }
        val dishType = dishTypes.firstOrNull()
        if (!dishType.isNullOrBlank()) return dishType.replaceFirstChar { it.uppercase() }
        return "其他"
    }

    private fun estimateDifficulty(minutes: Int, instructions: List<AnalyzedInstruction>): Int {
        val stepCount = instructions.sumOf { it.steps.size }
        return when {
            stepCount <= 3 && minutes <= 20 -> 1
            stepCount <= 5 && minutes <= 30 -> 2
            stepCount <= 8 && minutes <= 45 -> 3
            stepCount <= 12 && minutes <= 60 -> 4
            else -> 5
        }
    }

    private fun cleanSummary(summary: String?): String {
        if (summary.isNullOrBlank()) return ""
        return summary
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(200)
    }

    private fun String.cleanHtml(): String {
        return this.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .trim()
    }
}
