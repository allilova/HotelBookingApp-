package com.example.hotelbookingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hotelName: String,
    val hotelCity: String,
    val hotelImageUrl: String?,
    val checkIn: String,   // stored as "dd/MM/yyyy"
    val checkOut: String,
    val pricePerNight: Double,
    val bookedAt: Long = System.currentTimeMillis()
)