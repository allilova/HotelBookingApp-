package com.example.hotelbookingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_hotels")
data class FavoriteHotel(
    @PrimaryKey val id: Int,
    val name: String,
    val city: String,
    val imageUrl: String?
)