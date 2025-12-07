package com.visioncoders.movieapp.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ReportService {
    @FormUrlEncoded
    @POST("submit_report.php")
    fun submitReport(
        @Field("reporter_id") reporterId: String,
        @Field("reporter_name") reporterName: String,
        @Field("reported_user_id") reportedUserId: String,
        @Field("reported_user_name") reportedUserName: String,
        @Field("reason") reason: String
    ): Call<UploadResponse>
}