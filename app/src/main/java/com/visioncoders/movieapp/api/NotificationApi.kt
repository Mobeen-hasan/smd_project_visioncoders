package com.visioncoders.movieapp.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Data model for the JSON body sent to Node.js
data class NotificationRequest(
    val targetUserId: String,
    val title: String,
    val body: String
)

// Retrofit interface to call your Node.js server
interface NotificationApi {
    @POST("send-notification")
    fun sendNotification(@Body request: NotificationRequest): Call<Void>
}