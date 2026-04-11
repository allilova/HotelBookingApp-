package com.example.hotelbookingapp



data class Booking(
    val firestoreId:   String = "",
    val hotelId:       Int    = 0,
    val hotelName:     String = "",
    val hotelCity:     String = "",
    val hotelImageUrl: String = "",
    val checkIn:       String = "",
    val checkOut:      String = "",
    val pricePerNight: Double = 0.0,
    val guestCount:    Int          = 1,
    val guestNames:    List<String> = emptyList(),
    val guestUserId:   String = "",
    val guestUserName: String = "",
    val hostUserId:    String = "",
    val status:        String = BookingStatus.PENDING.name,
    val bookedAt:      Long   = System.currentTimeMillis()
)

enum class BookingStatus {
    PENDING,    // Created by guest, waiting for host action
    CONFIRMED,  // Host has confirmed the booking
    CANCELLED   // Cancelled by either guest or host
}