package com.example.hotelbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.rvHotels)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val allHotels = HotelRepository.getHotels()
        val adapter = HotelAdapter(allHotels)
        recyclerView.adapter = adapter


        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (!newText.isNullOrEmpty()) {
                    val q = newText.lowercase()
                    allHotels.filter {
                        it.name.lowercase().contains(q) || it.city.lowercase().contains(q)
                    }
                } else {
                    allHotels
                }
                adapter.filterList(filtered)
                return true
            }
        })


        val sharedPref = getSharedPreferences("HotelAppPrefs", android.content.Context.MODE_PRIVATE)
        val points = sharedPref.getInt("user_points", 0) + 10
        sharedPref.edit().putInt("user_points", points).apply()

        findViewById<TextView>(R.id.tvPoints).text = "Бонус точки: $points"

        if (points >= 100) {
            android.widget.Toast.makeText(
                this,
                "Ти си VIP клиент! Отключи 15% отстъпка!",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }


        findViewById<ImageButton>(R.id.btnGoToFavorites).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }
}