package com.visioncoders.movieapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserReviewsAdapter(
    private val reviews: MutableList<UserReview>,
    private val onEdit: (UserReview) -> Unit,
    private val onDelete: (UserReview) -> Unit
) : RecyclerView.Adapter<UserReviewsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvMovieTitle)
        val poster: ImageView = view.findViewById(R.id.imgMoviePoster)
        val reviewText: TextView = view.findViewById(R.id.tvReviewText)
        val date: TextView = view.findViewById(R.id.tvReviewDate)
        val ratingBar: RatingBar = view.findViewById(R.id.userRatingBar)
        val btnEdit: TextView = view.findViewById(R.id.btnEdit)
        val btnDelete: TextView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]

        holder.title.text = review.movieTitle
        holder.reviewText.text = review.reviewText
        holder.ratingBar.rating = review.rating.toFloat()

        val dateString = android.text.format.DateFormat.format("dd MMM yyyy", review.timestamp)
        holder.date.text = dateString

        Glide.with(holder.itemView.context)
            .load(review.posterPath)
            .placeholder(R.drawable.movie_frame)
            .into(holder.poster)

        // --- TIME CHECK LOGIC ---
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - review.timestamp
        val fiveMinutes = 5 * 60 * 1000
        val oneHour = 60 * 60 * 1000

        // Show Edit if within 5 mins
        if (timeDiff < fiveMinutes) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEdit(review) }
        } else {
            holder.btnEdit.visibility = View.GONE
        }

        // Show Delete if within 1 hour
        if (timeDiff < oneHour) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDelete(review) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }

        // Normal Click to Open Details
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MovieDetailsActivity::class.java)
            intent.putExtra("MOVIE_ID", review.movieId)
            intent.putExtra("MEDIA_TYPE", review.mediaType)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = reviews.size

    fun removeItem(review: UserReview) {
        val index = reviews.indexOf(review)
        if (index != -1) {
            reviews.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}