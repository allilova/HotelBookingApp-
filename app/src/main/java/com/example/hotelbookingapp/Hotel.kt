package com.example.hotelbookingapp


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

    val ownerUserId: String  = ""
)