package com.example.hotelbookingapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private val list: List<Booking>,
    private val activityContext: Context
) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Pre-resolve hotel names once from the activity context to guarantee
    // the correct locale is used.
    private val resolvedHotels: List<Hotel> = HotelRepository.getHotels(activityContext)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image:      ImageView = view.findViewById(R.id.bookingImage)
        val tvName:     TextView  = view.findViewById(R.id.bookingHotelName)
        val tvDates:    TextView  = view.findViewById(R.id.bookingDates)
        val tvPrice:    TextView  = view.findViewById(R.id.bookingPrice)
        val tvBookedAt: TextView  = view.findViewById(R.id.bookingBookedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(activityContext)
                .inflate(R.layout.item_booking, parent, false)
        )

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val b = list[position]

        val resolved = resolvedHotels.find { it.id == b.hotelId }
        val displayName = resolved?.name ?: b.hotelName
        val displayCity = resolved?.city ?: b.hotelCity

        holder.tvName.text  = "$displayName — $displayCity"
        holder.tvDates.text = "${b.checkIn}  →  ${b.checkOut}"

        val nights = calculateNights(b.checkIn, b.checkOut)
        val total  = nights * b.pricePerNight

        holder.tvPrice.text    = activityContext.getString(R.string.nights_total, nights.toInt(), total)
        holder.tvBookedAt.text = activityContext.getString(R.string.booked_at, sdf.format(Date(b.bookedAt)))

        Glide.with(activityContext)
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