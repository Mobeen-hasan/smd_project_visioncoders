package com.visioncoders.movieapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class Page7Activity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var movieId: Int = -1
    private var movieTitle: String = ""
    private var moviePoster: String = ""
    private var movieRating: String = ""
    private var movieInfo: String = ""
    private var mediaType: String = "movie"

    // Edit Mode Variables
    private var reviewId: String? = null
    private var oldRating: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page7)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get Data
        movieId = intent.getIntExtra("MOVIE_ID", -1)
        movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: ""
        moviePoster = intent.getStringExtra("MOVIE_POSTER") ?: ""
        movieRating = intent.getStringExtra("MOVIE_RATING") ?: "0.0"
        movieInfo = intent.getStringExtra("MOVIE_INFO") ?: ""
        mediaType = intent.getStringExtra("MEDIA_TYPE") ?: "movie"

        // CHECK IF EDITING
        reviewId = intent.getStringExtra("REVIEW_ID")
        oldRating = intent.getDoubleExtra("OLD_RATING", 0.0)
        val oldText = intent.getStringExtra("OLD_TEXT")

        // UI Setup
        findViewById<TextView>(R.id.text_movie_title).text = movieTitle
        findViewById<TextView>(R.id.text_rating).text = movieRating
        findViewById<TextView>(R.id.text_movie_details).text = movieInfo

        val etReview = findViewById<EditText>(R.id.edit_text_review)
        val ratingBar = findViewById<RatingBar>(R.id.rating_bar_input)
        val btnSubmit = findViewById<Button>(R.id.button_submit_review)

        // Pre-fill if editing
        if (reviewId != null) {
            etReview.setText(oldText)
            ratingBar.rating = oldRating.toFloat()
            btnSubmit.text = "Update Review"
            findViewById<TextView>(R.id.text_title).text = "Edit Review"
        }

        val posterImg = findViewById<ImageView>(R.id.image_movie_poster)
        if (moviePoster.isNotEmpty()) {
            Glide.with(this).load(moviePoster).into(posterImg)
        }

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }
        btnSubmit.setOnClickListener { submitReview() }
    }

    private fun submitReview() {
        val rating = findViewById<RatingBar>(R.id.rating_bar_input).rating
        val text = findViewById<EditText>(R.id.edit_text_review).text.toString()
        val user = auth.currentUser ?: return

        if (rating == 0f) {
            Toast.makeText(this, "Please rate the movie", Toast.LENGTH_SHORT).show()
            return
        }

        val review = UserReview(
            movieId = movieId,
            userId = user.uid,
            username = user.displayName ?: "User",
            rating = rating.toDouble(),
            reviewText = text,
            timestamp = System.currentTimeMillis(),
            movieTitle = movieTitle,
            posterPath = moviePoster,
            mediaType = mediaType,
            userImage = ""
        )

        if (NetworkUtils.isInternetAvailable(this)) {
            // ONLINE: Send directly to Firebase
            db.collection("reviews").add(review).addOnSuccessListener {
                Toast.makeText(this, "Review Posted!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Error posting review", Toast.LENGTH_SHORT).show()
            }
        } else {
            // OFFLINE: Save to SQLite Queue
            saveToOfflineQueue(review)
        }
    }

    private fun saveToOfflineQueue(review: UserReview) {
        val gson = Gson()
        val jsonPayload = gson.toJson(review)
        val syncRequest = SyncRequest(type = "REVIEW", payload = jsonPayload)

        // Save to Room
        lifecycleScope.launch {
            AppDatabase.getDatabase(this@Page7Activity).appDao().addToQueue(syncRequest)

            // Schedule WorkManager
            scheduleSyncWorker()

            Toast.makeText(this@Page7Activity, "No Internet. Saved to Queue!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "MovieAppSync",
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }



    private fun createNewReview(uid: String, name: String, rating: Double, text: String) {
        val review = hashMapOf(
            "movieId" to movieId,
            "userId" to uid,
            "username" to name,
            "rating" to rating,
            "reviewText" to text,
            "timestamp" to System.currentTimeMillis(),
            "movieTitle" to movieTitle,
            "posterPath" to moviePoster,
            "mediaType" to mediaType
        )

        db.collection("reviews").add(review).addOnSuccessListener {
            updateUserStats(uid, rating, isNew = true)
        }
    }

    private fun updateExistingReview(uid: String, newRating: Double, newText: String) {

        db.collection("reviews").document(reviewId!!)
            .update(mapOf("rating" to newRating, "reviewText" to newText))
            .addOnSuccessListener {
                updateUserStats(uid, newRating, isNew = false, oldRatingVal = oldRating)
            }
    }

    private fun updateUserStats(userId: String, newRating: Double, isNew: Boolean, oldRatingVal: Double = 0.0) {
        val userRef = db.collection("users").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentCount = snapshot.getLong("reviewCount") ?: 0L
            val currentAvg = snapshot.getDouble("rating") ?: 0.0

            var newCount = currentCount
            var newAvg = currentAvg

            if (isNew) {
                newCount = currentCount + 1
                newAvg = ((currentAvg * currentCount) + newRating) / newCount
            } else {
                // Formula: (OldSum - OldRating + NewRating) / Count
                val oldSum = currentAvg * currentCount
                newAvg = (oldSum - oldRatingVal + newRating) / currentCount
            }

            transaction.update(userRef, "reviewCount", newCount)
            transaction.update(userRef, "rating", newAvg)
        }.addOnSuccessListener {
            finish()
        }
    }
}