package com.visioncoders.movieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.visioncoders.movieapp.api.Cast
import de.hdodenhof.circleimageview.CircleImageView

class CastAdapter(private val castList: List<Cast>) : RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    class CastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: CircleImageView = view.findViewById(R.id.castImage)
        val name: TextView = view.findViewById(R.id.castName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cast, parent, false)
        return CastViewHolder(view)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        val cast = castList[position]
        holder.name.text = cast.name

        val imageUrl = "https://image.tmdb.org/t/p/w200${cast.profilePath}"
        Glide.with(holder.itemView.context).load(imageUrl).placeholder(R.drawable.profile).into(holder.img)
    }

    override fun getItemCount() = castList.size
}