package com.example.hotelbookingapp

object HotelRepository {
    fun getHotels(): List<Hotel> {
        return listOf(
            Hotel(
                id = 1,
                name = "Гранд Хотел София",
                city = "София",
                price = 180.0,
                rating = 4.8f,
                imageUrl = "https://images.unsplash.com/photo-1566073771259-6a8506099945",
                description = "Луксозен хотел в центъра.",
                latitude = 42.6977,
                longitude = 23.3219
            ),
            Hotel(
                id = 2,
                name = "Хотел Тримонциум",
                city = "Пловдив",
                price = 120.0,
                rating = 4.5f,
                imageUrl = "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4",
                description = "Класически стил и комфорт.",
                latitude = 42.1354,
                longitude = 24.7453
            ),
            Hotel(
                id = 3,
                name = "Морска Звезда",
                city = "Варна",
                price = 95.0,
                rating = 4.2f,
                imageUrl = "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b",
                description = "Прекрасна гледка към морето.",
                latitude = 43.2141,
                longitude = 27.9147
            )
        )
    }
}