package com.visioncoders.movieapp

data class FriendRequest(
    val id: String = "",
    val fromId: String = "",
    val fromName: String = "",
    val fromImage: String = "",
    val toId: String = "",
    val status: String = "",
    val timestamp: Long = 0
)