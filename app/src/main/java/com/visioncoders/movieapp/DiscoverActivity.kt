package com.visioncoders.movieapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.visioncoders.movieapp.api.MovieResponse
import com.visioncoders.movieapp.api.TmdbApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DiscoverActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var recentRecyclerView: RecyclerView

    private lateinit var recentSearchesTitle: TextView
    private lateinit var trendingHeaderLayout: LinearLayout

    private val API_KEY = "b372be1db50166bf650c36ada4c6d41b"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover)

        // 1. Initialize Views
        searchEditText = findViewById(R.id.searchEditText)
        resultsRecyclerView = findViewById(R.id.trendingRecyclerView)
        recentRecyclerView = findViewById(R.id.recentSearchesRecyclerView)

        // hide these ids
        recentSearchesTitle = findViewById(R.id.tvRecentSearchesTitle)
        trendingHeaderLayout = findViewById(R.id.layoutTrendingHeader)

        // 2. Setup RecyclerViews
        resultsRecyclerView.layoutManager = GridLayoutManager(this, 3)


        recentRecyclerView.layoutManager = LinearLayoutManager(this)

        // 3. Load Initial Data
        loadRecentSearches()
        loadTrending()

        // 4. Search Action Listener
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString()
                if (query.isNotEmpty()) {
                    performSearch(query)
                    saveSearch(query)
                }
                true
            } else {
                false
            }
        }


        setupBottomNav()
    }

    private fun loadTrending() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(TmdbApi::class.java)

        api.getTrendingMovies(API_KEY).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val movies = response.body()!!.results

                    // Ensure Layout is Grid
                    resultsRecyclerView.layoutManager = GridLayoutManager(this@DiscoverActivity, 3)

                    resultsRecyclerView.adapter = TrendingAdapter(movies) { movie ->
                        val intent = Intent(this@DiscoverActivity, MovieDetailsActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("MEDIA_TYPE", "movie")
                        startActivity(intent)
                    }
                }
            }
            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                Toast.makeText(this@DiscoverActivity, "Error loading trending: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performSearch(query: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(TmdbApi::class.java)

        api.searchMulti(API_KEY, query).enqueue(object : Callback<MovieResponse> {
            override fun onResponse(call: Call<MovieResponse>, response: Response<MovieResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!.results
                    val mediaResults = results.filter { it.mediaType == "movie" || it.mediaType == "tv" }

                    resultsRecyclerView.layoutManager = GridLayoutManager(this@DiscoverActivity, 3)

                    resultsRecyclerView.adapter = TrendingAdapter(mediaResults) { movie ->
                        val intent = Intent(this@DiscoverActivity, MovieDetailsActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        intent.putExtra("MEDIA_TYPE", movie.mediaType ?: "movie")
                        startActivity(intent)
                    }


                    recentRecyclerView.visibility = View.GONE
                    recentSearchesTitle.visibility = View.GONE
                    trendingHeaderLayout.visibility = View.GONE


                    resultsRecyclerView.visibility = View.VISIBLE
                }
            }
            override fun onFailure(call: Call<MovieResponse>, t: Throwable) {
                Toast.makeText(this@DiscoverActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRecentSearches() {
        val prefs = getSharedPreferences("MovieApp", Context.MODE_PRIVATE)
        val historySet = prefs.getStringSet("history", mutableSetOf()) ?: mutableSetOf()
        val historyList = historySet.toMutableList()

        val adapter = RecentSearchAdapter(historyList,
            onSearchClick = { query ->
                searchEditText.setText(query)
                performSearch(query)
            },
            onDeleteClick = { query ->
                historyList.remove(query)
                prefs.edit().putStringSet("history", historyList.toSet()).apply()
            }
        )
        recentRecyclerView.adapter = adapter
    }

    private fun saveSearch(query: String) {
        val prefs = getSharedPreferences("MovieApp", Context.MODE_PRIVATE)
        val historySet = prefs.getStringSet("history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        historySet.add(query)
        prefs.edit().putStringSet("history", historySet).apply()
        loadRecentSearches()
    }


    private fun setupBottomNav() {
        // Home
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish() // Close current activity
        }

        // Search (Current Page - do nothing or refresh)
        findViewById<LinearLayout>(R.id.navSearch).setOnClickListener {
        }

        // Watchlist
        findViewById<LinearLayout>(R.id.navWatchlist)?.setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
            finish()
        }

        // Reviews
        findViewById<LinearLayout>(R.id.navReviews)?.setOnClickListener {
            startActivity(Intent(this, ReviewsActivity::class.java))
            finish()
        }

        // Profile
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, Page9Activity::class.java))
            finish()
        }
    }


}