package com.visioncoders.movieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FriendAdapter(private val friends: List<User>, private val onClick: (User) -> Unit) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgFriendProfile)
        val name: TextView = view.findViewById(R.id.tvFriendName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = friends[position]
        holder.name.text = user.name
        Glide.with(holder.itemView.context).load(user.profileImage).placeholder(R.drawable.profile).into(holder.img)
        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = friends.size
}