package com.visioncoders.movieapp

import com.visioncoders.movieapp.api.NotificationApi
import com.visioncoders.movieapp.api.NotificationRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NotificationHelper {

    private const val BASE_URL = "http://192.168.0.102:3000/"

    private val api: NotificationApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotificationApi::class.java)
    }

    fun sendPush(targetUserId: String, title: String, body: String) {
        val request = NotificationRequest(targetUserId, title, body)

        api.sendNotification(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // Log success if needed
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {

            }
        })
    }
}