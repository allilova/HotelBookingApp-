package com.example.hotelbookingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hotelId: Int,              // used to re-resolve name/city in the active locale
    val hotelName: String,         // kept as fallback only
    val hotelCity: String,         // kept as fallback only
    val hotelImageUrl: String?,
    val checkIn: String,           // stored as "dd/MM/yyyy"
    val checkOut: String,
    val pricePerNight: Double,
    val bookedAt: Long = System.currentTimeMillis()
)