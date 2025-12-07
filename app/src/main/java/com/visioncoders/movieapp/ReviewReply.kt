package com.visioncoders.movieapp

data class ReviewReply(
    val id: String = "",
    val reviewId: String = "",
    val userId: String = "",
    val username: String = "",
    val userImage: String = "",
    val text: String = "",
    val timestamp: Long = 0
)