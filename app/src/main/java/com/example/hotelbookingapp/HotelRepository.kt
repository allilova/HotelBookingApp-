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

    /**
     * Finds a hotel in the current locale by trying:
     * 1. Match by hotelId directly (works for records saved after MIGRATION_4_5)
     * 2. Match by saved name against ALL locales (works for old records with hotelId = 0)
     *    by loading both BG and EN names and finding which hotel matches,
     *    then returning the current-locale version.
     */
    fun resolve(context: Context, hotelId: Int, savedName: String): Hotel? {
        val current = getHotels(context)

        // Try direct ID match first
        if (hotelId > 0) {
            current.find { it.id == hotelId }?.let { return it }
        }

        // Fallback: match saved name against BG strings (the original save language)
        val bgContext = createLocaleContext(context, "bg")
        val enContext = createLocaleContext(context, "en")

        val bgHotels = getHotels(bgContext)
        val enHotels = getHotels(enContext)

        // Find which hotel index matches the saved name (in any language)
        val allLocaleHotels = listOf(bgHotels, enHotels)
        for (localeHotels in allLocaleHotels) {
            val idx = localeHotels.indexOfFirst { it.name == savedName }
            if (idx >= 0) {
                return current[idx] // return the same hotel in the current locale
            }
        }
        return null
    }

    private fun createLocaleContext(context: Context, language: String): Context {
        val locale = java.util.Locale(language)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}