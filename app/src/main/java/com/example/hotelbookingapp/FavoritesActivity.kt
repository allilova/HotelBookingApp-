package com.example.hotelbookingapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room

class FavoritesActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var adapter: FavoriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "hotel-db")
            .allowMainThreadQueries().build()

        val recyclerView = findViewById<RecyclerView>(R.id.rvFavorites)
        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = FavoriteAdapter(mutableListOf()) { hotel ->
            db.hotelDao().deleteFavorite(hotel)
            refreshData()
        }

        recyclerView.adapter = adapter
        refreshData()
    }

    private fun refreshData() {
        val favorites = db.hotelDao().getAllFavorites()

        val rvFavorites = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFavorites)
        val tvEmptyMessage = findViewById<android.widget.TextView>(R.id.tvEmptyMessage)

        if (favorites.isEmpty()) {

            rvFavorites.visibility = android.view.View.GONE
            tvEmptyMessage.visibility = android.view.View.VISIBLE
        } else {

            rvFavorites.visibility = android.view.View.VISIBLE
            tvEmptyMessage.visibility = android.view.View.GONE
            adapter.updateData(favorites)
        }
    }
}