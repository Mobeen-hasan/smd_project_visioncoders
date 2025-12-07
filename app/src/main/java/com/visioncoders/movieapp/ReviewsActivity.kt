package com.visioncoders.movieapp

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReviewsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserReviewsAdapter

    // Stats Views
    private lateinit var tvTotalReviews: TextView
    private lateinit var tvTotalLikes: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reviews)

        // Initialize Views
        recyclerView = findViewById(R.id.reviewsRecyclerView)
        tvTotalReviews = findViewById(R.id.tvTotalReviews)
        tvTotalLikes = findViewById(R.id.tvTotalLikes)

        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchMyReviews()

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        fetchMyReviews()
        //fetchStats()
    }

    private fun fetchMyReviews() {
        val user = auth.currentUser ?: return

        db.collection("reviews")
            .whereEqualTo("userId", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val reviews = mutableListOf<UserReview>()
                for (doc in documents) {
                    val r = doc.toObject(UserReview::class.java)
                    r.id = doc.id
                    reviews.add(r)
                }

                // --- CALCULATE STATS HERE ---
                val totalReviews = reviews.size

                // Sum up the 'likedBy' list size for each review
                val totalLikesReceived = reviews.sumOf { it.likedBy.size }


                tvTotalReviews.text = totalReviews.toString()
                tvTotalLikes.text = totalLikesReceived.toString()


                adapter = UserReviewsAdapter(reviews,
                    onEdit = { review -> openEditScreen(review) },
                    onDelete = { review -> confirmDelete(review) }
                )
                recyclerView.adapter = adapter
            }
    }

    private fun openEditScreen(review: UserReview) {
        val intent = Intent(this, Page7Activity::class.java)
        intent.putExtra("MOVIE_ID", review.movieId)
        intent.putExtra("MOVIE_TITLE", review.movieTitle)
        intent.putExtra("MOVIE_POSTER", review.posterPath)
        intent.putExtra("MEDIA_TYPE", review.mediaType)
        intent.putExtra("REVIEW_ID", review.id)
        intent.putExtra("OLD_RATING", review.rating)
        intent.putExtra("OLD_TEXT", review.reviewText)
        startActivity(intent)
    }

    private fun confirmDelete(review: UserReview) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteReview(review) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReview(review: UserReview) {
        val user = auth.currentUser ?: return
        val userRef = db.collection("users").document(user.uid)

        // Update User Review Count (in Database)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentCount = snapshot.getLong("reviewCount") ?: 1L
            val currentAvg = snapshot.getDouble("rating") ?: 0.0

            if (currentCount > 0) {
                val oldSum = currentAvg * currentCount
                val newCount = currentCount - 1
                val newAvg = if (newCount > 0) (oldSum - review.rating) / newCount else 0.0

                transaction.update(userRef, "reviewCount", newCount)
                transaction.update(userRef, "rating", newAvg)
            }
        }.addOnSuccessListener {
            // Delete actual review
            db.collection("reviews").document(review.id).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Review Deleted", Toast.LENGTH_SHORT).show()

                    // Remove from adapter immediately
                    adapter.removeItem(review)

                    // Update Stats on screen by subtracting
                    val currentReviews = tvTotalReviews.text.toString().toIntOrNull() ?: 0
                    val currentLikes = tvTotalLikes.text.toString().toIntOrNull() ?: 0

                    if (currentReviews > 0) {
                        tvTotalReviews.text = (currentReviews - 1).toString()
                    }
                    if (currentLikes >= review.likedBy.size) {
                        tvTotalLikes.text = (currentLikes - review.likedBy.size).toString()
                    }
                }
        }
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, DiscoverActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navWatchlist).setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
            finish()
        }

        // Reviews (Current)

        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, Page9Activity::class.java))
            finish()
        }
    }
}