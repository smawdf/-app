package com.myorderapp.data.repository

import com.myorderapp.domain.model.Dish

object DishMergePolicy {
    fun merge(
        local: List<Dish>,
        cloud: List<Dish>,
        includeCloud: Boolean
    ): List<Dish> {
        val merged = mutableListOf<Dish>()
        val seen = mutableSetOf<String>()

        if (includeCloud) {
            cloud.forEach { dish ->
                if (seen.add(dish.mergeKey())) merged.add(dish)
            }
        }

        local.forEach { dish ->
            if (seen.add(dish.mergeKey())) merged.add(dish)
        }

        return merged
    }

    private fun Dish.mergeKey(): String {
        val normalizedName = name.trim().lowercase()
        return normalizedName.ifBlank { "id:${id.trim().lowercase()}" }
    }
}
