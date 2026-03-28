package com.example.hotelbookingapp

import android.content.Context

object HotelRepository {
    fun getHotels(context: Context): List<Hotel> {
        val res = context.resources
        return listOf(
            Hotel(
                id = 1,
                name = res.getString(R.string.hotel_1_name),
                city = res.getString(R.string.hotel_1_city),
                price = 180.0,
                rating = 4.8f,
                imageUrl = "https://images.unsplash.com/photo-1566073771259-6a8506099945",
                description = res.getString(R.string.hotel_1_desc),
                latitude = 42.6977,
                longitude = 23.3219
            ),
            Hotel(
                id = 2,
                name = res.getString(R.string.hotel_2_name),
                city = res.getString(R.string.hotel_2_city),
                price = 120.0,
                rating = 4.5f,
                imageUrl = "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4",
                description = res.getString(R.string.hotel_2_desc),
                latitude = 42.1354,
                longitude = 24.7453
            ),
            Hotel(
                id = 3,
                name = res.getString(R.string.hotel_3_name),
                city = res.getString(R.string.hotel_3_city),
                price = 95.0,
                rating = 4.2f,
                imageUrl = "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b",
                description = res.getString(R.string.hotel_3_desc),
                latitude = 43.2141,
                longitude = 27.9147
            )
        )
    }
}