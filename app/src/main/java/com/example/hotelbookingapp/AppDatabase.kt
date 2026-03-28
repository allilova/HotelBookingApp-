package com.example.hotelbookingapp

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteHotel::class, Booking::class, User::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hotelDao(): HotelDao
    abstract fun bookingDao(): BookingDao
    abstract fun userDao(): UserDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorite_hotels ADD COLUMN imageUrl TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fullName TEXT NOT NULL,
                        email TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        UNIQUE(email)
                    )
                """.trimIndent())
            }
        }

        // Adds hotelId to favorite_hotels and bookings so adapters can
        // re-resolve the hotel name/city in whatever locale is currently active,
        // rather than displaying the language that was active at save time.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // favorite_hotels: add hotelId (default 0 for pre-existing rows)
                db.execSQL(
                    "ALTER TABLE favorite_hotels ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 0"
                )
                // bookings: add hotelId (default 0 for pre-existing rows)
                db.execSQL(
                    "ALTER TABLE bookings ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}