package com.visioncoders.movieapp

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class WatchlistItem(
    @DocumentId
    val id: String = "",
    val movieId: Int = 0,
    val title: String = "",
    val posterPath: String = "",
    val rating: Double = 0.0,
    val mediaType: String = "movie",
    val userId: String = "",
    val timestamp: Long = 0,

    @get:PropertyName("isLiked")
    @set:PropertyName("isLiked")
    var isLiked: Boolean = false,

    @get:PropertyName("isWatched")
    @set:PropertyName("isWatched")
    var isWatched: Boolean = false
)