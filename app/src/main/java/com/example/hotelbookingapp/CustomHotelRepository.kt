package com.example.hotelbookingapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * CustomHotelRepository handles all Firestore operations for host-created hotels.
 *
 * Firestore structure:
 *   hotels/                      ← top-level collection
 *     {auto-id}/                 ← one document per custom hotel
 *       firestoreId:  String     ← same as the document ID, stored for convenience
 *       ownerUserId:  String     ← Firebase UID of the host
 *       name:         String
 *       city:         String
 *       price:        Number
 *       rating:       Number
 *       imageUrl:     String
 *       description:  String
 *       latitude:     Number
 *       longitude:    Number
 *       isAvailable:  Boolean
 *       createdAt:    Number     ← epoch millis
 *
 * Queries used:
 *   All hotels list:  hotels ORDER BY createdAt DESC  (merged with static hotels)
 *   Host's hotels:    hotels WHERE ownerUserId == uid ORDER BY createdAt DESC
 *   Single hotel:     hotels/{firestoreId}
 */
object CustomHotelRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Top-level "hotels" collection — every custom hotel is a document here
    private val hotelsCollection = firestore.collection("hotels")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Saves a new custom hotel to Firestore.
     *
     * Steps:
     *  1. Let Firestore generate a unique document ID with .add()
     *  2. Store the generated ID back into the document as firestoreId
     *     so we can delete or update the hotel without a secondary query.
     *
     * @return The saved CustomHotel with firestoreId filled in.
     * @throws Exception if the Firestore write fails.
     */
    suspend fun createHotel(hotel: CustomHotel): CustomHotel {
        // Step 1: Write the hotel to Firestore with an auto-generated ID
        val docRef = hotelsCollection.add(hotel.toMap()).await()

        // Step 2: Store the generated document ID back into the document
        val savedHotel = hotel.copy(firestoreId = docRef.id)
        docRef.update("firestoreId", docRef.id).await()

        return savedHotel
    }

    // ── Read: All ─────────────────────────────────────────────────────────────

    /**
     * Fetches ALL custom hotels from Firestore, ordered by creation date.
     * This list is merged with static hotels in HotelRepository.getAllHotels()
     * so guests see both static and host-created hotels in the main list.
     *
     * Query: hotels ORDER BY createdAt DESC
     */
    suspend fun getAllHotels(): List<CustomHotel> {
        val snapshot = hotelsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toCustomHotel()
        }
    }

    // ── Read: By Owner ────────────────────────────────────────────────────────

    /**
     * Fetches all hotels created by a specific host.
     * Used in MyHotelsActivity for the host's management screen.
     *
     * Query: hotels WHERE ownerUserId == uid ORDER BY createdAt DESC
     *
     * NOTE: This query requires a Firestore composite index on
     * (ownerUserId, createdAt). Firestore will print a Logcat link
     * to create it automatically the first time this query runs.
     *
     * @param uid Firebase UID of the host.
     */
    suspend fun getHotelsByOwner(uid: String): List<CustomHotel> {
        val snapshot = hotelsCollection
            .whereEqualTo("ownerUserId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toCustomHotel()
        }
    }

    // ── Read: Single ──────────────────────────────────────────────────────────

    /**
     * Fetches a single custom hotel by its Firestore document ID.
     * Used in HotelDetailActivity and HotelRepository.resolve() to look up
     * a specific hotel when displaying its details.
     *
     * @param firestoreId The Firestore document ID of the hotel.
     * @return The CustomHotel, or null if the document doesn't exist.
     */
    suspend fun getHotelById(firestoreId: String): CustomHotel? {
        val snapshot = hotelsCollection
            .document(firestoreId)
            .get()
            .await()

        return if (snapshot.exists()) snapshot.toCustomHotel() else null
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a custom hotel from Firestore.
     * Only called from MyHotelsActivity after the host confirms deletion.
     *
     * @param firestoreId The Firestore document ID of the hotel to delete.
     * @throws Exception if the document doesn't exist or the delete fails.
     */
    suspend fun deleteHotel(firestoreId: String) {
        hotelsCollection
            .document(firestoreId)
            .delete()
            .await()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts a CustomHotel data class to a Map<String, Any> for Firestore.
     * Explicit map keeps field names stable and visible.
     */
    private fun CustomHotel.toMap(): Map<String, Any> = mapOf(
        "firestoreId"  to firestoreId,
        "ownerUserId"  to ownerUserId,
        "name"         to name,
        "city"         to city,
        "price"        to price,
        "rating"       to rating,
        "imageUrl"     to imageUrl,
        "description"  to description,
        "latitude"     to latitude,
        "longitude"    to longitude,
        "isAvailable"  to isAvailable,
        "createdAt"    to createdAt
    )

    /**
     * Converts a Firestore DocumentSnapshot to a CustomHotel data class.
     * Returns null if the document is missing required fields or is malformed.
     */
    private fun com.google.firebase.firestore.DocumentSnapshot.toCustomHotel(): CustomHotel? {
        return try {
            CustomHotel(
                firestoreId  = getString("firestoreId")      ?: id,
                ownerUserId  = getString("ownerUserId")      ?: "",
                name         = getString("name")             ?: "",
                city         = getString("city")             ?: "",
                price        = getDouble("price")            ?: 0.0,
                rating       = getDouble("rating")?.toFloat() ?: 0f,
                imageUrl     = getString("imageUrl")         ?: "",
                description  = getString("description")      ?: "",
                latitude     = getDouble("latitude")         ?: 0.0,
                longitude    = getDouble("longitude")        ?: 0.0,
                isAvailable  = getBoolean("isAvailable")     ?: true,
                createdAt    = getLong("createdAt")          ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // If the document is malformed skip it rather than crashing
            null
        }
    }
}