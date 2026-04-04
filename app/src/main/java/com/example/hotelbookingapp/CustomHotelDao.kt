package com.example.hotelbookingapp

import androidx.room.*

@Dao
interface CustomHotelDao {

    @Query("SELECT * FROM custom_hotels ORDER BY createdAt DESC")
    fun getAllCustomHotels(): List<CustomHotel>

    @Query("SELECT * FROM custom_hotels WHERE ownerUserId = :userId ORDER BY createdAt DESC")
    fun getHotelsByOwner(userId: Int): List<CustomHotel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHotel(hotel: CustomHotel): Long

    @Update
    fun updateHotel(hotel: CustomHotel)

    @Delete
    fun deleteHotel(hotel: CustomHotel)

    @Query("SELECT * FROM custom_hotels WHERE id = :id LIMIT 1")
    fun getHotelById(id: Int): CustomHotel?
}