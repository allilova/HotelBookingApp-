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
     * Includes validation for firestoreId and detailed logging.
     */
    private fun confirmCancel(booking: Booking) {
        // Log the booking details so we can verify firestoreId is not empty
        android.util.Log.d("BookingCancel",
            "Attempting to cancel booking: firestoreId='${booking.firestoreId}' " +
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
                        android.util.Log.d("BookingCancel",
                            "Calling updateStatus with firestoreId='${booking.firestoreId}'"
                        )

                        BookingRepository.updateStatus(
                            firestoreId = booking.firestoreId,
                            newStatus   = BookingStatus.CANCELLED,
                            booking     = booking
                        )

                        android.util.Log.d("BookingCancel", "updateStatus succeeded")

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
                        android.util.Log.e("BookingCancel",
                            "updateStatus FAILED: ${e.message}", e)

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