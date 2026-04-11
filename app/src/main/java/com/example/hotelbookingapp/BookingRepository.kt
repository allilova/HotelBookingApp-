package com.example.hotelbookingapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * BookingRepository handles all Firestore operations for bookings.
 *
 * Firestore structure:
 *   bookings/                        ← top-level collection
 *     {auto-id}/                     ← one document per booking
 *       firestoreId:   String        ← same as the document ID, stored for convenience
 *       hotelId:       Number
 *       hotelName:     String
 *       hotelCity:     String
 *       hotelImageUrl: String
 *       checkIn:       String        ← "dd/MM/yyyy"
 *       checkOut:      String        ← "dd/MM/yyyy"
 *       pricePerNight: Number
 *       guestCount:    Number
 *       guestNames:    Array<String>
 *       guestUserId:   String        ← Firebase UID of the guest
 *       guestUserName: String        ← display name of the guest
 *       hostUserId:    String        ← Firebase UID of the hotel owner
 *       status:        String        ← PENDING | CONFIRMED | CANCELLED
 *       bookedAt:      Number        ← epoch millis
 *
 * Queries used:
 *   Guest history:  bookings WHERE guestUserId == currentUid ORDER BY bookedAt DESC
 *   Host dashboard: bookings WHERE hostUserId  == currentUid ORDER BY bookedAt DESC
 *
 * IMPORTANT: These queries require Firestore composite indexes.
 * Firestore will print a link in Logcat to create them automatically
 * the first time each query runs. Click the link and the index is
 * created in the Firebase Console automatically.
 */
object BookingRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Top-level "bookings" collection — every booking is a document here
    private val bookingsCollection = firestore.collection("bookings")


    suspend fun createBooking(booking: Booking): Booking {

        val docRef = bookingsCollection.add(booking.toMap()).await()


        val savedBooking = booking.copy(firestoreId = docRef.id)
        docRef.update("firestoreId", docRef.id).await()

        return savedBooking
    }


    suspend fun getBookingsForGuest(uid: String): List<Booking> {
        val snapshot = bookingsCollection
            .whereEqualTo("guestUserId", uid)
            .orderBy("bookedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toBooking()
        }
    }


    suspend fun getBookingsForHost(uid: String): List<Booking> {
        val snapshot = bookingsCollection
            .whereEqualTo("hostUserId", uid)
            .orderBy("bookedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toBooking()
        }
    }



    suspend fun updateStatus(firestoreId: String, newStatus: BookingStatus) {
        bookingsCollection
            .document(firestoreId)
            .update("status", newStatus.name)
            .await()
    }



    private fun Booking.toMap(): Map<String, Any> = mapOf(
        "firestoreId"   to firestoreId,
        "hotelId"       to hotelId,
        "hotelName"     to hotelName,
        "hotelCity"     to hotelCity,
        "hotelImageUrl" to hotelImageUrl,
        "checkIn"       to checkIn,
        "checkOut"      to checkOut,
        "pricePerNight" to pricePerNight,
        "guestCount"    to guestCount,
        "guestNames"    to guestNames,
        "guestUserId"   to guestUserId,
        "guestUserName" to guestUserName,
        "hostUserId"    to hostUserId,
        "status"        to status,
        "bookedAt"      to bookedAt
    )

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toBooking(): Booking? {
        return try {
            Booking(
                firestoreId   = getString("firestoreId")   ?: id,
                hotelId       = getLong("hotelId")?.toInt() ?: 0,
                hotelName     = getString("hotelName")     ?: "",
                hotelCity     = getString("hotelCity")     ?: "",
                hotelImageUrl = getString("hotelImageUrl") ?: "",
                checkIn       = getString("checkIn")       ?: "",
                checkOut      = getString("checkOut")      ?: "",
                pricePerNight = getDouble("pricePerNight") ?: 0.0,
                guestCount    = getLong("guestCount")?.toInt() ?: 1,
                // guestNames is stored as an array in Firestore
                // Firestore returns arrays as List<*> so we cast safely
                guestNames    = (get("guestNames") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList(),
                guestUserId   = getString("guestUserId")   ?: "",
                guestUserName = getString("guestUserName") ?: "",
                hostUserId    = getString("hostUserId")    ?: "",
                status        = getString("status")        ?: BookingStatus.PENDING.name,
                bookedAt      = getLong("bookedAt")        ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // If the document is malformed (e.g. old schema), skip it
            null
        }
    }
}