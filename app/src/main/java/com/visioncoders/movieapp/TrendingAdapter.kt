package com.visioncoders.movieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.visioncoders.movieapp.api.Movie


class TrendingAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<TrendingAdapter.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val poster: ImageView = itemView.findViewById(R.id.moviePosterImageView)
        val title: TextView = itemView.findViewById(R.id.movieTitleTextView)
        val rating: TextView = itemView.findViewById(R.id.movieRatingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_poster, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.title.text = movie.title
        holder.rating.text = String.format("%.1f", movie.rating)

        val imageUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.movie_frame)
            .into(holder.poster)

        holder.itemView.setOnClickListener {
            onMovieClick(movie)
        }
    }

    override fun getItemCount() = movies.size

    fun updateData(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }
}