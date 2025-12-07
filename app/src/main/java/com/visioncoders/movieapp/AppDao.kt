package com.visioncoders.movieapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppDao {
    // --- Queue Methods ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToQueue(request: SyncRequest)

    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<SyncRequest>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteAction(id: Int)

    // --- Cache Methods ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheReview(review: CachedReview)

    @Query("SELECT * FROM cached_reviews WHERE movieId = :movieId")
    suspend fun getOfflineReviews(movieId: Int): List<CachedReview>
}