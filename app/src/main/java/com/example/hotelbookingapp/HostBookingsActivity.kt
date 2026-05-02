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


class HostBookingsActivity : AppCompatActivity() {

    private lateinit var adapter: HostBookingAdapter
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvPending: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_bookings)

        supportActionBar?.apply {
            title = getString(R.string.title_host_bookings)
            setDisplayHomeAsUpEnabled(true)
        }

        rv        = findViewById(R.id.rvHostBookings)
        tvEmpty   = findViewById(R.id.tvEmptyHostBookings)
        tvPending = findViewById(R.id.tvPendingCount)

        rv.layoutManager = LinearLayoutManager(this)

        adapter = HostBookingAdapter(
            list      = emptyList(),
            context   = this,
            onConfirm = { booking -> confirmAction(booking, BookingStatus.CONFIRMED) },
            onCancel  = { booking -> confirmAction(booking, BookingStatus.CANCELLED) }
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


        rv.visibility      = View.GONE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val bookings = BookingRepository.getBookingsForHost(uid)

                if (bookings.isEmpty()) {
                    showEmpty(getString(R.string.no_host_bookings))
                    updatePendingCount(0)
                } else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility      = View.VISIBLE
                    adapter.updateData(bookings)

                    val pendingCount = bookings.count {
                        it.status == BookingStatus.PENDING.name
                    }
                    updatePendingCount(pendingCount)
                }
            } catch (e: Exception) {
                android.util.Log.e("HostBookings", "Failed to load bookings: ${e.message}", e)
                showEmpty(getString(R.string.error_loading_bookings))
                Toast.makeText(
                    this@HostBookingsActivity,
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

    private fun updatePendingCount(count: Int) {
        tvPending.text = if (count == 0) {
            getString(R.string.pending_count_zero)
        } else {
            getString(R.string.pending_count, count)
        }
    }

    private fun confirmAction(booking: Booking, newStatus: BookingStatus) {
        val title = when (newStatus) {
            BookingStatus.CONFIRMED -> getString(R.string.confirm_booking_dialog_title)
            BookingStatus.CANCELLED -> getString(R.string.cancel_booking_title)
            else -> ""
        }

        val message = when (newStatus) {
            BookingStatus.CONFIRMED ->
                getString(
                    R.string.confirm_booking_dialog_msg,
                    booking.guestUserName.ifBlank { booking.hotelName },
                    booking.hotelName
                )
            BookingStatus.CANCELLED ->
                getString(R.string.cancel_booking_msg, booking.hotelName)
            else -> ""
        }

        val positiveLabel = when (newStatus) {
            BookingStatus.CONFIRMED -> getString(R.string.btn_confirm_booking)
            BookingStatus.CANCELLED -> getString(R.string.btn_confirm_cancel)
            else                    -> getString(R.string.btn_confirm_booking)
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveLabel) { _, _ ->
                lifecycleScope.launch {
                    try {
                        BookingRepository.updateStatus(
                            firestoreId = booking.firestoreId,
                            newStatus   = newStatus,
                            booking     = booking
                        )

                        val successMsg = when (newStatus) {
                            BookingStatus.CONFIRMED -> getString(R.string.booking_confirmed_success)
                            BookingStatus.CANCELLED -> getString(R.string.booking_cancelled_success)
                            else -> ""
                        }
                        Toast.makeText(this@HostBookingsActivity, successMsg, Toast.LENGTH_SHORT).show()

                        loadBookings()

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@HostBookingsActivity,
                            getString(R.string.error_update_booking_status),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}