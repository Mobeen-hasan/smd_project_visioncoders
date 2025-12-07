package com.visioncoders.movieapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.visioncoders.movieapp.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MovieDetailsActivity : AppCompatActivity() {

    // --- UI Variables ---
    private lateinit var posterImg: ImageView
    private lateinit var titleTxt: TextView
    private lateinit var ratingTxt: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var summaryRatingTxt: TextView
    private lateinit var ratingCountTxt: TextView
    private lateinit var infoTxt: TextView
    private lateinit var synopsisTxt: TextView

    // Buttons
    private lateinit var writeReviewBtn: AppCompatButton
    private lateinit var addToWatchlistBtn: AppCompatButton
    private lateinit var btnTrailer: AppCompatButton

    // --- RecyclerViews ---
    private lateinit var castRecycler: RecyclerView
    private lateinit var reviewsRecycler: RecyclerView
    private lateinit var similarRecycler: RecyclerView

    // --- Data & API ---
    private val API_KEY = "b372be1db50166bf650c36ada4c6d41b"
    private lateinit var api: TmdbApi

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var currentMovieTitle: String = ""
    private var currentMoviePoster: String = ""
    private var trailerKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)

        // 1. Initialize API Client
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(TmdbApi::class.java)

        // 2. Initialize Views
        posterImg = findViewById(R.id.moviePosterImageView)
        titleTxt = findViewById(R.id.movieTitleTextView)
        ratingTxt = findViewById(R.id.movieRatingValue)
        ratingBar = findViewById(R.id.movieRatingBar)
        summaryRatingTxt = findViewById(R.id.summaryRatingValue)
        ratingCountTxt = findViewById(R.id.ratingCountTextView)
        infoTxt = findViewById(R.id.movieInfoTextView)
        synopsisTxt = findViewById(R.id.movieSynopsisTextView)

        writeReviewBtn = findViewById(R.id.writeReviewButton)
        addToWatchlistBtn = findViewById(R.id.watchlistButton)
        btnTrailer = findViewById(R.id.btnTrailer)

        // 3. Initialize RecyclerViews
        castRecycler = findViewById(R.id.castRecyclerView)
        castRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        reviewsRecycler = findViewById(R.id.reviewsRecyclerView)
        reviewsRecycler.layoutManager = LinearLayoutManager(this)

        similarRecycler = findViewById(R.id.similarMoviesRecyclerView)
        similarRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 4. Handle Back Button
        findViewById<ImageView>(R.id.backIcon).setOnClickListener { finish() }

        // 5. Get Data from Intent
        val movieId = intent.getIntExtra("MOVIE_ID", -1)
        val mediaType = intent.getStringExtra("MEDIA_TYPE") ?: "movie"

        if (movieId != -1) {
            checkWatchlistStatus(movieId)
            fetchTrailer(movieId, mediaType)

            if (mediaType == "tv") {
                fetchTvDetails(movieId)
                fetchTvCast(movieId)
                fetchFirebaseReviews(movieId)
                fetchTvSimilar(movieId)
            } else {
                fetchDetails(movieId)
                fetchCast(movieId)
                fetchFirebaseReviews(movieId)
                fetchSimilar(movieId)
            }
        } else {
            Toast.makeText(this, "Error: Content ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 6. Click Listeners
        writeReviewBtn.setOnClickListener {
            val intent = Intent(this, Page7Activity::class.java)
            intent.putExtra("MOVIE_ID", movieId)
            intent.putExtra("MOVIE_TITLE", currentMovieTitle)
            intent.putExtra("MOVIE_POSTER", currentMoviePoster)
            intent.putExtra("MOVIE_RATING", summaryRatingTxt.text.toString())
            intent.putExtra("MOVIE_INFO", infoTxt.text.toString())
            intent.putExtra("MEDIA_TYPE", mediaType)
            startActivity(intent)
        }

        addToWatchlistBtn.setOnClickListener {
            addToWatchlist(movieId, mediaType)
        }

        // TRAILER BUTTON: Show Dialog
        btnTrailer.setOnClickListener {
            if (trailerKey != null) {
                showTrailerDialog(trailerKey!!)
            } else {
                Toast.makeText(this, "Trailer not available", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.tvSeeAllReviews).setOnClickListener {
            val intent = Intent(this, FragmentMovieReviewsActivity::class.java)
            intent.putExtra("MOVIE_ID", movieId)
            intent.putExtra("MOVIE_TITLE", currentMovieTitle)
            intent.putExtra("MOVIE_POSTER", currentMoviePoster)
            intent.putExtra("MOVIE_RATING", summaryRatingTxt.text.toString())
            intent.putExtra("MOVIE_INFO", infoTxt.text.toString())
            intent.putExtra("RATING_COUNT", ratingCountTxt.text.toString())
            startActivity(intent)
        }
    }

    private fun fetchTrailer(id: Int, mediaType: String) {

        val call = if (mediaType == "tv") {
            api.getTvVideos(id, API_KEY)
        } else {
            api.getMovieVideos(id, API_KEY)
        }

        call.enqueue(object : Callback<VideoResponse> {
            override fun onResponse(call: Call<VideoResponse>, response: Response<VideoResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val videos = response.body()!!.results

                    val trailer = videos.find { it.site == "YouTube" && it.type == "Trailer" }
                        ?: videos.find { it.site == "YouTube" }

                    trailerKey = trailer?.key

                    if (trailerKey == null) {
                        btnTrailer.alpha = 0.5f
                        btnTrailer.isEnabled = false
                        btnTrailer.text = "No Trailer"
                    }
                }
            }
            override fun onFailure(call: Call<VideoResponse>, t: Throwable) {}
        })
    }

    private fun showTrailerDialog(videoId: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setContentView(R.layout.dialog_trailer)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val playerView = dialog.findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseTrailer)

        lifecycle.addObserver(playerView)

        playerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.cueVideo(videoId, 0f)
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                super.onError(youTubePlayer, error)

                dialog.dismiss()
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@MovieDetailsActivity, "Could not open video", Toast.LENGTH_SHORT).show()
                }
            }
        })

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            playerView.release()

            lifecycle.removeObserver(playerView)
        }
        dialog.show()
    }


    private fun addToWatchlist(movieId: Int, mediaType: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("watchlist")
            .whereEqualTo("userId", user.uid)
            .whereEqualTo("movieId", movieId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(this, "Already in Watchlist", Toast.LENGTH_SHORT).show()
                    updateWatchlistButton(true)
                } else {
                    val item = hashMapOf(
                        "movieId" to movieId,
                        "title" to currentMovieTitle,
                        "posterPath" to currentMoviePoster,
                        "rating" to ratingBar.rating.toDouble() * 2,
                        "mediaType" to mediaType,
                        "userId" to user.uid,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("watchlist").add(item)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Added to Watchlist!", Toast.LENGTH_SHORT).show()
                            updateWatchlistButton(true)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun checkWatchlistStatus(movieId: Int) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("watchlist")
            .whereEqualTo("userId", userId)
            .whereEqualTo("movieId", movieId)
            .get()
            .addOnSuccessListener { documents ->
                updateWatchlistButton(!documents.isEmpty)
            }
    }

    private fun updateWatchlistButton(isAdded: Boolean) {
        if (isAdded) {
            addToWatchlistBtn.text = "Added"
            addToWatchlistBtn.alpha = 0.5f
            addToWatchlistBtn.isEnabled = false
        } else {
            addToWatchlistBtn.text = "Watchlist"
            addToWatchlistBtn.alpha = 1.0f
            addToWatchlistBtn.isEnabled = true
        }
    }


    private fun fetchFirebaseReviews(movieId: Int) {
        db.collection("reviews")
            .whereEqualTo("movieId", movieId)
            .get()
            .addOnSuccessListener { documents ->
                val reviewsList = mutableListOf<UserReview>()
                for (document in documents) {
                    val review = document.toObject(UserReview::class.java)
                    review.id = document.id
                    reviewsList.add(review)
                }
                val limitedList = reviewsList.take(2)
                reviewsRecycler.adapter = SocialReviewAdapter(limitedList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchDetails(id: Int) {
        api.getMovieDetails(id, API_KEY).enqueue(object : Callback<MovieDetails> {
            override fun onResponse(call: Call<MovieDetails>, response: Response<MovieDetails>) {
                if (isDestroyed || isFinishing) return
                if (response.isSuccessful && response.body() != null) {
                    val movie = response.body()!!
                    currentMovieTitle = movie.title
                    currentMoviePoster = "https://image.tmdb.org/t/p/w500${movie.posterPath}"

                    titleTxt.text = movie.title
                    synopsisTxt.text = movie.overview
                    ratingCountTxt.text = "Based on ${movie.voteCount} ratings"
                    val ratingString = String.format("%.1f", movie.rating)
                    ratingTxt.text = ratingString
                    summaryRatingTxt.text = ratingString
                    ratingBar.rating = (movie.rating / 2).toFloat()

                    val year = if (movie.releaseDate.length >= 4) movie.releaseDate.substring(0, 4) else "N/A"
                    val genres = movie.genres.take(2).joinToString(", ") { it.name }
                    val hours = (movie.runtime ?: 0) / 60
                    val minutes = (movie.runtime ?: 0) % 60
                    infoTxt.text = "$year • $genres • ${hours}h ${minutes}m"

                    Glide.with(this@MovieDetailsActivity).load(currentMoviePoster).into(posterImg)
                }
            }
            override fun onFailure(call: Call<MovieDetails>, t: Throwable) {}
        })
    }

    private fun fetchCast(id: Int) {
        api.getMovieCast(id, API_KEY).enqueue(object : Callback<CreditsResponse> {
            override fun onResponse(call: Call<CreditsResponse>, response: Response<CreditsResponse>) {
                if (isDestroyed || isFinishing) return
                if (response.isSuccessful) {
                    val cast = response.body()?.cast ?: emptyList()
                    castRecycler.adapter = CastAdapter(cast)
                }
            }
            override fun onFailure(call: Call<CreditsResponse>, t: Throwable) {}
        })
    }

    private fun fetchSimilar(id: Int) {
        api.getSimilarMovies(id, API_KEY).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (isDestroyed || isFinishing) return
                if (response.isSuccessful) {
                    val movies = response.body()?.results ?: emptyList()
                    similarRecycler.adapter = TrendingAdapter(movies) { movie ->
                        val intent = Intent(this@MovieDetailsActivity, MovieDetailsActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("MEDIA_TYPE", "movie")
                        startActivity(intent)
                    }
                }
            }
            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {}
        })
    }

    private fun fetchTvDetails(id: Int) {
        api.getTvDetails(id, API_KEY).enqueue(object : Callback<MovieDetails> {
            override fun onResponse(call: Call<MovieDetails>, response: Response<MovieDetails>) {
                if (isDestroyed || isFinishing) return
                if (response.isSuccessful && response.body() != null) {
                    val item = response.body()!!
                    currentMovieTitle = item.title
                    currentMoviePoster = "https://image.tmdb.org/t/p/w500${item.posterPath}"

                    titleTxt.text = item.title
                    synopsisTxt.text = item.overview
                    ratingCountTxt.text = "Based on ${item.voteCount} ratings"
                    val ratingString = String.format("%.1f", item.rating)
                    ratingTxt.text = ratingString
                    summaryRatingTxt.text = ratingString
                    ratingBar.rating = (item.rating / 2).toFloat()

                    val date = item.releaseDate
                    val year = if (date.length >= 4) date.substring(0, 4) else "N/A"
                    val genres = item.genres.take(2).joinToString(", ") { it.name }
                    val runtimeMins = item.duration
                    val hours = runtimeMins / 60
                    val minutes = runtimeMins % 60
                    infoTxt.text = "$year • $genres • ${hours}h ${minutes}m"

                    Glide.with(this@MovieDetailsActivity).load(currentMoviePoster).into(posterImg)
                }
            }
            override fun onFailure(call: Call<MovieDetails>, t: Throwable) {
                Toast.makeText(this@MovieDetailsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchTvCast(id: Int) {
        api.getTvCast(id, API_KEY).enqueue(object : Callback<CreditsResponse> {
            override fun onResponse(call: Call<CreditsResponse>, response: Response<CreditsResponse>) {
                if (isDestroyed || isFinishing) return
                if (response.isSuccessful) {
                    val cast = response.body()?.cast ?: emptyList()
                    castRecycler.adapter = CastAdapter(cast)
                }
            }
            override fun onFailure(call: Call<CreditsResponse>, t: Throwable) {}
        })
    }

    private fun fetchTvSimilar(id: Int) {
        api.getTvSimilar(id, API_KEY).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (isDestroyed || isFinishing) return
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    similarRecycler.adapter = TrendingAdapter(results) { item ->
                        val intent = Intent(this@MovieDetailsActivity, MovieDetailsActivity::class.java)
                        intent.putExtra("MOVIE_ID", item.id)
                        intent.putExtra("MEDIA_TYPE", "tv")
                        startActivity(intent)
                    }
                }
            }
            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {}
        })
    }
}