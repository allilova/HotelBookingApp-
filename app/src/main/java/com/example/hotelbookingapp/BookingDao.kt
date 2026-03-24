package com.example.hotelbookingapp

import androidx.room.*

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY bookedAt DESC")
    fun getAllBookings(): List<Booking>

    @Insert
    fun insertBooking(booking: Booking)

    @Delete
    fun deleteBooking(booking: Booking)
}