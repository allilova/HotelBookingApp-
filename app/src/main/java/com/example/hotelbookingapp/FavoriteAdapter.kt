package com.example.hotelbookingapp

import android.content.Context
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
    private val activityContext: Context,
    private val onDeleteClick: (FavoriteHotel) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    // Pre-resolve ALL hotel names once using the activity context when the
    // adapter is created or data is updated. This guarantees the correct locale
    // is used and avoids any stale-context issues inside onBindViewHolder.
    private var resolvedHotels: List<Hotel> = HotelRepository.getHotels(activityContext)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView       = view.findViewById(R.id.favImage)
        val name: TextView         = view.findViewById(R.id.favName)
        val city: TextView         = view.findViewById(R.id.favCity)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteFav)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activityContext)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hotel = list[position]
        val resolved = resolvedHotels.find { it.id == hotel.hotelId }
        holder.name.text = resolved?.name ?: hotel.name
        holder.city.text = resolved?.city ?: hotel.city

        Glide.with(activityContext)
            .load(hotel.imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.image)

        holder.deleteBtn.setOnClickListener { onDeleteClick(hotel) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<FavoriteHotel>) {
        // Re-resolve hotels on every update so a locale change mid-session
        // is also picked up correctly.
        resolvedHotels = HotelRepository.getHotels(activityContext)
        list = newList
        notifyDataSetChanged()
    }
}