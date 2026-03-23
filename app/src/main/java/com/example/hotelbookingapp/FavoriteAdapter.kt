package com.example.hotelbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FavoriteAdapter(
    private var list: List<FavoriteHotel>,
    private val onDeleteClick: (FavoriteHotel) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView  = view.findViewById(R.id.favImage)
        val name: TextView    = view.findViewById(R.id.favName)
        val city: TextView    = view.findViewById(R.id.favCity)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteFav)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hotel = list[position]
        holder.name.text = hotel.name
        holder.city.text = hotel.city
        Glide.with(holder.itemView.context)
            .load(hotel.imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.image)

        holder.deleteBtn.setOnClickListener { onDeleteClick(hotel) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<FavoriteHotel>) {
        list = newList
        notifyDataSetChanged()
    }
}