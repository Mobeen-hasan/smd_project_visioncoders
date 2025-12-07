package com.visioncoders.movieapp

data class AppNotification(
    val id: String = "",
    val type: String = "",
    val fromId: String = "",
    val fromName: String = "",
    val fromImage: String = "",
    val toId: String = "",
    val targetId: String = "",
    val message: String = "",
    val timestamp: Long = 0
)