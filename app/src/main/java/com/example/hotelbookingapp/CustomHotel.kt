package com.example.hotelbookingapp


data class CustomHotel(
    // Firestore document ID — empty when creating, filled after Firestore generates it
    val firestoreId:  String  = "",

    // Firebase UID of the host who created this hotel
    // Changed from Int to String in Phase 4 (was a Room autoincrement ID before)
    val ownerUserId:  String  = "",

    val name:         String  = "",
    val city:         String  = "",
    val price:        Double  = 0.0,
    val rating:       Float   = 0f,
    val imageUrl:     String  = "",
    val description:  String  = "",
    val latitude:     Double  = 0.0,
    val longitude:    Double  = 0.0,
    val isAvailable:  Boolean = true,
    val createdAt:    Long    = System.currentTimeMillis()
)