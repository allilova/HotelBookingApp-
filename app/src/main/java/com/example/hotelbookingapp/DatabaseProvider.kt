package com.example.hotelbookingapp

import android.content.Context
import android.util.Log
import androidx.room.Room

object DatabaseProvider {

    private const val TAG = "DatabaseProvider"
    private const val DB_NAME = "hotel-db"

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }

    private fun buildDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            // REMOVED fallbackToDestructiveMigration() — it was silently wiping the
            // database whenever a migration was missing, which caused users and hotels
            // to disappear on every app update. If a migration truly fails, Room will
            // now throw an exception so the problem is visible rather than silent data loss.
            .build()
    }
}