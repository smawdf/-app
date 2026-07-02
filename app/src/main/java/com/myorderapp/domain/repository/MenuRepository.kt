package com.myorderapp.domain.repository

import com.myorderapp.domain.model.MenuCategory
import com.myorderapp.domain.model.MenuItem
import com.myorderapp.ui.search.SearchableMenuItem
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun getMenuCategories(shopId: String): Flow<List<MenuCategory>>
    fun getMenuItems(shopId: String): Flow<List<MenuItem>>
    fun searchMenuItems(query: String): Flow<List<SearchableMenuItem>>
}
