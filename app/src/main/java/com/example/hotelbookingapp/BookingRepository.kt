package com.example.hotelbookingapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object BookingRepository {

    private val firestore          = FirebaseFirestore.getInstance()
    private val bookingsCollection = firestore.collection("bookings")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Saves a new booking to Firestore.
     *
     * KEY FIX: We first get a new document reference (which gives us the ID
     * immediately), then write the booking WITH the correct firestoreId already
     * set. This is atomic — no separate update() call needed, so the ID can
     * never be missing from the document.
     */
    suspend fun createBooking(booking: Booking): Booking {
        // Get a new document reference — this gives us the ID before writing
        val docRef = bookingsCollection.document()

        // Build the booking with the correct firestoreId already filled in
        val savedBooking = booking.copy(firestoreId = docRef.id)

        // Write the complete booking in ONE call — no separate update needed
        docRef.set(savedBooking.toMap()).await()

        // Notify the host
        if (booking.hostUserId.isNotBlank()) {
            NotificationHelper.sendRemoteNotification(
                recipientUid = booking.hostUserId,
                title        = "Нова резервация!",
                body         = "${booking.guestUserName} резервира " +
                        "${booking.hotelName} " +
                        "(${booking.checkIn} → ${booking.checkOut})"
            )
        }

        return savedBooking
    }

    // ── Read: Guest ───────────────────────────────────────────────────────────

    suspend fun getBookingsForGuest(uid: String): List<Booking> {
        val snapshot = bookingsCollection
            .whereEqualTo("guestUserId", uid)
            .orderBy("bookedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toBooking() }
    }

    // ── Read: Host ────────────────────────────────────────────────────────────

    suspend fun getBookingsForHost(uid: String): List<Booking> {
        val snapshot = bookingsCollection
            .whereEqualTo("hostUserId", uid)
            .orderBy("bookedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toBooking() }
    }

    // ── Update: Status ────────────────────────────────────────────────────────

    /**
     * Updates the booking status in Firestore.
     *
     * KEY FIX: Before updating, we validate that firestoreId is not blank.
     * If it is blank we throw immediately with a clear message instead of
     * silently failing.
     */
    suspend fun updateStatus(
        firestoreId: String,
        newStatus:   BookingStatus,
        booking:     Booking? = null
    ) {
        // Guard: never attempt to update a document with an empty ID
        if (firestoreId.isBlank()) {
            throw Exception(
                "Cannot update booking status: firestoreId is empty. " +
                        "The booking document ID was not saved correctly."
            )
        }

        // Update the status field in Firestore
        bookingsCollection
            .document(firestoreId)
            .update("status", newStatus.name)
            .await()

        // Send notification — best effort, never throws
        if (booking == null) return

        try {
            val currentUid = FirebaseAuthManager.currentUid

            when (newStatus) {
                BookingStatus.CONFIRMED -> {
                    if (booking.guestUserId.isNotBlank()) {
                        NotificationHelper.sendRemoteNotification(
                            recipientUid = booking.guestUserId,
                            title        = "Резервацията е потвърдена! ✓",
                            body         = "${booking.hotelName} " +
                                    "(${booking.checkIn} → ${booking.checkOut})"
                        )
                    }
                }

                BookingStatus.CANCELLED -> {
                    if (currentUid == booking.guestUserId) {
                        // Guest cancelled → notify host
                        if (booking.hostUserId.isNotBlank()) {
                            NotificationHelper.sendRemoteNotification(
                                recipientUid = booking.hostUserId,
                                title        = "Резервация отказана от госта",
                                body         = "${booking.guestUserName} отказа " +
                                        "${booking.hotelName} " +
                                        "(${booking.checkIn} → ${booking.checkOut})"
                            )
                        }
                    } else {
                        // Host cancelled → notify guest
                        if (booking.guestUserId.isNotBlank()) {
                            NotificationHelper.sendRemoteNotification(
                                recipientUid = booking.guestUserId,
                                title        = "Резервацията е отказана ✗",
                                body         = "${booking.hotelName} " +
                                        "(${booking.checkIn} → ${booking.checkOut})"
                            )
                        }
                    }
                }

                BookingStatus.PENDING -> { /* only set on creation */ }
            }
        } catch (e: Exception) {
            android.util.Log.e("BookingRepository",
                "Failed to send notification: ${e.message}")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
            // KEY FIX: Use the actual Firestore document ID (this.id) as the
            // primary source for firestoreId. Only fall back to the stored
            // field value if the document ID is somehow unavailable.
            // This ensures firestoreId is NEVER empty even for old bookings
            // that were saved before the fix.
            val resolvedFirestoreId = this.id.ifBlank {
                getString("firestoreId") ?: ""
            }

            Booking(
                firestoreId   = resolvedFirestoreId,
                hotelId       = getLong("hotelId")?.toInt()     ?: 0,
                hotelName     = getString("hotelName")          ?: "",
                hotelCity     = getString("hotelCity")          ?: "",
                hotelImageUrl = getString("hotelImageUrl")      ?: "",
                checkIn       = getString("checkIn")            ?: "",
                checkOut      = getString("checkOut")           ?: "",
                pricePerNight = getDouble("pricePerNight")      ?: 0.0,
                guestCount    = getLong("guestCount")?.toInt()  ?: 1,
                guestNames    = (get("guestNames") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList(),
                guestUserId   = getString("guestUserId")        ?: "",
                guestUserName = getString("guestUserName")      ?: "",
                hostUserId    = getString("hostUserId")         ?: "",
                status        = getString("status")             ?: BookingStatus.PENDING.name,
                bookedAt      = getLong("bookedAt")             ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}