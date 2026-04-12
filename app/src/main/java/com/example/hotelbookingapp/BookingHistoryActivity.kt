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

        adapter = BookingAdapter(
            list            = emptyList(),
            activityContext = this,
            onCancelClick   = { booking -> confirmCancel(booking) }
        )
        rv.adapter = adapter

        loadBookings(rv, tvEmpty)
    }

    private fun loadBookings(rv: RecyclerView, tvEmpty: TextView) {
        val uid = FirebaseAuthManager.currentUid
        if (uid == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text       = getString(R.string.error_not_logged_in)
            rv.visibility      = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
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
     * Shows a confirmation dialog then cancels the booking in Firestore.
     * Passes the full booking object to updateStatus() so the notification
     * to the host can be sent correctly.
     */
    private fun confirmCancel(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cancel_booking_title))
            .setMessage(getString(R.string.cancel_booking_msg, booking.hotelName))
            .setPositiveButton(getString(R.string.btn_confirm_cancel)) { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Pass the full booking object — needed so BookingRepository
                        // can read booking.hostUserId and send the host a notification
                        BookingRepository.updateStatus(
                            firestoreId = booking.firestoreId,
                            newStatus   = BookingStatus.CANCELLED,
                            booking     = booking
                        )
                        Toast.makeText(
                            this@BookingHistoryActivity,
                            getString(R.string.booking_cancelled_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        // Reload the list so the status badge updates immediately
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