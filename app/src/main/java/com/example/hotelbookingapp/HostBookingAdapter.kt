package com.example.hotelbookingapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*


class HostBookingAdapter(
    private var list: List<Booking>,
    private val context: Context,
    private val onConfirm: (Booking) -> Unit,
    private val onCancel:  (Booking) -> Unit
) : RecyclerView.Adapter<HostBookingAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHotelName:  TextView = view.findViewById(R.id.tvHostBookingHotelName)
        val tvGuestNames: TextView = view.findViewById(R.id.tvHostBookingGuestNames)
        val tvDates:      TextView = view.findViewById(R.id.tvHostBookingDates)
        val tvPrice:      TextView = view.findViewById(R.id.tvHostBookingPrice)
        val tvBookedAt:   TextView = view.findViewById(R.id.tvHostBookingBookedAt)
        val tvStatus:     TextView = view.findViewById(R.id.tvHostBookingStatus)
        val btnConfirm:   Button   = view.findViewById(R.id.btnHostConfirm)
        val btnCancel:    Button   = view.findViewById(R.id.btnHostCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.item_host_booking, parent, false)
        )

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val b = list[position]

        // ── Hotel name ────────────────────────────────────────────────────────
        holder.tvHotelName.text = b.hotelName

        // ── Guest info ─────────────────────────────────────────────────────────
        val namesDisplay = if (b.guestNames.isNotEmpty()) {
            b.guestNames.joinToString(", ")
        } else {
            b.guestUserName.ifBlank {
                context.getString(R.string.guest_name_fallback, 1)
            }
        }
        holder.tvGuestNames.text = context.getString(
            R.string.booking_guests_display,
            b.guestCount,
            namesDisplay
        )

        // ── Dates ─────────────────────────────────────────────────────────────
        holder.tvDates.text = "${b.checkIn}  →  ${b.checkOut}"

        // ── Price ─────────────────────────────────────────────────────────────
        val nights = calculateNights(b.checkIn, b.checkOut)
        val total  = nights * b.pricePerNight * b.guestCount
        holder.tvPrice.text = context.getString(
            R.string.nights_total, nights.toInt(), total
        )

        // ── Booked at ─────────────────────────────────────────────────────────
        holder.tvBookedAt.text = context.getString(
            R.string.booked_at, sdf.format(Date(b.bookedAt))
        )

        // ── Status badge ──────────────────────────────────────────────────────
        val status = try {
            BookingStatus.valueOf(b.status)
        } catch (e: IllegalArgumentException) {
            BookingStatus.PENDING
        }

        holder.tvStatus.text = when (status) {
            BookingStatus.PENDING   -> context.getString(R.string.status_pending)
            BookingStatus.CONFIRMED -> context.getString(R.string.status_confirmed)
            BookingStatus.CANCELLED -> context.getString(R.string.status_cancelled)
        }


        val badgeColor = when (status) {
            BookingStatus.PENDING   ->
                ContextCompat.getColor(context, R.color.text_secondary)
            BookingStatus.CONFIRMED ->
                ContextCompat.getColor(context, R.color.teal_available)
            BookingStatus.CANCELLED ->
                ContextCompat.getColor(context, R.color.red_unavailable)
        }
        val bg = holder.tvStatus.background.mutate()
        bg.setTint(badgeColor)
        holder.tvStatus.background = bg

        // ── Action buttons ────────────────────────────────────────────────────


        holder.btnConfirm.visibility = when (status) {
            BookingStatus.PENDING -> View.VISIBLE
            else                  -> View.GONE
        }
        holder.btnConfirm.setOnClickListener { onConfirm(b) }


        holder.btnCancel.visibility = when (status) {
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED -> View.VISIBLE
            BookingStatus.CANCELLED -> View.GONE
        }
        holder.btnCancel.setOnClickListener { onCancel(b) }
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
        } catch (e: Exception) {
            1L
        }
    }
}