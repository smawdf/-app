package com.myorderapp.data.remote.recipe

import com.myorderapp.domain.model.CookStep
import com.myorderapp.domain.model.Dish

object JisuRecipeMapper {

    fun JisuRecipeItem.toDishModel(sourceType: String = "external"): Dish {
        return Dish(
            id = "jisu_$id",
            pairId = "",
            name = name.trim(),
            source = sourceType,
            externalId = id,
            externalSource = "jisuapi",
            category = resolveCategory(classid, tag),
            imageUrl = pic.ifBlank { null },
            cookSteps = process.mapIndexed { idx, step ->
                CookStep(
                    step = idx + 1,
                    description = step.pcontent.trim(),
                    tip = null,
                    imageUrl = step.pic.ifBlank { null }
                )
            },
            ingredients = material.mapNotNull { mat ->
                val name = mat.mname.trim()
                if (name.isBlank()) {
                    null
                } else {
                    val qty = mat.amount.trim()
                    if (qty.isNotBlank()) "$name $qty" else name
                }
            },
            difficulty = estimateDifficulty(process.size),
            cookTimeMin = parseCookTime(cookingtime, preparetime, process.size),
            whoLikes = emptyList(),
            rating = 0f,
            notes = content.trim().replace(Regex("<br\\s*/?>"), "\n").ifBlank { "" },
            createdBy = "极速数据",
            createdAt = "",
            updatedAt = ""
        )
    }

    fun toBuiltinDish(item: JisuRecipeItem): Dish {
        return item.toDishModel("builtin").copy(
            id = "jisu_builtin_${item.id}"
        )
    }

    private fun resolveCategory(classid: String, tag: String): String {
        val firstTag = tag.split(",", "，").firstOrNull()?.trim()
        if (!firstTag.isNullOrBlank()) return firstTag
        return when (classid) {
            "1" -> "家常菜"
            "2" -> "素菜"
            "3" -> "荤菜"
            "4" -> "汤类"
            "5" -> "主食"
            "6" -> "小吃"
            "7" -> "烘焙"
            "8" -> "饮品"
            else -> "其他"
        }
    }

    private fun parseCookTime(cookingTime: String, prepareTime: String, stepCount: Int): Int {
        fun extractMinutes(text: String): Int? {
            val range = Regex("(\\d+)\\s*-\\s*(\\d+)\\s*分钟").find(text)
            if (range != null) {
                val lo = range.groupValues[1].toIntOrNull() ?: return null
                val hi = range.groupValues[2].toIntOrNull() ?: return null
                return (lo + hi) / 2
            }
            return Regex("(\\d+)\\s*分钟").find(text)?.groupValues?.get(1)?.toIntOrNull()
        }

        val total = (extractMinutes(cookingTime) ?: 0) + (extractMinutes(prepareTime) ?: 0)
        if (total > 0) return total
        return stepCount.coerceAtLeast(1) * 5 + 10
    }

    private fun estimateDifficulty(stepCount: Int): Int {
        return when {
            stepCount <= 3 -> 1
            stepCount <= 5 -> 2
            stepCount <= 7 -> 3
            stepCount <= 10 -> 4
            else -> 5
        }
    }
}
