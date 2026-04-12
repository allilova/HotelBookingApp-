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

/**
 * Adapter for the host's booking management list (HostBookingsActivity).
 *
 * Each item shows:
 *  - Hotel name (which of the host's hotels this booking is for)
 *  - Guest name(s) and count
 *  - Check-in / check-out dates
 *  - Total price (nights × pricePerNight × guestCount)
 *  - Booking timestamp
 *  - Status badge (color-coded: gray=PENDING, green=CONFIRMED, red=CANCELLED)
 *  - Confirm button (PENDING only)
 *  - Cancel button (PENDING and CONFIRMED only)
 *
 * @param onConfirm Called when the host taps "Confirm" on a booking.
 * @param onCancel  Called when the host taps "Cancel" on a booking.
 *                  Both callbacks receive the full Booking object so the
 *                  Activity can call BookingRepository.updateStatus().
 */
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
        // The hotel name is stored directly on the Booking document so we
        // don't need a separate Firestore lookup here.
        holder.tvHotelName.text = b.hotelName

        // ── Guest info ─────────────────────────────────────────────────────────
        // Show "N guest(s): Name1, Name2" format
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
        // Total = nights × price per night × number of guests
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

        // Update the badge background color by tinting the drawable.
        // We mutate() the drawable so other items are not affected.
        val badgeColor = when (status) {
            BookingStatus.PENDING   ->
                ContextCompat.getColor(context, R.color.text_secondary)  // gray
            BookingStatus.CONFIRMED ->
                ContextCompat.getColor(context, R.color.teal_available)  // green
            BookingStatus.CANCELLED ->
                ContextCompat.getColor(context, R.color.red_unavailable) // red
        }
        val bg = holder.tvStatus.background.mutate()
        bg.setTint(badgeColor)
        holder.tvStatus.background = bg

        // ── Action buttons ────────────────────────────────────────────────────

        // Confirm button: only visible for PENDING bookings.
        // Once confirmed, this button disappears and the status badge
        // changes to green.
        holder.btnConfirm.visibility = when (status) {
            BookingStatus.PENDING -> View.VISIBLE
            else                  -> View.GONE
        }
        holder.btnConfirm.setOnClickListener { onConfirm(b) }

        // Cancel button: visible for PENDING and CONFIRMED bookings.
        // Not shown for already-cancelled bookings.
        holder.btnCancel.visibility = when (status) {
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED -> View.VISIBLE
            BookingStatus.CANCELLED -> View.GONE
        }
        holder.btnCancel.setOnClickListener { onCancel(b) }
    }

    /**
     * Replaces the list and refreshes the RecyclerView.
     * Called after each status update so the UI reflects the change immediately.
     */
    fun updateData(newList: List<Booking>) {
        list = newList
        notifyDataSetChanged()
    }

    /**
     * Calculates the number of nights between check-in and check-out.
     * Returns at least 1 to avoid showing 0 nights for same-day bookings.
     */
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