package com.example.hotelbookingapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteHotel::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hotelDao(): HotelDao
}