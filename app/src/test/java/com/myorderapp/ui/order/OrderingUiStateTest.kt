package com.myorderapp.ui.order

import com.myorderapp.domain.model.MenuItem
import com.myorderapp.domain.model.MenuCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderingUiStateTest {

    @Test
    fun `hot category shows all available items before search filters`() {
        val state = OrderingUiState(
            selectedCategory = ORDERING_HOT_CATEGORY_ID,
            searchQuery = "牛",
            menuItems = listOf(
                menuItem(id = "beef", category = "热销", name = "招牌牛肉饭"),
                menuItem(id = "tea", category = "热销", name = "橙香气泡茶"),
                menuItem(id = "noodle", category = "主食", name = "牛肉面")
            )
        )

        assertEquals(listOf("beef", "noodle"), state.visibleItems.map { it.id })
    }

    @Test
    fun `visible items are filtered by selected real category and search query`() {
        val state = OrderingUiState(
            selectedCategory = "主食",
            searchQuery = "牛",
            menuItems = listOf(
                menuItem(id = "beef", category = "热销", name = "招牌牛肉饭"),
                menuItem(id = "tea", category = "热销", name = "橙香气泡茶"),
                menuItem(id = "noodle", category = "主食", name = "牛肉面")
            )
        )

        assertEquals(listOf("noodle"), state.visibleItems.map { it.id })
    }

    @Test
    fun `real hot category replaces virtual hot category to avoid duplicate labels`() {
        val state = OrderingUiState(
            categories = listOf(
                menuCategory("热销"),
                menuCategory("主食")
            ),
            selectedCategory = "热销",
            menuItems = listOf(
                menuItem(id = "beef", category = "热销", name = "招牌牛肉饭"),
                menuItem(id = "noodle", category = "主食", name = "牛肉面")
            )
        )

        assertEquals(listOf("热销", "主食"), state.orderingCategories.map { it.name })
        assertEquals(listOf("beef", "noodle"), state.visibleItems.map { it.id })
    }

    private fun menuItem(
        id: String,
        category: String,
        name: String
    ): MenuItem = MenuItem(
        id = id,
        shopId = "single_shop",
        categoryId = category,
        name = name,
        subtitle = category,
        description = "本店自制菜品",
        imageUrl = "",
        price = 12.0
    )

    private fun menuCategory(name: String): MenuCategory = MenuCategory(
        id = name,
        shopId = "single_shop",
        name = name,
        sortOrder = 0
    )
}
