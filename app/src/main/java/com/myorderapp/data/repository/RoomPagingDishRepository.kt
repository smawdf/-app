package com.myorderapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.myorderapp.data.local.EntityMapper.toDomain
import com.myorderapp.data.local.dao.DishDao
import com.myorderapp.domain.model.Dish
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPagingDishRepository(
    private val dishDao: DishDao
) {
    fun getDishesPaged(
        query: String = "",
        source: String? = null
    ): Flow<PagingData<Dish>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                dishDao.pagingSource(
                    pairId = "%",
                    query = query.trim(),
                    source = source
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }
}
