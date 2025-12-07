package com.visioncoders.movieapp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = "",
    val handle: String = "",
    val rating: Double = 0.0, // Average rating given by user
    val reviewCount: Int = 0  // Total number of reviews
)