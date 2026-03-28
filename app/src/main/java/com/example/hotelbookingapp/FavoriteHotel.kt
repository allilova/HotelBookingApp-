package com.example.hotelbookingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_hotels")
data class FavoriteHotel(
    @PrimaryKey val id: Int,
    val hotelId: Int,          // used to re-resolve name/city in the active locale
    val name: String,          // kept as fallback only
    val city: String,          // kept as fallback only
    val imageUrl: String?
)