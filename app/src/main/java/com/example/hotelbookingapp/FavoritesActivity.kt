package com.example.hotelbookingapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesActivity : AppCompatActivity() {

    private lateinit var adapter: FavoriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val db = DatabaseProvider.get(this)
        val rv = findViewById<RecyclerView>(R.id.rvFavorites)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = FavoriteAdapter(emptyList(), this) { hotel ->
            lifecycleScope.launch {
                try {
                    // Room DELETE must run on IO dispatcher
                    withContext(Dispatchers.IO) {
                        db.hotelDao().deleteFavorite(hotel)
                    }
                    refreshData(db)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@FavoritesActivity,
                        getString(R.string.error_delete_favorite),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        rv.adapter = adapter
        refreshData(db)
    }

    private fun refreshData(db: AppDatabase) {
        lifecycleScope.launch {
            try {
                // Room SELECT must run on IO dispatcher
                val favorites = withContext(Dispatchers.IO) {
                    db.hotelDao().getAllFavorites()
                }
                val rv      = findViewById<RecyclerView>(R.id.rvFavorites)
                val tvEmpty = findViewById<TextView>(R.id.tvEmptyMessage)

                if (favorites.isEmpty()) {
                    rv.visibility      = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rv.visibility      = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                    adapter.updateData(favorites)
                }
            } catch (e: Exception) {
                val tvEmpty = findViewById<TextView>(R.id.tvEmptyMessage)
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text       = getString(R.string.error_loading_favorites)
                Toast.makeText(
                    this@FavoritesActivity,
                    getString(R.string.error_loading_favorites),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}