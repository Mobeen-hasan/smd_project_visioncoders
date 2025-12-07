package com.visioncoders.movieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentSearchAdapter(
    private var searches: MutableList<String>,
    private val onSearchClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<RecentSearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Reusing ID from layout, ensure these match your item_recent_search.xml
        val text: TextView = view.findViewById(R.id.text_username)
        val delete: ImageView = view.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val query = searches[position]
        holder.text.text = query

        holder.itemView.setOnClickListener { onSearchClick(query) }

        holder.delete.setOnClickListener {

            val currentPos = holder.bindingAdapterPosition


            if (currentPos != RecyclerView.NO_POSITION) {
                // Get the actual item to delete based on current position
                val itemToDelete = searches[currentPos]

                // 1. Perform external delete logic (SharedPrefs)
                onDeleteClick(itemToDelete)

                // 2. Remove from list using CURRENT position
                searches.removeAt(currentPos)

                // 3. Notify Adapter
                notifyItemRemoved(currentPos)
                notifyItemRangeChanged(currentPos, searches.size)
            }
        }
    }

    override fun getItemCount() = searches.size
}