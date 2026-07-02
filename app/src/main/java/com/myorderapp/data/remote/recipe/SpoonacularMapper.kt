package com.myorderapp.data.remote.recipe

import com.myorderapp.ui.search.ExternalDishImageResult

object SpoonacularMapper {

    fun toExternalResult(recipe: SpoonacularRecipe, query: String): ExternalDishImageResult {
        return ExternalDishImageResult(
            id = "spoon_${recipe.id}",
            name = query.ifBlank { "菜品图片" },
            imageUrl = recipe.image?.replace("-312x231.", "-636x393."),
            source = "外部图片",
            subtitle = "图片参考",
            category = "网络图片",
            description = "网络菜品图片参考"
        )
    }
}
