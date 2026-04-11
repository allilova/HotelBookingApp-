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

        // Set fallback values immediately so the item is not blank while loading
        holder.name.text = hotel.name
        holder.city.text = hotel.city

        // Resolve the current locale name in a coroutine because resolve()
        // may hit Firestore for custom hotels
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                HotelRepository.resolve(activityContext, hotel.hotelId, hotel.name)
            }
            holder.name.text = resolved?.name ?: hotel.name
            holder.city.text = resolved?.city ?: hotel.city
        }

        Glide.with(activityContext)
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