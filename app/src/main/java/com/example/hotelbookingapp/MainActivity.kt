package com.example.hotelbookingapp

import android.content.Intent
import android.os.Bundle
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


        val hotelList = HotelRepository.getHotels()


        val adapter = HotelAdapter(hotelList)


        recyclerView.adapter = adapter

        // Вътре в onCreate на MainActivity.kt:

        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        val allHotels = HotelRepository.getHotels() // Вземаме пълния списък

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = mutableListOf<Hotel>()

                if (!newText.isNullOrEmpty()) {
                    val searchText = newText.lowercase()
                    for (hotel in allHotels) {
                        // Проверяваме името или града
                        if (hotel.name.lowercase().contains(searchText) ||
                            hotel.city.lowercase().contains(searchText)) {
                            filteredList.add(hotel)
                        }
                    }
                    adapter.filterList(filteredList)
                } else {
                    adapter.filterList(allHotels) // Показваме всичко, ако е празно
                }
                return true
            }
        })


        val sharedPref = getSharedPreferences("HotelAppPrefs", android.content.Context.MODE_PRIVATE)
        var points = sharedPref.getInt("user_points", 0)


        points += 10
        sharedPref.edit().putInt("user_points", points).apply()


        val tvPoints = findViewById<TextView>(R.id.tvPoints)
        tvPoints.text = "Бонус точки: $points"


        if (points >= 100) {
            android.widget.Toast.makeText(this, "Ти си VIP клиент! Отключи 15% отстъпка!", android.widget.Toast.LENGTH_LONG).show()
        }

        val btnGoToFavorites = findViewById<android.widget.ImageButton>(R.id.btnGoToFavorites)
        btnGoToFavorites.setOnClickListener {
            val intent = android.content.Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }

        btnGoToFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }



    }


}