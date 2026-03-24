package com.example.hotelbookingapp

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteHotel::class, Booking::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hotelDao(): HotelDao
    abstract fun bookingDao(): BookingDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorite_hotels ADD COLUMN imageUrl TEXT")
            }
        }


        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hotelName TEXT NOT NULL,
                        hotelCity TEXT NOT NULL,
                        hotelImageUrl TEXT,
                        checkIn TEXT NOT NULL,
                        checkOut TEXT NOT NULL,
                        pricePerNight REAL NOT NULL,
                        bookedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}