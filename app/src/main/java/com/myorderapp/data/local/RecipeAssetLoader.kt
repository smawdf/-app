package com.myorderapp.data.local

import android.content.Context
import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


data class RecipeJsonDto(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val zuofa: String = "",
    val ingredients: List<String> = emptyList(),
    val seasonings: List<String> = emptyList(),
    val texing: String = "",
    val tishi: String = "",
    val difficulty: Int = 1,
    val cookTimeMin: Int = 0
)


data class RecipeListDto(
    val recipes: List<RecipeJsonDto> = emptyList()
)

class RecipeAssetLoader(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun loadRecipes(): List<Dish> {
        val json = context.assets.open("recipes.json")
            .bufferedReader()
            .use { it.readText() }

        val adapter = moshi.adapter(RecipeListDto::class.java)
        val dto = adapter.fromJson(json) ?: return emptyList()

        return dto.recipes.map { it.toDishModel() }
    }

    private fun RecipeJsonDto.toDishModel(): Dish {
        return Dish(
            id = "builtin_$id",
            pairId = "",
            name = name,
            source = "builtin",
            externalId = id.toString(),
            externalSource = "juhe",
            category = category,
            imageUrl = null,
            cookSteps = parseCookSteps(zuofa, tishi),
            ingredients = ingredients + seasonings,
            difficulty = difficulty,
            cookTimeMin = cookTimeMin,
            whoLikes = emptyList(),
            rating = 0f,
            notes = texing,
            createdBy = "内置菜谱",
            createdAt = "",
            updatedAt = ""
        )
    }

    private fun parseCookSteps(zuofa: String, tishi: String): List<CookStep> {
        if (zuofa.isBlank()) return emptyList()

        val cleaned = zuofa
            .replace("\\n", "\n")
            .trim()

        val lines = cleaned.split("\n").filter { it.trim().isNotBlank() }

        // 如果换行后只有1行，按步骤标记分割（Juhe API 格式）
        val stepTexts = if (lines.size <= 1) {
            cleaned.split(Regex("(?=\\d+[.、．)）]\\s*)"))
                .filter { it.isNotBlank() }
        } else {
            lines
        }

        val steps = mutableListOf<CookStep>()
        for (text in stepTexts) {
            val trimmed = text.trim()
            if (trimmed.isBlank()) continue

            val match = Regex("^(\\d+)[.、．)）]\\s*(.+)").find(trimmed)
            if (match != null) {
                val stepNum = match.groupValues[1].toIntOrNull() ?: (steps.size + 1)
                var desc = match.groupValues[2].trim()
                desc = desc.trimEnd('；', ';')
                steps.add(CookStep(step = stepNum, description = desc))
            }
        }

        if (tishi.isNotBlank() && steps.isNotEmpty()) {
            val last = steps.last()
            steps[steps.size - 1] = last.copy(tip = tishi)
        }

        return steps
    }

    companion object {
        fun getDifficultyLabel(difficulty: Int): String = when (difficulty) {
            1 -> "新手"
            2 -> "简单"
            3 -> "中等"
            4 -> "困难"
            5 -> "大厨"
            else -> "未知"
        }
    }
}
