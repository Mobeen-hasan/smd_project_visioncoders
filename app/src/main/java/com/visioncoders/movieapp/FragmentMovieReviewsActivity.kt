package com.visioncoders.movieapp

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FragmentMovieReviewsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAll: TextView
    private lateinit var btnFriends: TextView
    private lateinit var etReview: EditText
    private lateinit var ratingBar: RatingBar

    // Header Views
    private lateinit var imgPoster: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDetails: TextView
    private lateinit var tvRatingSummary: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var movieId: Int = -1

    // Data
    private var allReviews = listOf<UserReview>()
    private var friendIds = mutableSetOf<String>()

    // Movie Info
    private var movieTitle: String = ""
    private var moviePoster: String = ""

    private var myProfileUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_movie_reviews)

        movieId = intent.getIntExtra("MOVIE_ID", -1)

        movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: "Unknown Title"
        moviePoster = intent.getStringExtra("MOVIE_POSTER") ?: ""
        val movieInfo = intent.getStringExtra("MOVIE_INFO") ?: ""
        val movieRating = intent.getStringExtra("MOVIE_RATING") ?: "0.0"
        val ratingCount = intent.getStringExtra("RATING_COUNT") ?: ""

        recyclerView = findViewById(R.id.recycler_view_reviews)
        btnAll = findViewById(R.id.btnFilterAll)
        btnFriends = findViewById(R.id.btnFilterFriends)
        etReview = findViewById(R.id.edit_text_review)
        ratingBar = findViewById(R.id.inputRatingBar)

        imgPoster = findViewById(R.id.image_poster)
        tvTitle = findViewById(R.id.text_movie_title)
        tvDetails = findViewById(R.id.text_movie_details)
        tvRatingSummary = findViewById(R.id.text_rating_summary)

        tvTitle.text = movieTitle
        tvDetails.text = movieInfo
        tvRatingSummary.text = "$movieRating ($ratingCount)"

        if (moviePoster.isNotEmpty()) {
            Glide.with(this).load(moviePoster).into(imgPoster)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.button_send_review).setOnClickListener { submitReview() }

        btnAll.setOnClickListener { filterList("all") }
        btnFriends.setOnClickListener { filterList("friends") }


        val imgMyProfile = findViewById<ImageView>(R.id.image_profile_pic)

        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
                myProfileUrl = doc.getString("profileImage") ?: ""
                if (myProfileUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(myProfileUrl)
                        .placeholder(R.drawable.profile)
                        .into(imgMyProfile)
                }
            }
        }

        if (movieId != -1) {
            fetchFriends()
            fetchAllReviews()
        } else {
            Toast.makeText(this, "Error: Invalid Movie ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFriends() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("friends").get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    friendIds.add(doc.id)
                }
            }
    }

    private fun fetchAllReviews() {
        db.collection("reviews")
            .whereEqualTo("movieId", movieId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val reviewsList = mutableListOf<UserReview>()

                for (document in documents) {
                    val review = document.toObject(UserReview::class.java)
                    review.id = document.id
                    reviewsList.add(review)
                }

                allReviews = reviewsList
                filterList("all")
            }
    }

    private fun filterList(type: String) {
        val filtered = if (type == "friends") {
            updateFilterUI(false)
            allReviews.filter { friendIds.contains(it.userId) }
        } else {
            updateFilterUI(true)
            allReviews
        }
        recyclerView.adapter = SocialReviewAdapter(filtered)
    }

    private fun updateFilterUI(isAllSelected: Boolean) {
        if (isAllSelected) {
            btnAll.setBackgroundResource(R.drawable.bg_chip_selected)
            btnAll.setTextColor(Color.WHITE)
            btnFriends.setBackgroundResource(R.drawable.bg_chip_unselected)
            btnFriends.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary))
        } else {
            btnFriends.setBackgroundResource(R.drawable.bg_chip_selected)
            btnFriends.setTextColor(Color.WHITE)
            btnAll.setBackgroundResource(R.drawable.bg_chip_unselected)
            btnAll.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary))
        }
    }

    private fun submitReview() {
        val text = etReview.text.toString().trim()
        val rating = ratingBar.rating
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "Please Login first", Toast.LENGTH_SHORT).show()
            return
        }
        if (text.isEmpty()) {
            Toast.makeText(this, "Please write a review", Toast.LENGTH_SHORT).show()
            return
        }
        if (rating == 0f) {
            Toast.makeText(this, "Please give a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val review = hashMapOf(
            "movieId" to movieId,
            "userId" to user.uid,
            "username" to (user.displayName ?: "User"),
            "rating" to rating,
            "reviewText" to text,
            "timestamp" to System.currentTimeMillis(),
            "movieTitle" to movieTitle,
            "posterPath" to moviePoster,
            "mediaType" to "movie",
            "likedBy" to emptyList<String>(),


            "userImage" to myProfileUrl
        )

        db.collection("reviews").add(review)
            .addOnSuccessListener {
                Toast.makeText(this, "Review Posted!", Toast.LENGTH_SHORT).show()
                etReview.setText("")
                ratingBar.rating = 0f
                fetchAllReviews()
                updateUserStats(user.uid, rating.toDouble())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserStats(userId: String, newRating: Double) {
        val userRef = db.collection("users").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentCount = snapshot.getLong("reviewCount") ?: 0L
            val currentAvg = snapshot.getDouble("rating") ?: 0.0

            val newCount = currentCount + 1
            val newAvg = ((currentAvg * currentCount) + newRating) / newCount

            transaction.update(userRef, "reviewCount", newCount)
            transaction.update(userRef, "rating", newAvg)
        }
    }
}
