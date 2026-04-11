package com.example.hotelbookingapp

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database — after Phase 2 this only contains:
 *  - FavoriteHotel  (stays local — personal device preference)
 *  - Booking        (will move to Firestore in Phase 3)
 *  - CustomHotel    (will move to Firestore in Phase 4)
 *
 * User has been removed because authentication and user profiles
 * are now managed by Firebase Auth + Firestore.
 *
 * Version history:
 *  1 → 2: Added imageUrl to favorite_hotels
 *  2 → 3: Added bookings table
 *  3 → 4: Added users table (NOW REMOVED in version 8)
 *  4 → 5: Added hotelId to favorite_hotels and bookings
 *  5 → 6: Added role column to users
 *  6 → 7: Added custom_hotels table
 *  7 → 8: Dropped users table (moved to Firestore)
 */
@Database(
    entities = [FavoriteHotel::class, Booking::class, CustomHotel::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hotelDao(): HotelDao
    abstract fun bookingDao(): BookingDao
    abstract fun customHotelDao(): CustomHotelDao
    // userDao() removed — use FirebaseAuthManager instead

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE favorite_hotels ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE bookings ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'GUEST'"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_hotels (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ownerUserId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        city TEXT NOT NULL,
                        price REAL NOT NULL,
                        rating REAL NOT NULL DEFAULT 0,
                        imageUrl TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        latitude REAL NOT NULL DEFAULT 0.0,
                        longitude REAL NOT NULL DEFAULT 0.0,
                        isAvailable INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration 7 → 8: Drop the local users table.
         * Users are now authenticated via Firebase Auth and their profiles
         * are stored in Firestore. The local users table is no longer needed.
         *
         * We keep all previous migrations (1-7) so that users upgrading
         * from any older version of the app go through each migration
         * step in order without losing their favorites or bookings.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS users")
            }
        }
    }
}