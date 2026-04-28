package com.example.hotelbookingapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddHotelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hotel)

        supportActionBar?.apply {
            title = getString(R.string.add_hotel_title)
            setDisplayHomeAsUpEnabled(true)
        }

        val etName     = findViewById<TextInputEditText>(R.id.etHotelName)
        val etCity     = findViewById<TextInputEditText>(R.id.etHotelCity)
        val etPrice    = findViewById<TextInputEditText>(R.id.etHotelPrice)
        val etDesc     = findViewById<TextInputEditText>(R.id.etHotelDesc)
        val etImageUrl = findViewById<TextInputEditText>(R.id.etHotelImageUrl)
        val etLat      = findViewById<TextInputEditText>(R.id.etHotelLat)
        val etLon      = findViewById<TextInputEditText>(R.id.etHotelLon)
        val btnSave    = findViewById<Button>(R.id.btnSaveHotel)
        val progress   = findViewById<ProgressBar>(R.id.addHotelProgress)

        btnSave.setOnClickListener {
            val name   = etName.text.toString().trim()
            val city   = etCity.text.toString().trim()
            val price  = etPrice.text.toString().toDoubleOrNull()
            val desc   = etDesc.text.toString().trim()
            val imgUrl = etImageUrl.text.toString().trim()
            val lat    = etLat.text.toString().toDoubleOrNull() ?: 42.6977
            val lon    = etLon.text.toString().toDoubleOrNull() ?: 23.3219

            // Validate required fields
            if (name.isBlank() || city.isBlank() || price == null) {
                Toast.makeText(
                    this,
                    getString(R.string.add_hotel_validation_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Get the Firebase UID of the currently logged-in host
            val ownerUid = FirebaseAuthManager.currentUid
            if (ownerUid == null) {
                Toast.makeText(
                    this,
                    getString(R.string.error_not_logged_in),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnSave.isEnabled   = false

            lifecycleScope.launch {
                try {
                    // Save to Firestore via CustomHotelRepository.
                    // ownerUserId is now a String (Firebase UID) instead of Int.
                    CustomHotelRepository.createHotel(
                        CustomHotel(
                            ownerUserId = ownerUid,
                            name        = name,
                            city        = city,
                            price       = price,
                            description = desc,
                            imageUrl    = imgUrl,
                            latitude    = lat,
                            longitude   = lon
                        )
                    )
                    Toast.makeText(
                        this@AddHotelActivity,
                        getString(R.string.add_hotel_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AddHotelActivity,
                        getString(R.string.add_hotel_error),
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    progress.visibility = View.GONE
                    btnSave.isEnabled   = true
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}