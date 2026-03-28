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
        return try {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(
                    AppDatabase.MIGRATION_1_2,
                    AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4,
                    AppDatabase.MIGRATION_4_5
                )
                .fallbackToDestructiveMigration()
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build database, retrying without migrations", e)
            context.applicationContext.deleteDatabase(DB_NAME)
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}