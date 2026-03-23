package com.example.hotelbookingapp

object HotelRepository {
    fun getHotels(): List<Hotel> {
        return listOf(
            Hotel(1, "Гранд Хотел София", "София", 180.0, 4.8f, "https://images.unsplash.com/photo-1566073771259-6a8506099945", "Луксозен хотел в центъра."),
            Hotel(2, "Хотел Тримонциум", "Пловдив", 120.0, 4.5f, "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4", "Класически стил и комфорт."),
            Hotel(3, "Морска Звезда", "Варна", 95.0, 4.2f, "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b", "Прекрасна гледка към морето.")
        )
    }
}