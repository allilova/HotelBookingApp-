package com.example.hotelbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HotelAdapter(
    private val onClick: (Hotel, ImageView) -> Unit
) : ListAdapter<Hotel, HotelAdapter.HotelViewHolder>(DiffCallback) {

    companion object DiffCallback : DiffUtil.ItemCallback<Hotel>() {
        override fun areItemsTheSame(a: Hotel, b: Hotel) = a.id == b.id
        override fun areContentsTheSame(a: Hotel, b: Hotel) = a == b
    }

    class HotelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgHotel: ImageView = view.findViewById(R.id.hotelImage)
        val tvName: TextView    = view.findViewById(R.id.hotelName)
        val tvCity: TextView    = view.findViewById(R.id.hotelCity)
        val tvPrice: TextView   = view.findViewById(R.id.hotelPrice)
        val rbRating: RatingBar = view.findViewById(R.id.hotelRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hotel, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        val hotel = getItem(position)
        holder.tvName.text  = hotel.name
        holder.tvCity.text  = hotel.city
        holder.tvPrice.text = "${hotel.price} лв. / нощ"
        holder.rbRating.rating = hotel.rating


        holder.imgHotel.transitionName = "hotelImageTransition_${hotel.id}"

        Glide.with(holder.itemView.context)
            .load(hotel.imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgHotel)

        holder.itemView.setOnClickListener {
            onClick(hotel, holder.imgHotel)
        }
    }
}