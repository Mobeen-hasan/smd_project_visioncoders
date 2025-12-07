package com.visioncoders.movieapp

import androidx.room.Entity
import androidx.room.PrimaryKey

// Table to store user actions (Queue)
@Entity(tableName = "sync_queue")
data class SyncRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "REVIEW" or "REPLY"
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Table to store reviews for offline viewing (Cache)
@Entity(tableName = "cached_reviews")
data class CachedReview(
    @PrimaryKey val id: String, // Firebase ID
    val movieId: Int,
    val username: String,
    val rating: Double,
    val text: String,
    val timestamp: Long
)