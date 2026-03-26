package com.example.hotelbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: HotelListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.rvHotels)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = HotelAdapter { hotel, sharedImageView ->
            val intent = Intent(this, HotelDetailActivity::class.java).apply {
                putExtra("HOTEL_NAME",      hotel.name)
                putExtra("HOTEL_CITY",      hotel.city)
                putExtra("HOTEL_DESC",      hotel.description)
                putExtra("HOTEL_IMAGE",     hotel.imageUrl)
                putExtra("HOTEL_LAT",       hotel.latitude)
                putExtra("HOTEL_LON",       hotel.longitude)
                putExtra("HOTEL_PRICE",     hotel.price)
                putExtra("HOTEL_RATING",    hotel.rating)
                putExtra("HOTEL_AVAILABLE", hotel.isAvailable)
            }
            val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, sharedImageView, "hotelImageTransition"
            )
            startActivity(intent, options.toBundle())
        }
        recyclerView.adapter = adapter

        val tvEmpty = findViewById<TextView>(R.id.tvEmptySearch)

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.hotels)
                tvEmpty.isVisible = state.isEmpty
                recyclerView.isVisible = !state.isEmpty
            }
        }

        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onQueryChanged(newText.orEmpty())
                return true
            }
        })

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupSort)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val order = when (checkedIds.firstOrNull()) {
                R.id.chipPriceAsc  -> SortOrder.PRICE_ASC
                R.id.chipPriceDesc -> SortOrder.PRICE_DESC
                R.id.chipRating    -> SortOrder.RATING_DESC
                else               -> SortOrder.NONE
            }
            viewModel.onSortChanged(order)
        }

        val sharedPref = getSharedPreferences("HotelAppPrefs", android.content.Context.MODE_PRIVATE)
        val points = sharedPref.getInt("user_points", 0) + 10
        sharedPref.edit().putInt("user_points", points).apply()
        findViewById<TextView>(R.id.tvPoints).text = getString(R.string.bonus_points, points)
        if (points >= 100) {
            android.widget.Toast.makeText(this, getString(R.string.vip_toast), android.widget.Toast.LENGTH_LONG).show()
        }

        findViewById<ImageButton>(R.id.btnGoToFavorites).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnGoToBookings).setOnClickListener {
            startActivity(Intent(this, BookingHistoryActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnGoToProfile).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }
}