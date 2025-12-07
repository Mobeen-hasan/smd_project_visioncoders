package com.visioncoders.movieapp

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendRequestAdapter(
    private var requests: MutableList<FriendRequest>,
    private val onAccept: (FriendRequest) -> Unit,
    private val onReject: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvUserName)
        val time: TextView = view.findViewById(R.id.tvTime)
        val btnConfirm: Button = view.findViewById(R.id.btnConfirm)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]

        holder.name.text = request.fromName
        holder.time.text = DateUtils.getRelativeTimeSpanString(request.timestamp)

        holder.btnConfirm.setOnClickListener { onAccept(request) }
        holder.btnDelete.setOnClickListener { onReject(request) }
    }

    override fun getItemCount() = requests.size

    fun removeItem(request: FriendRequest) {
        val index = requests.indexOf(request)
        if (index != -1) {
            requests.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}