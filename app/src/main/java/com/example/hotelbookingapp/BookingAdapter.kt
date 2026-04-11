package com.example.hotelbookingapp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for the guest's booking history list.
 */
class BookingAdapter(
    private var list: List<Booking>,
    private val activityContext: Context,
    private val onCancelClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image:        ImageView = view.findViewById(R.id.bookingImage)
        val tvName:       TextView  = view.findViewById(R.id.bookingHotelName)
        val tvDates:      TextView  = view.findViewById(R.id.bookingDates)
        val tvPrice:      TextView  = view.findViewById(R.id.bookingPrice)
        val tvGuests:     TextView  = view.findViewById(R.id.bookingGuests)
        val tvBookedAt:   TextView  = view.findViewById(R.id.bookingBookedAt)
        val tvStatus:     TextView  = view.findViewById(R.id.bookingStatus)
        val btnCancel:    Button    = view.findViewById(R.id.btnCancelBooking)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(activityContext)
                .inflate(R.layout.item_booking, parent, false)
        )

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val b = list[position]

        // ── Hotel name + city (With Coroutine Resolution) ─────────────────────
        // 1. Set fallback values immediately
        holder.tvName.text = "${b.hotelName} — ${b.hotelCity}"

        // 2. Resolve in coroutine to avoid blocking the UI thread
        CoroutineScope(Dispatchers.Main).launch {
            val resolved = withContext(Dispatchers.IO) {
                HotelRepository.resolve(activityContext, b.hotelId, b.hotelName)
            }
            val displayName = resolved?.name ?: b.hotelName
            val displayCity = resolved?.city ?: b.hotelCity
            holder.tvName.text = "$displayName — $displayCity"
        }

        // ── Dates + price ─────────────────────────────────────────────────────
        holder.tvDates.text = "${b.checkIn}  →  ${b.checkOut}"

        val nights = calculateNights(b.checkIn, b.checkOut)
        val total  = nights * b.pricePerNight * b.guestCount
        holder.tvPrice.text = activityContext.getString(
            R.string.nights_total, nights.toInt(), total
        )

        // ── Guest info ────────────────────────────────────────────────────────
        val namesDisplay = if (b.guestNames.isNotEmpty()) {
            b.guestNames.joinToString(", ")
        } else {
            activityContext.getString(R.string.guest_name_fallback, 1)
        }
        holder.tvGuests.text = activityContext.getString(
            R.string.booking_guests_display, b.guestCount, namesDisplay
        )

        // ── Booked at ─────────────────────────────────────────────────────────
        holder.tvBookedAt.text = activityContext.getString(
            R.string.booked_at, sdf.format(Date(b.bookedAt))
        )

        // ── Image ─────────────────────────────────────────────────────────────
        Glide.with(activityContext)
            .load(b.hotelImageUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.image)

        // ── Status badge ──────────────────────────────────────────────────────
        val status = try {
            BookingStatus.valueOf(b.status)
        } catch (e: IllegalArgumentException) {
            BookingStatus.PENDING
        }

        holder.tvStatus.text = when (status) {
            BookingStatus.PENDING   -> activityContext.getString(R.string.status_pending)
            BookingStatus.CONFIRMED -> activityContext.getString(R.string.status_confirmed)
            BookingStatus.CANCELLED -> activityContext.getString(R.string.status_cancelled)
        }

        val badgeColor = when (status) {
            BookingStatus.PENDING   ->
                ContextCompat.getColor(activityContext, R.color.text_secondary)
            BookingStatus.CONFIRMED ->
                ContextCompat.getColor(activityContext, R.color.teal_available)
            BookingStatus.CANCELLED ->
                ContextCompat.getColor(activityContext, R.color.red_unavailable)
        }

        val bg = holder.tvStatus.background.mutate()
        bg.setTint(badgeColor)
        holder.tvStatus.background = bg

        // ── Cancel button ─────────────────────────────────────────────────────
        val canCancel = status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED
        holder.btnCancel.visibility = if (canCancel) View.VISIBLE else View.GONE
        holder.btnCancel.setOnClickListener { onCancelClick(b) }
    }

    fun updateData(newList: List<Booking>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun calculateNights(checkIn: String, checkOut: String): Long {
        return try {
            val fmt  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val diff = fmt.parse(checkOut)!!.time - fmt.parse(checkIn)!!.time
            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        } catch (e: Exception) { 1L }
    }
}