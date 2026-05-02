package com.example.hotelbookingapp


data class CustomHotel(

    val firestoreId:  String  = "",


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