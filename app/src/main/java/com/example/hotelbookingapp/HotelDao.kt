package com.example.hotelbookingapp

import androidx.room.*

@Dao
interface HotelDao {
    @Query("SELECT * FROM favorite_hotels")
    fun getAllFavorites(): List<FavoriteHotel>

    @Query("SELECT * FROM favorite_hotels WHERE id = :id LIMIT 1")
    fun getFavoriteById(id: Int): FavoriteHotel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFavorite(hotel: FavoriteHotel)

    @Delete
    fun deleteFavorite(hotel: FavoriteHotel)
}