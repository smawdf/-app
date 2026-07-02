package com.myorderapp.data.remote.recipe

import com.myorderapp.ui.search.ExternalDishImageResult

object TheMealDbMapper {

    fun toExternalResult(meal: TheMealDbMeal, query: String): ExternalDishImageResult {
        return ExternalDishImageResult(
            id = "mealdb_${meal.idMeal}",
            name = query.ifBlank { "菜品图片" },
            imageUrl = meal.strMealThumb,
            source = "外部图片",
            subtitle = "图片参考",
            category = "网络图片",
            description = "网络菜品图片参考"
        )
    }
}
