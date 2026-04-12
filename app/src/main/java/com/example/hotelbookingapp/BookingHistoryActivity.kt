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
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        rv      = findViewById(R.id.rvBookings)
        tvEmpty = findViewById(R.id.tvEmptyBookings)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = BookingAdapter(
            list            = emptyList(),
            activityContext = this,
            onCancelClick   = { booking -> confirmCancel(booking) }
        )
        rv.adapter = adapter

        loadBookings()
    }

    private fun loadBookings() {
        val uid = FirebaseAuthManager.currentUid
        if (uid == null) {
            showEmpty(getString(R.string.error_not_logged_in))
            return
        }

        // Show loading state
        rv.visibility      = View.GONE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val bookings = BookingRepository.getBookingsForGuest(uid)

                if (bookings.isEmpty()) {
                    showEmpty(getString(R.string.no_bookings))
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility      = View.VISIBLE
                    adapter.updateData(bookings)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookingHistory", "Failed to load bookings: ${e.message}", e)
                showEmpty(getString(R.string.error_loading_bookings))
                Toast.makeText(
                    this@BookingHistoryActivity,
                    getString(R.string.error_loading_bookings),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showEmpty(message: String) {
        rv.visibility      = View.GONE
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text       = message
    }

    private fun confirmCancel(booking: Booking) {
        android.util.Log.d("BookingCancel",
            "Attempting to cancel: firestoreId='${booking.firestoreId}' " +
                    "hotelName='${booking.hotelName}' status='${booking.status}'"
        )

        if (booking.firestoreId.isBlank()) {
            Toast.makeText(
                this,
                "Грешка: резервацията няма валиден ID. Моля, направете нова резервация.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cancel_booking_title))
            .setMessage(getString(R.string.cancel_booking_msg, booking.hotelName))
            .setPositiveButton(getString(R.string.btn_confirm_cancel)) { _, _ ->
                lifecycleScope.launch {
                    try {
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
                        loadBookings()
                    } catch (e: Exception) {
                        android.util.Log.e("BookingCancel", "Cancel FAILED: ${e.message}", e)
                        Toast.makeText(
                            this@BookingHistoryActivity,
                            "Грешка: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}