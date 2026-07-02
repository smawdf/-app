package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish

object TianRecipeMapper {

    fun TianRecipeItem.toDishModel(sourceType: String = "external"): Dish {
        return Dish(
            id = "tian_$id",
            pairId = "",
            name = cpName.trim(),
            source = sourceType,
            externalId = id.toString(),
            externalSource = "tianapi",
            category = cleanTypeName(typeName),
            imageUrl = null,
            cookSteps = parseZuofaToSteps(zuofa, tishi),
            ingredients = parseIngredients(yuanliao, tiaoliao),
            difficulty = estimateDifficulty(zuofa),
            cookTimeMin = estimateCookTime(zuofa),
            whoLikes = emptyList(),
            rating = 0f,
            notes = texing.ifBlank { tishi },
            createdBy = "天行数据",
            createdAt = "",
            updatedAt = ""
        )
    }

    fun toBuiltinDish(item: TianRecipeItem): Dish {
        return item.toDishModel("builtin").copy(
            id = "tian_builtin_${item.id}"
        )
    }

    private fun parseZuofaToSteps(zuofa: String, tishi: String): List<CookStep> {
        if (zuofa.isBlank()) return emptyList()

        val cleaned = zuofa
            .replace("\\n", "\n")
            .replace("x", "")
            .trim()

        val lines = cleaned.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val stepTexts = if (lines.size <= 1) {
            cleaned.split(Regex("(?=\\d+[.、．)）]\\s*)"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        } else {
            lines
        }

        val steps = stepTexts.mapIndexedNotNull { index, text ->
            val match = Regex("^(\\d+)[.、．)）]\\s*(.+)").find(text)
            val description = match?.groupValues?.get(2)?.trim() ?: text
            if (description.isBlank()) {
                null
            } else {
                CookStep(
                    step = match?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1),
                    description = description.trimEnd('；', ';')
                )
            }
        }.toMutableList()

        if (tishi.isNotBlank() && steps.isNotEmpty()) {
            steps[steps.lastIndex] = steps.last().copy(tip = tishi)
        }

        return steps
    }

    private fun parseIngredients(yuanliao: String, tiaoliao: String): List<String> {
        return listOf(yuanliao, tiaoliao)
            .flatMap { parseSeparatedValues(it) }
            .distinct()
    }

    private fun parseSeparatedValues(raw: String): List<String> {
        return raw.split("、", "，", ",", ";", "；")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun cleanTypeName(typeName: String): String {
        if (typeName.isBlank()) return "其他"
        return typeName
            .replace("类$".toRegex(), "")
            .replace("菜$".toRegex(), "")
            .trim()
            .ifBlank { typeName }
    }

    private fun estimateDifficulty(zuofa: String): Int {
        val stepCount = countSteps(zuofa)
        return when {
            stepCount <= 3 -> 1
            stepCount <= 5 -> 2
            stepCount <= 7 -> 3
            stepCount <= 10 -> 4
            else -> 5
        }
    }

    private fun estimateCookTime(zuofa: String): Int {
        Regex("(\\d+)\\s*分钟").find(zuofa)?.groupValues?.get(1)?.toIntOrNull()?.let {
            return it
        }
        Regex("(\\d+)\\s*小时").find(zuofa)?.groupValues?.get(1)?.toIntOrNull()?.let {
            return it * 60
        }
        return countSteps(zuofa) * 5 + 10
    }

    private fun countSteps(zuofa: String): Int {
        return Regex("\\d+[.、．)）]").findAll(zuofa).count().coerceAtLeast(1)
    }
}
