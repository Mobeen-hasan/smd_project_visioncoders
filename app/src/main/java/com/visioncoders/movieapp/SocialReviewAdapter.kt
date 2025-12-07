package com.visioncoders.movieapp

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class SocialReviewAdapter(private val reviews: List<UserReview>) :
    RecyclerView.Adapter<SocialReviewAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.reviewAuthor)
        val content: TextView = view.findViewById(R.id.reviewContent)
        val ratingBar: RatingBar = view.findViewById(R.id.reviewRating)
        val btnLike: ImageView = view.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
        val btnComment: ImageView = view.findViewById(R.id.btnComment)
        val tvCommentCount: TextView = view.findViewById(R.id.tvCommentCount)
        val profileImage: CircleImageView = view.findViewById(R.id.image_profile_pic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]

        holder.name.text = review.username
        holder.content.text = review.reviewText
        holder.ratingBar.rating = review.rating.toFloat()

        if (review.userImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(review.userImage)
                .placeholder(R.drawable.profile)
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile)
        }

        val likes = review.likedBy
        holder.tvLikeCount.text = likes.size.toString()
        updateLikeIcon(holder, likes)

        holder.btnLike.setOnClickListener {
            if (currentUserId != null && review.id.isNotEmpty()) {
                toggleLike(review, holder)
            }
        }

        holder.btnComment.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ReviewRepliesActivity::class.java)
            intent.putExtra("REVIEW_ID", review.id)
            intent.putExtra("REVIEW_OWNER_ID", review.userId)
            context.startActivity(intent)
        }
    }

    private fun toggleLike(review: UserReview, holder: ViewHolder) {
        val ref = db.collection("reviews").document(review.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentLikes = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

            if (currentLikes.contains(currentUserId)) {
                currentLikes.remove(currentUserId)
            } else {
                currentLikes.add(currentUserId!!)
            }

            transaction.update(ref, "likedBy", currentLikes)
            currentLikes
        }.addOnSuccessListener { updatedLikes ->
            holder.tvLikeCount.text = updatedLikes.size.toString()
            updateLikeIcon(holder, updatedLikes)

            if (updatedLikes.contains(currentUserId)) {
                sendLikeNotification(review)
            }
        }
    }

    private fun sendLikeNotification(review: UserReview) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (review.userId == user.uid) return

        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            val myImage = doc.getString("profileImage") ?: ""
            val myName = user.displayName ?: "User"

            // 1. In-App Notification (Firestore)
            val notif = hashMapOf(
                "type" to "like",
                "fromId" to user.uid,
                "fromName" to myName,
                "fromImage" to myImage,
                "toId" to review.userId,
                "targetId" to review.id,
                "message" to "liked your review.",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("notifications").add(notif)

            // 2. Push Notification (Node.js)
            NotificationHelper.sendPush(
                targetUserId = review.userId,
                title = "New Like",
                body = "$myName liked your review."
            )
        }
    }

    private fun updateLikeIcon(holder: ViewHolder, likes: List<String>) {
        if (currentUserId != null && likes.contains(currentUserId)) {
            holder.btnLike.setColorFilter(Color.RED)
        } else {
            holder.btnLike.setColorFilter(Color.GRAY)
        }
    }

    override fun getItemCount() = reviews.size
}