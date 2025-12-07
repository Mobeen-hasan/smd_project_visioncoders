package com.visioncoders.movieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.visioncoders.movieapp.api.Review

class ReviewAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewAdapter.ReviewHolder>() {

    class ReviewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val author: TextView = view.findViewById(R.id.reviewAuthor)
        val content: TextView = view.findViewById(R.id.reviewContent)
        val rating: RatingBar = view.findViewById(R.id.reviewRating)
        val avatar: ImageView = view.findViewById(R.id.image_profile_pic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_review, parent, false)
        return ReviewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewHolder, position: Int) {
        val review = reviews[position]
        holder.author.text = review.author
        holder.content.text = review.content
        val ratingVal = review.authorDetails?.rating ?: 0.0
        holder.rating.rating = (ratingVal / 2).toFloat()

        // Load Avatar
        val avatarPath = review.authorDetails?.avatarPath
        if (avatarPath != null) {
            val fullUrl = if (avatarPath.startsWith("/http")) {
                avatarPath.substring(1)
            } else {
                "https://image.tmdb.org/t/p/w200$avatarPath"
            }

            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .placeholder(R.drawable.profile)
                .into(holder.avatar)
        }
    }

    override fun getItemCount() = reviews.size
}