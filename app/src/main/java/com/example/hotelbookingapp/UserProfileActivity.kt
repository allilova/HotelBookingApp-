package com.example.hotelbookingapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
        val tvRoleBadge = findViewById<TextView>(R.id.tvProfileRoleBadge)
        val btnLogout   = findViewById<Button>(R.id.btnLogout)
        val btnFav      = findViewById<Button>(R.id.btnProfileFavorites)
        val btnBookings = findViewById<Button>(R.id.btnProfileBookings)
        val btnMyHotels = findViewById<Button>(R.id.btnProfileMyHotels)

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        tvPoints.text = getString(R.string.bonus_points, 0)

        viewModel.getLoggedInUser { user ->
            runOnUiThread {
                if (user == null) { goToLogin(); return@runOnUiThread }
                tvName.text    = user.fullName
                tvEmail.text   = user.email
                tvInitial.text = user.fullName.firstOrNull()?.uppercase() ?: "?"
                tvJoined.text  = getString(R.string.profile_joined, sdf.format(Date(user.createdAt)))

                // Points from Firestore
                tvPoints.text = getString(R.string.bonus_points, user.points)
                if (user.points >= 100) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        getString(R.string.vip_toast),
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Role badge
                val isHost = user.role == UserRole.HOST.name
                tvRoleBadge.text = if (isHost)
                    getString(R.string.role_badge_host)
                else
                    getString(R.string.role_badge_guest)
                tvRoleBadge.setBackgroundColor(
                    if (isHost) getColor(R.color.primary_blue)
                    else        getColor(R.color.teal_available)
                )

                // "My Hotels" button only visible for HOSTs
                btnMyHotels.visibility = if (isHost) View.VISIBLE else View.GONE
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

        btnMyHotels.setOnClickListener {
            startActivity(Intent(this, MyHotelsActivity::class.java))
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}