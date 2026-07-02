package com.myorderapp.ui.order

import com.myorderapp.domain.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderingUiStateTest {

    @Test
    fun `visible items are filtered by selected category and search query`() {
        val state = OrderingUiState(
            selectedCategory = "热销",
            searchQuery = "牛",
            menuItems = listOf(
                menuItem(id = "beef", category = "热销", name = "招牌牛肉饭"),
                menuItem(id = "tea", category = "热销", name = "橙香气泡茶"),
                menuItem(id = "noodle", category = "主食", name = "牛肉面")
            )
        )

        assertEquals(listOf("beef"), state.visibleItems.map { it.id })
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
}
