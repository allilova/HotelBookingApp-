package com.example.hotelbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoriteAdapter(
    private var list: List<FavoriteHotel>,
    private val onDeleteClick: (FavoriteHotel) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.favName)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteFav)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hotel = list[position]
        holder.name.text = hotel.name
        holder.deleteBtn.setOnClickListener { onDeleteClick(hotel) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<FavoriteHotel>) {
        list = newList
        notifyDataSetChanged()
    }
}