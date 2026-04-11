package com.example.hotelbookingapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class BookingHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: BookingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        val rv      = findViewById<RecyclerView>(R.id.rvBookings)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyBookings)
        rv.layoutManager = LinearLayoutManager(this)

        // Create the adapter with a cancel callback.
        // When the guest taps Cancel, we show a confirmation dialog
        // before updating Firestore.
        adapter = BookingAdapter(
            list            = emptyList(),
            activityContext = this,
            onCancelClick   = { booking -> confirmCancel(booking) }
        )
        rv.adapter = adapter

        loadBookings(rv, tvEmpty)
    }

    /**
     * Loads the current user's bookings from Firestore.
     * Shows an empty state if there are no bookings.
     */
    private fun loadBookings(rv: RecyclerView, tvEmpty: TextView) {
        val uid = FirebaseAuthManager.currentUid
        if (uid == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = getString(R.string.error_not_logged_in)
            rv.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                // BookingRepository.getBookingsForGuest() queries Firestore
                // for all bookings where guestUserId == uid
                val bookings = BookingRepository.getBookingsForGuest(uid)

                if (bookings.isEmpty()) {
                    rv.visibility      = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rv.visibility      = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    adapter.updateData(bookings)
                }
            } catch (e: Exception) {
                rv.visibility      = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text       = getString(R.string.error_loading_bookings)
                Toast.makeText(
                    this@BookingHistoryActivity,
                    getString(R.string.error_loading_bookings),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Shows a confirmation dialog before cancelling a booking.
     * If confirmed, updates the booking status to CANCELLED in Firestore
     * and refreshes the list.
     */
    private fun confirmCancel(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cancel_booking_title))
            .setMessage(getString(R.string.cancel_booking_msg, booking.hotelName))
            .setPositiveButton(getString(R.string.btn_confirm_cancel)) { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Update only the status field in Firestore —
                        // all other booking data remains unchanged
                        BookingRepository.updateStatus(
                            booking.firestoreId,
                            BookingStatus.CANCELLED
                        )
                        Toast.makeText(
                            this@BookingHistoryActivity,
                            getString(R.string.booking_cancelled_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        // Refresh the list so the status badge updates immediately
                        val rv      = findViewById<RecyclerView>(R.id.rvBookings)
                        val tvEmpty = findViewById<TextView>(R.id.tvEmptyBookings)
                        loadBookings(rv, tvEmpty)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@BookingHistoryActivity,
                            getString(R.string.error_cancel_booking),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}