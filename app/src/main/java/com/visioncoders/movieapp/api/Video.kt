package com.visioncoders.movieapp.api

data class VideoResponse(
    val results: List<Video>
)

data class Video(
    val key: String,
    val site: String,
    val type: String,
    val name: String
)