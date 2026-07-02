package com.myorderapp.domain.repository

import com.myorderapp.domain.model.Shop
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    fun getFeaturedShops(): Flow<List<Shop>>
    fun getNearbyShops(): Flow<List<Shop>>
    fun searchShops(query: String): Flow<List<Shop>>
    fun getShopById(shopId: String): Flow<Shop?>
}
