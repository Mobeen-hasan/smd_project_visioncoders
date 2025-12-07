package com.visioncoders.movieapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class UserAdapter(
    private var users: List<User>,
    private val sentRequests: Set<String>,
    private val myFriends: Set<String>,
    private val onUserClick: (User) -> Unit,
    private val onRequestClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProfile: ShapeableImageView = view.findViewById(R.id.imgProfile)
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvHandle: TextView = view.findViewById(R.id.tvUserHandle)
        val btnAction: Button = view.findViewById(R.id.btnAction)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val tvReviewCount: TextView = view.findViewById(R.id.tvReviewCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.tvName.text = user.name
        holder.tvHandle.text = if (user.handle.isNotEmpty()) user.handle else "@${user.name.replace(" ", "")}"
        holder.tvRating.text = String.format("%.1f", user.rating)
        holder.tvReviewCount.text = "${user.reviewCount} reviews"

        if (user.profileImage.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(user.profileImage).placeholder(R.drawable.profile).into(holder.imgProfile)
        }

        holder.itemView.setOnClickListener { onUserClick(user) }

        // --- BUTTON STATE LOGIC ---
        when {
            myFriends.contains(user.id) -> {
                holder.btnAction.text = "Friends"
                holder.btnAction.isEnabled = false
                holder.btnAction.setTextColor(Color.GRAY)
            }
            sentRequests.contains(user.id) -> {
                holder.btnAction.text = "Sent"
                holder.btnAction.isEnabled = false
                holder.btnAction.setTextColor(Color.GRAY)
            }
            else -> {
                holder.btnAction.text = "Add"
                holder.btnAction.isEnabled = true
                holder.btnAction.setTextColor(Color.WHITE)
                holder.btnAction.setOnClickListener { onRequestClick(user) }
            }
        }
    }

    override fun getItemCount() = users.size

    fun updateList(newList: List<User>) {
        this.users = newList
        notifyDataSetChanged()
    }
}