package com.visioncoders.movieapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WatchlistActivity : AppCompatActivity() {


    private lateinit var recyclerView: RecyclerView
    private lateinit var totalCountTxt: TextView
    private lateinit var movieCountTxt: TextView
    private lateinit var tvCountTxt: TextView

    private var allItems: List<WatchlistItem> = emptyList()
    private var currentFilter: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        recyclerView = findViewById(R.id.recycler_view_watchlist)
        recyclerView.layoutManager = LinearLayoutManager(this)

        totalCountTxt = findViewById(R.id.text_item_count)
        movieCountTxt = findViewById(R.id.text_movie_count)
        tvCountTxt = findViewById(R.id.text_tv_show_count)

        setupFilters()
        fetchWatchlist()
        setupBottomNav()
    }

    private fun setupFilters() {
        findViewById<View>(R.id.chip_all).setOnClickListener { filterList("all") }
        findViewById<View>(R.id.chip_movies).setOnClickListener { filterList("movie") }
        findViewById<View>(R.id.chip_tv_shows).setOnClickListener { filterList("tv") }
        findViewById<View>(R.id.chip_recently_added).setOnClickListener { filterList("recent") }
        findViewById<View>(R.id.chip_liked).setOnClickListener { filterList("liked") }


        findViewById<View>(R.id.chip_watched).setOnClickListener { filterList("watched") }
    }

    private fun filterList(type: String) {
        currentFilter = type

        val sortedList = when (type) {
            "all" -> allItems.sortedBy { it.title }
            "recent" -> allItems.sortedByDescending { it.timestamp }
            "movie" -> allItems.filter { it.mediaType == "movie" }.sortedBy { it.title }
            "tv" -> allItems.filter { it.mediaType == "tv" }.sortedBy { it.title }
            "liked" -> allItems.filter { it.isLiked }.sortedBy { it.title }


            "watched" -> allItems.filter { it.isWatched }.sortedBy { it.title }

            else -> allItems.sortedBy { it.title }
        }

        recyclerView.adapter = WatchlistAdapter(
            sortedList,
            onDeleteClick = { item -> deleteItem(item) },
            onLikeClick = { item -> toggleLike(item) },
            onWatchedClick = { item -> toggleWatched(item) } // Pass new callback
        )
    }


    private fun toggleWatched(item: WatchlistItem) {
        val db = FirebaseFirestore.getInstance()
        val newStatus = !item.isWatched

        db.collection("watchlist").document(item.id)
            .update("isWatched", newStatus)
            .addOnSuccessListener {
                // Update Local List
                allItems = allItems.map {
                    if (it.id == item.id) it.copy(isWatched = newStatus) else it
                }
                // Refresh View
                filterList(currentFilter)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }


    private fun toggleLike(item: WatchlistItem) {
        val db = FirebaseFirestore.getInstance()
        val newStatus = !item.isLiked

        db.collection("watchlist").document(item.id)
            .update("isLiked", newStatus)
            .addOnSuccessListener {
                allItems = allItems.map {
                    if (it.id == item.id) it.copy(isLiked = newStatus) else it
                }
                filterList(currentFilter)
            }
    }

    private fun deleteItem(item: WatchlistItem) {
        val db = FirebaseFirestore.getInstance()
        db.collection("watchlist").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Removed from watchlist", Toast.LENGTH_SHORT).show()
                allItems = allItems.filter { it.id != item.id }
                updateCounts(allItems)
                filterList(currentFilter)
            }
    }

    private fun fetchWatchlist() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("watchlist")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                allItems = documents.toObjects(WatchlistItem::class.java)
                filterList("all")
                updateCounts(allItems)
            }
    }

    private fun updateCounts(items: List<WatchlistItem>) {
        val total = items.size
        val movies = items.count { it.mediaType == "movie" }
        val shows = items.count { it.mediaType == "tv" }

        totalCountTxt.text = "$total items in watchlist"
        movieCountTxt.text = "$movies Movies"
        tvCountTxt.text = "$shows TV Shows"
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navSearch)?.setOnClickListener {
            startActivity(Intent(this, DiscoverActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navReviews)?.setOnClickListener {
            startActivity(Intent(this, ReviewsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, Page9Activity::class.java))
            finish()
        }
    }
}