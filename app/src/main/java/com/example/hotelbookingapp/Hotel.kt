package com.example.hotelbookingapp

/**
 * Universal hotel model used throughout the UI.
 *
 * Both static hotels and host-created (Firestore) hotels are represented
 * by this class after going through HotelRepository.
 *
 * firestoreId: empty for static hotels, filled for custom (Firestore) hotels.
 *              Passed via Intent to HotelDetailActivity so it can be included
 *              in Booking.hostUserId lookup and future host-specific operations.
 */
data class Hotel(
    val id:          Int,
    val name:        String,
    val city:        String,
    val price:       Double,
    val rating:      Float,
    val imageUrl:    String,
    val description: String,
    val isAvailable: Boolean = true,
    val latitude:    Double  = 0.0,
    val longitude:   Double  = 0.0,
    val firestoreId: String  = "",
    // Firebase UID of the host who created this hotel.
    // Empty string for static hotels (no owner).
    val ownerUserId: String  = ""
)