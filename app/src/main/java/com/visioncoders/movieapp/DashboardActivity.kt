package com.visioncoders.movieapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.visioncoders.movieapp.api.MovieResponse
import com.visioncoders.movieapp.api.TmdbApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.google.firebase.messaging.FirebaseMessaging

class DashboardActivity : AppCompatActivity() {

    private lateinit var trendingRecyclerView: RecyclerView
    private lateinit var watchlistRecyclerView: RecyclerView
    private lateinit var adapter: TrendingAdapter
    private lateinit var watchlistAdapter: DashboardWatchlistAdapter

    // Stats Views
    private lateinit var statTotal: TextView
    private lateinit var statMovies: TextView
    private lateinit var statTv: TextView

    private val API_KEY = "b372be1db50166bf650c36ada4c6d41b"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // 1. Initialize Views
        trendingRecyclerView = findViewById(R.id.trendingRecyclerView)
        watchlistRecyclerView = findViewById(R.id.watchlistRecyclerView)
        statTotal = findViewById(R.id.statTotalCount)
        statMovies = findViewById(R.id.statMoviesCount)
        statTv = findViewById(R.id.statTvCount)

        // 2. Setup RecyclerViews
        trendingRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        watchlistRecyclerView.layoutManager = LinearLayoutManager(this)

        // 3. Navigation Buttons
        findViewById<LinearLayout>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, DiscoverActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navReviews).setOnClickListener {
            startActivity(Intent(this, ReviewsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navWatchlist).setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, Page9Activity::class.java))
        }
        findViewById<Button>(R.id.searchUsersButton).setOnClickListener {
            startActivity(Intent(this, DiscoverUsersActivity::class.java))
        }

        // "Manage" Watchlist Button
        findViewById<TextView>(R.id.btnManageWatchlist).setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }

        findViewById<ImageView>(R.id.notificationIcon).setOnClickListener {
            startActivity(Intent(this, FragmentNotificationsActivity::class.java))
        }

        // 4. Load Data
        fetchTrendingMovies()
        fetchWatchlistData()

        saveFcmToken()
    }

    private fun saveFcmToken() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Save to Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .update("fcmToken", token)
                .addOnFailureListener {
                    // If the document doesn't exist yet (rare), set it with merge
                    val data = hashMapOf("fcmToken" to token)
                    db.collection("users").document(user.uid)
                        .set(data, com.google.firebase.firestore.SetOptions.merge())
                }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchWatchlistData()
    }

    private fun fetchWatchlistData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("watchlist")
            .whereEqualTo("userId", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val items = documents.toObjects(WatchlistItem::class.java)

                // A. Update Stats Cards
                val total = items.size
                val movies = items.count { it.mediaType == "movie" }
                val shows = items.count { it.mediaType == "tv" }

                statTotal.text = total.toString()
                statMovies.text = movies.toString()
                statTv.text = shows.toString()

                // B. Update Mini-List (Show only top 3)
                val recentItems = items.take(3)
                watchlistAdapter = DashboardWatchlistAdapter(recentItems)
                watchlistRecyclerView.adapter = watchlistAdapter
            }
            .addOnFailureListener {
            }
    }

    private fun fetchTrendingMovies() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(TmdbApi::class.java)
        val call = api.getTrendingMovies(API_KEY)

        call.enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val movies = response.body()!!.results
                    adapter = TrendingAdapter(movies) { movie ->
                        val intent = Intent(this@DashboardActivity, MovieDetailsActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("MEDIA_TYPE", "movie")
                        startActivity(intent)
                    }
                    trendingRecyclerView.adapter = adapter
                }
            }
            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                Toast.makeText(this@DashboardActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}