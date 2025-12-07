package com.visioncoders.movieapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DashboardWatchlistAdapter(private val items: List<WatchlistItem>) :
    RecyclerView.Adapter<DashboardWatchlistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.watchlistMoviePoster)
        val title: TextView = view.findViewById(R.id.watchlistMovieTitle)
        val info: TextView = view.findViewById(R.id.watchlistMovieInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist_movie, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.title

        // Show "Movie" or "TV"
        val type = if (item.mediaType == "tv") "TV Show" else "Movie"
        holder.info.text = type

        val imageUrl = item.posterPath.replace("w500", "w185")

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.movie_frame)
            .into(holder.poster)

        // Click to Open Details
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