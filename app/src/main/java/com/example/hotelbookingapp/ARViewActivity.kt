package com.example.hotelbookingapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ARViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arview)

        val hotelName = intent.getStringExtra("HOTEL_NAME")
        findViewById<TextView>(R.id.arHotelName).text = hotelName


    }
}