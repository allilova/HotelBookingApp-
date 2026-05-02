package com.example.hotelbookingapp

import android.content.Context


object HotelRepository {

    private const val CUSTOM_ID_OFFSET = 1_000

    fun isCustomId(hotelId: Int)       = hotelId >= CUSTOM_ID_OFFSET
    fun hotelIdToIndex(hotelId: Int)   = hotelId - CUSTOM_ID_OFFSET

    // ── Static hotels ─────────────────────────────────────────────────────────

    fun getStaticHotels(context: Context): List<Hotel> {
        val res = context.resources
        return listOf(
            Hotel(
                id          = 1,
                name        = res.getString(R.string.hotel_1_name),
                city        = res.getString(R.string.hotel_1_city),
                price       = 180.0,
                rating      = 4.8f,
                imageUrl    = "https://images.unsplash.com/photo-1566073771259-6a8506099945",
                description = res.getString(R.string.hotel_1_desc),
                latitude    = 42.6977,
                longitude   = 23.3219
            ),
            Hotel(
                id          = 2,
                name        = res.getString(R.string.hotel_2_name),
                city        = res.getString(R.string.hotel_2_city),
                price       = 120.0,
                rating      = 4.5f,
                imageUrl    = "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4",
                description = res.getString(R.string.hotel_2_desc),
                latitude    = 42.1354,
                longitude   = 24.7453
            ),
            Hotel(
                id          = 3,
                name        = res.getString(R.string.hotel_3_name),
                city        = res.getString(R.string.hotel_3_city),
                price       = 95.0,
                rating      = 4.2f,
                imageUrl    = "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b",
                description = res.getString(R.string.hotel_3_desc),
                latitude    = 43.2141,
                longitude   = 27.9147
            )
        )
    }


    fun getHotels(context: Context): List<Hotel> = getStaticHotels(context)

    // ── Merge static + Firestore custom hotels ────────────────────────────────

    suspend fun getAllHotels(context: Context): List<Hotel> {
        val static = getStaticHotels(context)
        val custom = CustomHotelRepository.getAllHotels().mapIndexed { index, hotel ->
            hotel.toHotel(CUSTOM_ID_OFFSET + index)
        }
        return static + custom
    }

    // ── Convert CustomHotel to Hotel ──────────────────────────────────────────
    fun CustomHotel.toHotel(intId: Int): Hotel = Hotel(
        id          = intId,
        name        = name,
        city        = city,
        price       = price,
        rating      = rating,
        imageUrl    = imageUrl,
        description = description,
        latitude    = latitude,
        longitude   = longitude,
        isAvailable = isAvailable,
        firestoreId = firestoreId,
        ownerUserId = ownerUserId
    )

    // ── Resolution helpers ────────────────────────────────────────────────────
    suspend fun resolve(
        context:   Context,
        hotelId:   Int,
        savedName: String,
        firestoreId: String = ""
    ): Hotel? {
        // ── Custom hotel lookup ───────────────────────────────────────────────
        if (isCustomId(hotelId)) {
            if (firestoreId.isNotBlank()) {
                return CustomHotelRepository.getHotelById(firestoreId)
                    ?.toHotel(hotelId)
            }

            val allCustom = CustomHotelRepository.getAllHotels()
            val index = hotelId - CUSTOM_ID_OFFSET
            return allCustom.getOrNull(index)?.toHotel(hotelId)
        }

        // ── Static hotel lookup ───────────────────────────────────────────────
        val current = getStaticHotels(context)
        if (hotelId > 0) {
            current.find { it.id == hotelId }?.let { return it }
        }


        val bgContext = createLocaleContext(context, "bg")
        val enContext = createLocaleContext(context, "en")
        val bgHotels  = getStaticHotels(bgContext)
        val enHotels  = getStaticHotels(enContext)

        for (localeHotels in listOf(bgHotels, enHotels)) {
            val idx = localeHotels.indexOfFirst { it.name == savedName }
            if (idx >= 0) return current[idx]
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