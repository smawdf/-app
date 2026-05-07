package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish

object JuheRecipeMapper {

    fun JuheRecipeItem.toDishModel(sourceType: String = "external"): Dish {
        return Dish(
            id = "juhe_$id",
            pairId = "",
            name = cpName.trim(),
            source = sourceType,
            externalId = id.toString(),
            externalSource = "juhe",
            category = cleanTypeName(typeName),
            imageUrl = null,
            cookSteps = parseZuofaToSteps(zuofa, tishi),
            ingredients = parseCommaSeparated(yuanliao),
            difficulty = estimateDifficulty(zuofa),
            cookTimeMin = estimateCookTime(zuofa),
            whoLikes = emptyList(),
            rating = 0f,
            notes = texing.ifBlank { "" },
            createdBy = "聚合数据",
            createdAt = "",
            updatedAt = ""
        )
    }

    fun toBuiltinDish(item: JuheRecipeItem): Dish {
        return item.toDishModel("builtin").copy(
            id = "builtin_${item.id}"
        )
    }

    private fun parseZuofaToSteps(zuofa: String, tishi: String): List<CookStep> {
        if (zuofa.isBlank()) return emptyList()

        val cleaned = zuofa
            .replace("\\n", "\n")
            .replace("x", "")
            .trim()

        // 先尝试按换行分割
        val lines = cleaned.split("\n").filter { it.trim().isNotBlank() }

        // 如果换行后只有1行（Juhe API 格式："1.xxx；2.xxx；3.xxx"），按步骤标记分割
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

    private fun parseCommaSeparated(raw: String): List<String> {
        return raw.split("、", "，", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    // 直接使用 API 返回的 type_name 作为分类，仅做简单清理
    private fun cleanTypeName(typeName: String): String {
        if (typeName.isBlank()) return "其他"
        return typeName
            .replace("类$".toRegex(), "")
            .replace("菜$".toRegex(), "")
            .trim()
            .ifBlank { typeName }
    }

    private fun estimateDifficulty(zuofa: String): Int {
        val stepCount = zuofa.split(Regex("\\d+[.、．)]")).size - 1
        return when {
            stepCount <= 3 -> 1
            stepCount <= 5 -> 2
            stepCount <= 7 -> 3
            stepCount <= 10 -> 4
            else -> 5
        }
    }

    private fun estimateCookTime(zuofa: String): Int {
        val minutes = Regex("(\\d+)\\s*分钟").find(zuofa)?.groupValues?.get(1)?.toIntOrNull()
        if (minutes != null) return minutes
        val hours = Regex("(\\d+)\\s*小时").find(zuofa)?.groupValues?.get(1)?.toIntOrNull()
        if (hours != null) return hours * 60
        val stepCount = zuofa.split(Regex("\\d+[.、．)]")).size - 1
        return stepCount * 5 + 10
    }
}
