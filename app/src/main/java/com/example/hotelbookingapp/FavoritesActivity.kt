package com.example.hotelbookingapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: FavoriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "hotel-db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        val recyclerView = findViewById<RecyclerView>(R.id.rvFavorites)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FavoriteAdapter(emptyList()) { hotel ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { db.hotelDao().deleteFavorite(hotel) }
                refreshData()
            }
        }

        recyclerView.adapter = adapter
        refreshData()
    }

    private fun refreshData() {
        lifecycleScope.launch {
            val favorites = withContext(Dispatchers.IO) { db.hotelDao().getAllFavorites() }

            val rvFavorites    = findViewById<RecyclerView>(R.id.rvFavorites)
            val tvEmptyMessage = findViewById<TextView>(R.id.tvEmptyMessage)

            if (favorites.isEmpty()) {
                rvFavorites.visibility    = View.GONE
                tvEmptyMessage.visibility = View.VISIBLE
            } else {
                rvFavorites.visibility    = View.VISIBLE
                tvEmptyMessage.visibility = View.GONE
                adapter.updateData(favorites)
            }
        }
    }
}