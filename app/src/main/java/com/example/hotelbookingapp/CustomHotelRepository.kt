package com.example.hotelbookingapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object CustomHotelRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val hotelsCollection = firestore.collection("hotels")

    // ── Create ────────────────────────────────────────────────────────────────
    suspend fun createHotel(hotel: CustomHotel): CustomHotel {
        val docRef = hotelsCollection.add(hotel.toMap()).await()


        val savedHotel = hotel.copy(firestoreId = docRef.id)
        docRef.update("firestoreId", docRef.id).await()

        return savedHotel
    }

    // ── Read: All ─────────────────────────────────────────────────────────────
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
    suspend fun getHotelById(firestoreId: String): CustomHotel? {
        val snapshot = hotelsCollection
            .document(firestoreId)
            .get()
            .await()

        return if (snapshot.exists()) snapshot.toCustomHotel() else null
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    suspend fun deleteHotel(firestoreId: String) {
        hotelsCollection
            .document(firestoreId)
            .delete()
            .await()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
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
            null
        }
    }
}