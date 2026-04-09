package com.example.hotelbookingapp

import android.app.Activity
import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: HotelListViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector

    private val addHotelLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.triggerReload()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ── Apply status-bar inset so toolbar clears the status bar on all devices ──
        val toolbarRow = findViewById<View>(R.id.toolbarRow)
        ViewCompat.setOnApplyWindowInsetsListener(toolbarRow) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight + resources.getDimensionPixelSize(R.dimen.toolbar_top_padding),
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        viewModel.setResourceContext(this)

        val sharedPref = getSharedPreferences("HotelAppPrefs", android.content.Context.MODE_PRIVATE)

        fun getSavedLang(): String = sharedPref.getString("app_language", "bg") ?: "bg"

        val savedLang = getSavedLang()
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (!currentLocales.isEmpty) currentLocales[0]?.language ?: "" else ""
        if (currentLang != savedLang) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang))
            return
        }

        // ── RecyclerView ──────────────────────────────────────────────
        val recyclerView = findViewById<RecyclerView>(R.id.rvHotels)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = HotelAdapter { hotel, sharedImageView ->
            val intent = Intent(this, HotelDetailActivity::class.java).apply {
                putExtra("HOTEL_ID",        hotel.id)
                putExtra("HOTEL_PRICE",     hotel.price)
                putExtra("HOTEL_RATING",    hotel.rating)
                putExtra("HOTEL_AVAILABLE", hotel.isAvailable)
                putExtra("HOTEL_LAT",       hotel.latitude)
                putExtra("HOTEL_LON",       hotel.longitude)
                putExtra("HOTEL_IMAGE",     hotel.imageUrl)
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
                tvEmpty.isVisible      = state.isEmpty
                recyclerView.isVisible = !state.isEmpty
            }
        }

        // ── FAB: only visible to HOST users ──────────────────────────
        val fabAddHotel = findViewById<FloatingActionButton>(R.id.fabAddHotel)
        if (authViewModel.isHost()) {
            fabAddHotel.visibility = View.VISIBLE
            fabAddHotel.setOnClickListener {
                addHotelLauncher.launch(Intent(this, AddHotelActivity::class.java))
            }
        } else {
            fabAddHotel.visibility = View.GONE
        }

        // ── Search ────────────────────────────────────────────────────
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onQueryChanged(newText.orEmpty())
                return true
            }
        })

        // ── Sort chips ────────────────────────────────────────────────
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

        // ── Points ────────────────────────────────────────────────────
        val points = sharedPref.getInt("user_points", 0)
        findViewById<TextView>(R.id.tvPoints).text = getString(R.string.bonus_points, points)
        if (points >= 100) {
            Toast.makeText(this, getString(R.string.vip_toast), Toast.LENGTH_LONG).show()
        }

        // ── Navigation buttons ────────────────────────────────────────
        findViewById<ImageButton>(R.id.btnGoToFavorites).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnGoToBookings).setOnClickListener {
            startActivity(Intent(this, BookingHistoryActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnGoToProfile).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        // ── Dark mode ─────────────────────────────────────────────────
        val savedMode = sharedPref.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (AppCompatDelegate.getDefaultNightMode() != savedMode) {
            AppCompatDelegate.setDefaultNightMode(savedMode)
        }
        val btnDark = findViewById<ImageButton>(R.id.btnDarkMode)
        fun updateThemeIcon() {
            val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            btnDark.setImageResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
        }
        updateThemeIcon()
        btnDark.setOnClickListener {
            val newMode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            sharedPref.edit().putInt("night_mode", newMode).apply()
            AppCompatDelegate.setDefaultNightMode(newMode)
        }

        // ── Language ──────────────────────────────────────────────────
        val btnLang = findViewById<Button>(R.id.btnLanguage)
        fun updateLangLabel() { btnLang.text = if (getSavedLang() == "en") "EN" else "БГ" }
        updateLangLabel()
        btnLang.setOnClickListener {
            val next = if (getSavedLang() == "bg") "en" else "bg"
            sharedPref.edit().putString("app_language", next).apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
        }

        // ── Shake to shuffle ──────────────────────────────────────────
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(onShake = {
            viewModel.shuffle()
            runOnUiThread {
                Toast.makeText(this, getString(R.string.shake_shuffled), Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)?.let { accel ->
            sensorManager.registerListener(
                shakeDetector, accel, SensorManager.SENSOR_DELAY_UI
            )
        }
        viewModel.triggerReload()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}