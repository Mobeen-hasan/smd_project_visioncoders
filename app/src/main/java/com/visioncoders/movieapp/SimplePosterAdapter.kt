package com.visioncoders.movieapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SimplePosterAdapter(private val items: List<WatchlistItem>) :
    RecyclerView.Adapter<SimplePosterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.moviePosterImageView)

        val title: TextView = view.findViewById(R.id.movieTitleTextView)
        val rating: TextView = view.findViewById(R.id.movieRatingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie_poster, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]


        holder.title.text = item.title
        holder.rating.text = String.format("%.1f", item.rating)

        // Load Image (Using smaller size for grid efficiency)
        val url = item.posterPath.replace("w500", "w185")

        Glide.with(holder.itemView.context)
            .load(url)
            .placeholder(R.drawable.movie_frame)
            .into(holder.img)

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