package com.visioncoders.movieapp

import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NotificationAdapter(
    private var items: MutableList<Any>,
    private val onReqAccept: (FriendRequest) -> Unit,
    private val onReqDelete: (FriendRequest) -> Unit,
    private val onNotifDelete: (AppNotification) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_REQUEST = 1
        private const val TYPE_GENERAL = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is FriendRequest -> TYPE_REQUEST
            is AppNotification -> TYPE_GENERAL
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_REQUEST) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
            RequestViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification_card, parent, false)
            GeneralViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_REQUEST) {
            (holder as RequestViewHolder).bind(items[position] as FriendRequest)
        } else {
            (holder as GeneralViewHolder).bind(items[position] as AppNotification)
        }
    }

    override fun getItemCount() = items.size

    fun removeItem(item: Any) {
        val index = items.indexOf(item)
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvUserName)
        val time: TextView = itemView.findViewById(R.id.tvTime)
        val btnConfirm: Button = itemView.findViewById(R.id.btnConfirm)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val img: ImageView = itemView.findViewById(R.id.imgProfile)

        fun bind(req: FriendRequest) {
            name.text = req.fromName
            time.text = DateUtils.getRelativeTimeSpanString(req.timestamp)

            // LOAD IMAGE
            if (req.fromImage.isNotEmpty()) {
                Glide.with(itemView.context).load(req.fromImage).placeholder(R.drawable.profile).into(img)
            } else {
                img.setImageResource(R.drawable.profile)
            }

            btnConfirm.setOnClickListener { onReqAccept(req) }
            btnDelete.setOnClickListener { onReqDelete(req) }
        }
    }

    inner class GeneralViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.text_notification_type)
        val body: TextView = itemView.findViewById(R.id.text_notification_body)
        val time: TextView = itemView.findViewById(R.id.text_notification_time)
        val img: ImageView = itemView.findViewById(R.id.image_poster)
        val btnClear: ImageView = itemView.findViewById(R.id.btnClear)

        fun bind(notif: AppNotification) {
            title.text = "New ${notif.type.replaceFirstChar { it.uppercase() }}"
            body.text = "${notif.fromName} ${notif.message}"
            time.text = DateUtils.getRelativeTimeSpanString(notif.timestamp)

            // LOAD IMAGE
            if (notif.fromImage.isNotEmpty()) {
                Glide.with(itemView.context).load(notif.fromImage).placeholder(R.drawable.profile).into(img)
            } else {
                img.setImageResource(R.drawable.profile)
            }

            btnClear.setOnClickListener { onNotifDelete(notif) }

            itemView.setOnClickListener {
                if (notif.type == "reply" || notif.type == "like") {
                    val context = itemView.context
                    val intent = Intent(context, ReviewRepliesActivity::class.java)
                    intent.putExtra("REVIEW_ID", notif.targetId)
                    context.startActivity(intent)
                }
            }
        }
    }
}