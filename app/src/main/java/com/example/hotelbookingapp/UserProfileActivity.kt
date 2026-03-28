package com.example.hotelbookingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class UserProfileActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val tvName      = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail     = findViewById<TextView>(R.id.tvProfileEmail)
        val tvInitial   = findViewById<TextView>(R.id.tvProfileInitial)
        val tvJoined    = findViewById<TextView>(R.id.tvProfileJoined)
        val tvPoints    = findViewById<TextView>(R.id.tvProfilePoints)
        val btnLogout   = findViewById<Button>(R.id.btnLogout)
        val btnFav      = findViewById<Button>(R.id.btnProfileFavorites)
        val btnBookings = findViewById<Button>(R.id.btnProfileBookings)

        // Use a locale-aware date format so the month name also respects the
        // currently active language.
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val sharedPref = getSharedPreferences("HotelAppPrefs", android.content.Context.MODE_PRIVATE)
        val points = sharedPref.getInt("user_points", 0)
        // Use the string resource so the label is translated correctly.
        tvPoints.text = getString(R.string.bonus_points, points)

        viewModel.getLoggedInUser { user ->
            runOnUiThread {
                if (user == null) { goToLogin(); return@runOnUiThread }
                tvName.text    = user.fullName
                tvEmail.text   = user.email
                tvInitial.text = user.fullName.firstOrNull()?.uppercase() ?: "?"
                // getString resolves from the active locale automatically.
                tvJoined.text  = getString(R.string.profile_joined, sdf.format(Date(user.createdAt)))
            }
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
            goToLogin()
        }

        btnFav.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        btnBookings.setOnClickListener {
            startActivity(Intent(this, BookingHistoryActivity::class.java))
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}