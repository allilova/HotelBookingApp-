package com.example.hotelbookingapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_history)

        val db      = DatabaseProvider.get(this)
        val rv      = findViewById<RecyclerView>(R.id.rvBookings)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyBookings)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            try {
                val bookings = withContext(Dispatchers.IO) {
                    db.bookingDao().getAllBookings()
                }
                if (bookings.isEmpty()) {
                    rv.visibility      = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rv.visibility      = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    // Pass `this@BookingHistoryActivity` so the adapter uses
                    // the activity's locale-correct context for all string lookups.
                    rv.adapter = BookingAdapter(bookings, this@BookingHistoryActivity)
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
}