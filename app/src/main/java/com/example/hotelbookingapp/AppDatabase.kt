package com.example.hotelbookingapp

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FavoriteHotel::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hotelDao(): HotelDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE favorite_hotels ADD COLUMN imageUrl TEXT"
                )
            }
        }
    }
}