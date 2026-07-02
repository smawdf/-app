package com.myorderapp.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleMerchantCatalogRepositoryTest {

    @Test
    fun `featured shop feed exposes image rich merchants`() = runBlocking {
        val repository = SampleShopRepository()

        val featured = repository.getFeaturedShops().first()

        assertTrue(featured.size >= 3)
        assertTrue(featured.all { it.coverUrl.isNotBlank() })
        assertTrue(featured.all { it.logoUrl.isNotBlank() })
        assertTrue(featured.all { it.rating >= 4.0 })
        assertTrue(featured.all { it.avgDeliveryMinutes > 0 })
    }

    @Test
    fun `menu repository returns categories and items for a shop`() = runBlocking {
        val shopRepository = SampleShopRepository()
        val menuRepository = SampleMenuRepository()
        val shopId = shopRepository.getFeaturedShops().first().first().id

        val categories = menuRepository.getMenuCategories(shopId).first()
        val items = menuRepository.getMenuItems(shopId).first()

        assertFalse(categories.isEmpty())
        assertFalse(items.isEmpty())
        assertTrue(items.all { it.shopId == shopId })
        assertTrue(items.all { it.imageUrl.isNotBlank() })
        assertTrue(items.any { item -> categories.any { it.id == item.categoryId } })
    }
}
