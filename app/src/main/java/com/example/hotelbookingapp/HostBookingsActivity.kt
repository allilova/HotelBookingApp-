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

/**
 * HostBookingsActivity shows all bookings made for the currently logged-in
 * host's hotels.
 *
 * Features:
 *  - Loads all bookings where hostUserId == FirebaseAuthManager.currentUid
 *  - Shows a summary card with the count of PENDING bookings
 *  - Confirm button: changes booking status to CONFIRMED
 *  - Cancel button: changes booking status to CANCELLED
 *  - Both actions show a confirmation dialog before updating Firestore
 *  - List refreshes automatically after each status change
 *
 * Data flow:
 *  HostBookingsActivity
 *    → BookingRepository.getBookingsForHost(uid)   [Firestore read]
 *    → BookingRepository.updateStatus(id, status)  [Firestore write]
 *    → reloads list
 */
class HostBookingsActivity : AppCompatActivity() {

    private lateinit var adapter: HostBookingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_bookings)

        supportActionBar?.apply {
            title = getString(R.string.title_host_bookings)
            setDisplayHomeAsUpEnabled(true)
        }

        val rv          = findViewById<RecyclerView>(R.id.rvHostBookings)
        val tvEmpty     = findViewById<TextView>(R.id.tvEmptyHostBookings)
        val tvPending   = findViewById<TextView>(R.id.tvPendingCount)

        rv.layoutManager = LinearLayoutManager(this)

        // Create the adapter with confirm and cancel callbacks.
        // Each callback shows a confirmation dialog before updating Firestore.
        adapter = HostBookingAdapter(
            list      = emptyList(),
            context   = this,
            onConfirm = { booking -> confirmAction(booking, BookingStatus.CONFIRMED) },
            onCancel  = { booking -> confirmAction(booking, BookingStatus.CANCELLED) }
        )
        rv.adapter = adapter

        // Load bookings from Firestore on first open
        loadBookings(rv, tvEmpty, tvPending)
    }

    /**
     * Loads all bookings for this host's hotels from Firestore.
     *
     * Query: bookings WHERE hostUserId == currentUid ORDER BY bookedAt DESC
     *
     * After loading:
     *  - Updates the RecyclerView
     *  - Updates the pending count summary card
     *  - Shows the empty state if there are no bookings
     */
    private fun loadBookings(
        rv:        RecyclerView,
        tvEmpty:   TextView,
        tvPending: TextView
    ) {
        val uid = FirebaseAuthManager.currentUid

        // If not logged in, show empty state with an error message
        if (uid == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text       = getString(R.string.error_not_logged_in)
            rv.visibility      = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                // Fetch all bookings for this host from Firestore
                val bookings = BookingRepository.getBookingsForHost(uid)

                if (bookings.isEmpty()) {
                    // No bookings yet — show empty state
                    rv.visibility      = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    // Show the list
                    rv.visibility      = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    adapter.updateData(bookings)

                    // Count how many bookings are still PENDING
                    // and update the summary card
                    val pendingCount = bookings.count {
                        it.status == BookingStatus.PENDING.name
                    }
                    updatePendingCount(tvPending, pendingCount)
                }
            } catch (e: Exception) {
                // Firestore read failed — show error in empty state
                rv.visibility      = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text       = getString(R.string.error_loading_bookings)
                Toast.makeText(
                    this@HostBookingsActivity,
                    getString(R.string.error_loading_bookings),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Updates the summary card text to show how many bookings need attention.
     *
     * @param tvPending The TextView in the summary card.
     * @param count     The number of PENDING bookings.
     */
    private fun updatePendingCount(tvPending: TextView, count: Int) {
        tvPending.text = if (count == 0) {
            getString(R.string.pending_count_zero)
        } else {
            getString(R.string.pending_count, count)
        }
    }

    /**
     * Shows a confirmation dialog before changing a booking's status.
     *
     * @param booking   The booking to update.
     * @param newStatus The new status to set (CONFIRMED or CANCELLED).
     *
     * If the host confirms:
     *  1. Call BookingRepository.updateStatus() to write to Firestore
     *  2. Show a success Toast
     *  3. Reload the list so the status badge updates immediately
     */
    private fun confirmAction(booking: Booking, newStatus: BookingStatus) {
        // Build dialog title and message based on the action
        val title = when (newStatus) {
            BookingStatus.CONFIRMED ->
                getString(R.string.confirm_booking_dialog_title)
            BookingStatus.CANCELLED ->
                getString(R.string.cancel_booking_title)
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
                        // Update only the "status" field in the Firestore document.
                        // All other booking data stays intact.
                        BookingRepository.updateStatus(
                            firestoreId = booking.firestoreId,
                            newStatus   = newStatus,
                            booking     = booking  // ← needed to look up guest's FCM token
                        )

                        // Show success message
                        val successMsg = when (newStatus) {
                            BookingStatus.CONFIRMED ->
                                getString(R.string.booking_confirmed_success)
                            BookingStatus.CANCELLED ->
                                getString(R.string.booking_cancelled_success)
                            else -> ""
                        }
                        Toast.makeText(
                            this@HostBookingsActivity,
                            successMsg,
                            Toast.LENGTH_SHORT
                        ).show()

                        // Reload the list so the status badge updates immediately
                        val rv        = findViewById<RecyclerView>(R.id.rvHostBookings)
                        val tvEmpty   = findViewById<TextView>(R.id.tvEmptyHostBookings)
                        val tvPending = findViewById<TextView>(R.id.tvPendingCount)
                        loadBookings(rv, tvEmpty, tvPending)

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