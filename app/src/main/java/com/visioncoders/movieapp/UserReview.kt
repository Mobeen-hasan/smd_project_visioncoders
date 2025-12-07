package com.visioncoders.movieapp

data class UserReview(
    var id: String = "",
    val movieId: Int = 0,
    val userId: String = "",
    val username: String = "",
    val userImage: String = "",
    val rating: Double = 0.0,
    val reviewText: String = "",
    val timestamp: Long = 0,
    val movieTitle: String = "",
    val posterPath: String = "",
    val mediaType: String = "movie",
    val likedBy: List<String> = emptyList()
)