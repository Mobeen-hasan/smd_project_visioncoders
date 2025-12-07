package com.visioncoders.movieapp.api

import com.google.gson.annotations.SerializedName

data class MovieResponse(
    val results: List<Movie>
)

data class Movie(
    val id: Int,
    // The API sends "title" for movies and "name" for TV. We capture both.
    @SerializedName("title") private val _title: String?,
    @SerializedName("name") private val _name: String?,

    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val rating: Double,
    @SerializedName("media_type") val mediaType: String? // "movie" or "tv"
) {
    // This custom property makes sure your Adapter always gets a valid title
    val title: String
        get() = _title ?: _name ?: "Unknown"
}

data class MovieDetails(
    val id: Int,
    // Movies have 'title', TV shows have 'name'
    @SerializedName("title") val _title: String?,
    @SerializedName("name") val _name: String?,

    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val rating: Double,
    @SerializedName("vote_count") val voteCount: Int,
    val overview: String,

    // Movies have 'release_date', TV shows have 'first_air_date'
    @SerializedName("release_date") val _releaseDate: String?,
    @SerializedName("first_air_date") val _firstAirDate: String?,

    // Movies have 'runtime' (Int), TV shows have 'episode_run_time' (List<Int>)
    val runtime: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,

    val genres: List<Genre>
) {
    // Helper properties to get the right data regardless of type
    val title: String get() = _title ?: _name ?: "Unknown"
    val releaseDate: String get() = _releaseDate ?: _firstAirDate ?: ""
    val duration: Int get() = runtime ?: episodeRunTime?.firstOrNull() ?: 0
}

// --- 2. Add Cast Models ---
data class CreditsResponse(
    val cast: List<Cast>
)

data class Cast(
    val id: Int,
    val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?
)

// --- 3. Add Review Models ---
data class ReviewsResponse(
    val results: List<Review>
)

data class Review(
    val id: String,
    val author: String,
    val content: String,
    @SerializedName("author_details") val authorDetails: AuthorDetails?
)

data class AuthorDetails(
    @SerializedName("rating") val rating: Double?,
    @SerializedName("avatar_path") val avatarPath: String?
)

data class Genre(val id: Int, val name: String)