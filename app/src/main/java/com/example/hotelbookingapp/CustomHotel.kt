package com.example.hotelbookingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A hotel listing created by a HOST user.
 * Stored locally in Room; merged with the static hotel list at runtime.
 */
@Entity(tableName = "custom_hotels")
data class CustomHotel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerUserId: Int,          // the HOST who created this listing
    val name: String,
    val city: String,
    val price: Double,
    val rating: Float = 0f,
    val imageUrl: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isAvailable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)