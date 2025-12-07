package com.visioncoders.movieapp

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class ReplyAdapter(private val replies: List<ReviewReply>) :
    RecyclerView.Adapter<ReplyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: CircleImageView = view.findViewById(R.id.imgProfile)
        val username: TextView = view.findViewById(R.id.tvUsername)
        val text: TextView = view.findViewById(R.id.tvReplyText)
        val time: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reply, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reply = replies[position]

        holder.username.text = reply.username
        holder.text.text = reply.text

        // Format time (e.g., "5 min ago")
        holder.time.text = DateUtils.getRelativeTimeSpanString(reply.timestamp)


        if (reply.userImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(reply.userImage)
                .placeholder(R.drawable.profile)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.profile)
        }
    }

    override fun getItemCount() = replies.size
}