package com.visioncoders.movieapp.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Response Model
data class UploadResponse(
    val status: String,
    val url: String?,
    val message: String
)

interface FileUploadService {
    @Multipart
    @POST("upload_image.php")
    fun uploadProfileImage(
        @Part image: MultipartBody.Part
    ): Call<UploadResponse>
}