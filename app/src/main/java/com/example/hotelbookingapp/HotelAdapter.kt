package com.example.hotelbookingapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HotelAdapter(private var hotels: List<Hotel>) :
    RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {

    class HotelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgHotel: ImageView = view.findViewById(R.id.hotelImage)
        val tvName: TextView = view.findViewById(R.id.hotelName)
        val tvCity: TextView = view.findViewById(R.id.hotelCity)
        val tvPrice: TextView = view.findViewById(R.id.hotelPrice)
        val rbRating: RatingBar = view.findViewById(R.id.hotelRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hotel, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        val hotel = hotels[position]
        holder.tvName.text = hotel.name
        holder.tvCity.text = hotel.city
        holder.tvPrice.text = "${hotel.price} лв."
        holder.rbRating.rating = hotel.rating

        Glide.with(holder.itemView.context)
            .load(hotel.imageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgHotel)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, HotelDetailActivity::class.java).apply {
                putExtra("HOTEL_NAME", hotel.name)
                putExtra("HOTEL_CITY", hotel.city)
                putExtra("HOTEL_DESC", hotel.description)
                putExtra("HOTEL_IMAGE", hotel.imageUrl)
                putExtra("HOTEL_LAT", hotel.latitude)
                putExtra("HOTEL_LON", hotel.longitude)
            }
            context.startActivity(intent)
        }
    }

    fun filterList(filteredList: List<Hotel>) {
        this.hotels = filteredList
        notifyDataSetChanged()
    }

    override fun getItemCount() = hotels.size
}