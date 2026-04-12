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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adapter for the favorites list.
 *
 * Key fix: we track the coroutine Job per ViewHolder so we can cancel
 * it when the view is recycled. This prevents old coroutines from writing
 * to the wrong view after RecyclerView recycles it.
 */
class FavoriteAdapter(
    private var list: List<FavoriteHotel>,
    private val activityContext: Context,
    private val onDeleteClick: (FavoriteHotel) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image:     ImageView   = view.findViewById(R.id.favImage)
        val name:      TextView    = view.findViewById(R.id.favName)
        val city:      TextView    = view.findViewById(R.id.favCity)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteFav)

        // Track the running coroutine so we can cancel it on recycle
        var resolveJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(activityContext)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Cancel the running resolve coroutine so it doesn't write
        // stale data to a recycled view
        holder.resolveJob?.cancel()
        holder.resolveJob = null
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hotel = list[position]

        // Cancel any previous job on this ViewHolder before starting a new one
        holder.resolveJob?.cancel()

        // Set fallback values immediately so the item is never blank
        holder.name.text = hotel.name
        holder.city.text = hotel.city

        // Resolve the hotel name in the current locale.
        // For custom hotels this may hit Firestore, so we run on IO.
        holder.resolveJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val resolved = withContext(Dispatchers.IO) {
                    HotelRepository.resolve(activityContext, hotel.hotelId, hotel.name)
                }
                // Only update if this ViewHolder still shows the same item
                holder.name.text = resolved?.name ?: hotel.name
                holder.city.text = resolved?.city ?: hotel.city
            } catch (e: Exception) {
                // Keep the fallback values
            }
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