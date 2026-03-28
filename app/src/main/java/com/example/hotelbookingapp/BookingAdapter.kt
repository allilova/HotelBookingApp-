package com.example.hotelbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(private val list: List<Booking>) :
    RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image:      ImageView = view.findViewById(R.id.bookingImage)
        val tvName:     TextView  = view.findViewById(R.id.bookingHotelName)
        val tvDates:    TextView  = view.findViewById(R.id.bookingDates)
        val tvPrice:    TextView  = view.findViewById(R.id.bookingPrice)
        val tvBookedAt: TextView  = view.findViewById(R.id.bookingBookedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val b = list[position]
        val ctx = holder.itemView.context

        val resolved = HotelRepository.getHotels(ctx).find { it.id == b.hotelId }
        val displayName = resolved?.name ?: b.hotelName
        val displayCity = resolved?.city ?: b.hotelCity

        holder.tvName.text  = "$displayName — $displayCity"
        holder.tvDates.text = "${b.checkIn}  →  ${b.checkOut}"

        val nights = calculateNights(b.checkIn, b.checkOut)
        val total  = nights.toDouble() * b.pricePerNight

        holder.tvPrice.text    = "${nights} нощ/и — ${"%.2f".format(total)} лв."
        holder.tvBookedAt.text = ctx.getString(R.string.booked_at, sdf.format(Date(b.bookedAt)))

        Glide.with(ctx)
            .load(b.hotelImageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.image)
    }

    private fun calculateNights(checkIn: String, checkOut: String): Long {
        return try {
            val fmt  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val diff = fmt.parse(checkOut)!!.time - fmt.parse(checkIn)!!.time
            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        } catch (e: Exception) { 1L }
    }
}