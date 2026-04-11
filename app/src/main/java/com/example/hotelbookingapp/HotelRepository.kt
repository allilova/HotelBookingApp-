package com.example.hotelbookingapp

import android.content.Context

/**
 * HotelRepository is the single source of truth for hotel data.
 *
 * It merges two sources:
 *  1. Static hotels — hardcoded in the app (HotelRepository.getStaticHotels)
 *  2. Custom hotels — created by HOST users, stored in Firestore
 *
 * ID scheme (unchanged from before):
 *  Static hotels:  id = 1, 2, 3, ...  (simple integers)
 *  Custom hotels:  id = CUSTOM_ID_OFFSET + index in the Firestore list
 *                  firestoreId stored separately for direct Firestore access
 *
 * Why keep the integer ID scheme for custom hotels?
 *  Many parts of the app pass hotel IDs as Int via Intent extras.
 *  Changing all of those to String would require touching every Activity.
 *  The CUSTOM_ID_OFFSET approach lets us keep the existing Intent API.
 *  The actual Firestore document ID is stored in Hotel.firestoreId.
 */
object HotelRepository {

    // Static hotels use IDs 1–999.
    // Custom hotels get IDs starting from CUSTOM_ID_OFFSET so they
    // never collide with static hotel IDs.
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

    // Kept for call sites that only need static hotels
    fun getHotels(context: Context): List<Hotel> = getStaticHotels(context)

    // ── Merge static + Firestore custom hotels ────────────────────────────────

    /**
     * Returns ALL hotels (static + host-created) as a unified list.
     *
     * Custom hotels are fetched from Firestore via CustomHotelRepository.
     * Each custom hotel gets a stable integer ID based on its position in
     * the Firestore result list plus CUSTOM_ID_OFFSET.
     *
     * This is a suspend function because it reads from Firestore.
     * Call it from a coroutine with Dispatchers.IO.
     */
    suspend fun getAllHotels(context: Context): List<Hotel> {
        val static = getStaticHotels(context)
        val custom = CustomHotelRepository.getAllHotels().mapIndexed { index, hotel ->
            hotel.toHotel(CUSTOM_ID_OFFSET + index)
        }
        return static + custom
    }

    // ── Convert CustomHotel to Hotel ──────────────────────────────────────────

    /**
     * Converts a CustomHotel (Firestore entity) to the universal Hotel model
     * used throughout the app.
     *
     * @param intId The integer ID assigned to this hotel (CUSTOM_ID_OFFSET + index).
     *              This is used for Intent extras and RecyclerView item identification.
     */
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
        ownerUserId = ownerUserId  // ← carry the host's Firebase UID
    )

    // ── Resolution helpers ────────────────────────────────────────────────────

    /**
     * Finds a hotel by its integer ID.
     *
     * For static hotels: looks up directly by ID.
     * For custom hotels: fetches from Firestore by firestoreId.
     *
     * This is a suspend function because custom hotel lookup hits Firestore.
     * Call from Dispatchers.IO.
     *
     * @param hotelId  The integer hotel ID (from Intent extra).
     * @param savedName Fallback name for old records where hotelId == 0.
     */
    suspend fun resolve(
        context:   Context,
        hotelId:   Int,
        savedName: String,
        firestoreId: String = ""
    ): Hotel? {
        // ── Custom hotel lookup ───────────────────────────────────────────────
        if (isCustomId(hotelId)) {
            // If we have the firestoreId, use it for a direct document fetch
            if (firestoreId.isNotBlank()) {
                return CustomHotelRepository.getHotelById(firestoreId)
                    ?.toHotel(hotelId)
            }
            // Fallback: scan all custom hotels to find by integer ID
            val allCustom = CustomHotelRepository.getAllHotels()
            val index = hotelId - CUSTOM_ID_OFFSET
            return allCustom.getOrNull(index)?.toHotel(hotelId)
        }

        // ── Static hotel lookup ───────────────────────────────────────────────
        val current = getStaticHotels(context)
        if (hotelId > 0) {
            current.find { it.id == hotelId }?.let { return it }
        }

        // Fallback: match by name across locales (for old records with hotelId == 0)
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