package com.myorderapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myorderapp.data.local.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query(
        """
        SELECT * FROM wishlist_items
        WHERE (:status IS NULL OR :status = 'all' OR status = :status)
        ORDER BY createdAt DESC
        """
    )
    fun getWishlistItems(status: String? = null): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistEntity)

    @Query("UPDATE wishlist_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM wishlist_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
