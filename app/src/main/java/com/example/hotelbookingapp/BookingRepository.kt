package com.example.hotelbookingapp

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object BookingRepository {

    private val firestore          = FirebaseFirestore.getInstance()
    private val bookingsCollection = firestore.collection("bookings")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Saves a new booking to Firestore.
     *
     * We first get a new document reference (which gives us the ID
     * immediately), then write the booking WITH the correct firestoreId already
     * set. This is atomic — no separate update() call needed.
     */
    suspend fun createBooking(booking: Booking): Booking {
        val docRef = bookingsCollection.document()
        val savedBooking = booking.copy(firestoreId = docRef.id)
        docRef.set(savedBooking.toMap()).await()

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

    /**
     * Fetches all bookings for a guest.
     *
     * NOTE: We intentionally avoid orderBy("bookedAt") here because combining
     * whereEqualTo + orderBy requires a Firestore composite index that may not
     * exist in the project. We sort the results in-memory instead, which is
     * functionally identical and works without any index configuration.
     */
    suspend fun getBookingsForGuest(uid: String): List<Booking> {
        val snapshot = bookingsCollection
            .whereEqualTo("guestUserId", uid)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { it.toBooking() }
            .sortedByDescending { it.bookedAt }
    }

    // ── Read: Host ────────────────────────────────────────────────────────────

    /**
     * Fetches all bookings for hotels owned by a host.
     *
     * Same reasoning as above — no orderBy to avoid composite index requirement.
     * Sorted in-memory by bookedAt descending.
     */
    suspend fun getBookingsForHost(uid: String): List<Booking> {
        val snapshot = bookingsCollection
            .whereEqualTo("hostUserId", uid)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { it.toBooking() }
            .sortedByDescending { it.bookedAt }
    }

    // ── Update: Status ────────────────────────────────────────────────────────

    suspend fun updateStatus(
        firestoreId: String,
        newStatus:   BookingStatus,
        booking:     Booking? = null
    ) {
        if (firestoreId.isBlank()) {
            throw Exception(
                "Cannot update booking status: firestoreId is empty. " +
                        "The booking document ID was not saved correctly."
            )
        }

        bookingsCollection
            .document(firestoreId)
            .update("status", newStatus.name)
            .await()

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
            android.util.Log.e("BookingRepository", "Failed to parse booking doc ${this.id}: ${e.message}")
            null
        }
    }
}