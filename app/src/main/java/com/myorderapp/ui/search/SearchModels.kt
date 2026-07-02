package com.myorderapp.ui.search

import com.myorderapp.domain.model.MenuItem

data class SearchableMenuItem(
    val shopId: String,
    val shopName: String,
    val menuItem: MenuItem
)

data class ExternalDishImageResult(
    val id: String,
    val name: String,
    val subtitle: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String? = null,
    val source: String
)
