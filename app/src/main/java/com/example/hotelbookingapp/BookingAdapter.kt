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

        // Re-resolve name and city from HotelRepository so they always reflect
        // the active locale, regardless of what language was active at booking time.
        val resolved = HotelRepository.getHotels(ctx).find { it.id == b.hotelId }
        val displayName = resolved?.name ?: b.hotelName
        val displayCity = resolved?.city ?: b.hotelCity

        holder.tvName.text  = "$displayName — $displayCity"
        holder.tvDates.text = "${b.checkIn}  →  ${b.checkOut}"

        val nights = calculateNights(b.checkIn, b.checkOut)
        val total  = nights * b.pricePerNight
        // Use string resource so "нощ/и" / "night(s)" and "лв." / "BGN" are localised
        holder.tvPrice.text    = ctx.getString(R.string.nights_total, nights, total)
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