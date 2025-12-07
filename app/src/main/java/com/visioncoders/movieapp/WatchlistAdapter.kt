package com.visioncoders.movieapp

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class WatchlistAdapter(
    private val items: List<WatchlistItem>,
    private val onDeleteClick: (WatchlistItem) -> Unit,
    private val onLikeClick: (WatchlistItem) -> Unit,
    private val onWatchedClick: (WatchlistItem) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.image_poster)
        val title: TextView = view.findViewById(R.id.text_movie_title)
        val rating: TextView = view.findViewById(R.id.text_rating)
        val tvTag: TextView = view.findViewById(R.id.text_tag_tv)
        val details: TextView = view.findViewById(R.id.text_movie_details)
        val date: TextView = view.findViewById(R.id.text_date_added)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_item)
        val btnLike: ImageView = view.findViewById(R.id.btn_like_item)

        // New Button
        val btnWatched: ImageView = view.findViewById(R.id.btn_watched_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.title
        holder.rating.text = String.format("%.1f", item.rating)
        if (item.mediaType == "tv") {
            holder.tvTag.visibility = View.VISIBLE
            holder.details.text = "TV Series"
        } else {
            holder.tvTag.visibility = View.GONE
            holder.details.text = "Movie"
        }
        val dateString = android.text.format.DateFormat.format("d MMM yyyy", item.timestamp)
        holder.date.text = "Added $dateString"
        Glide.with(holder.itemView.context).load(item.posterPath).placeholder(R.drawable.movie_frame).into(holder.poster)

        // --- LIKE LOGIC ---
        if (item.isLiked) {
            holder.btnLike.setImageResource(R.drawable.heart2)
        } else {
            holder.btnLike.setImageResource(R.drawable.heart1)
        }
        holder.btnLike.setOnClickListener { onLikeClick(item) }

        // --- WATCHED LOGIC ---
        if (item.isWatched) {
            // If watched: Green color
            holder.btnWatched.setColorFilter(Color.parseColor("#4CAF50"))
        } else {
            // If not watched: Gray color
            holder.btnWatched.setColorFilter(Color.GRAY)
        }

        holder.btnWatched.setOnClickListener { onWatchedClick(item) }



        holder.btnDelete.setOnClickListener { onDeleteClick(item) }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MovieDetailsActivity::class.java)
            intent.putExtra("MOVIE_ID", item.movieId)
            intent.putExtra("MEDIA_TYPE", item.mediaType)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size
}