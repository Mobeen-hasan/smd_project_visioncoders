package com.visioncoders.movieapp.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("trending/movie/week")
    fun getTrendingMovies(@Query("api_key") apiKey: String): Call<MovieResponse>

    @GET("movie/{movie_id}")
    fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Call<MovieDetails>

    @GET("movie/{movie_id}/credits")
    fun getMovieCast(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Call<CreditsResponse>

    @GET("movie/{movie_id}/reviews")
    fun getMovieReviews(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Call<ReviewsResponse>

    @GET("movie/{movie_id}/similar")
    fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Call<MovieResponse>


    @GET("search/multi")
    fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String
    ): Call<MovieResponse>


    @GET("tv/{tv_id}")
    fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): Call<MovieDetails>

    @GET("tv/{tv_id}/credits")
    fun getTvCast(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): Call<CreditsResponse>

    @GET("tv/{tv_id}/similar")
    fun getTvSimilar(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): Call<MovieResponse>


    @GET("tv/{tv_id}/reviews")
    fun getTvReviews(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): Call<ReviewsResponse>



    //VIDEO

    @GET("movie/{movie_id}/videos")
    fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Call<VideoResponse>

    @GET("tv/{tv_id}/videos")
    fun getTvVideos(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): Call<VideoResponse>

}