package com.example.hotelbookingapp

import androidx.room.*

@Dao
interface HotelDao {
    // Вземаме всичко от правилната таблица: favorite_hotels
    @Query("SELECT * FROM favorite_hotels")
    fun getAllFavorites(): List<FavoriteHotel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFavorite(hotel: FavoriteHotel)

    @Delete
    fun deleteFavorite(hotel: FavoriteHotel)
}