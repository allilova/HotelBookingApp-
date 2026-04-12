package com.example.hotelbookingapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * BookingRepository handles all Firestore operations for bookings
 * and triggers Firestore-based push notifications after each
 * status-changing operation.
 *
 * Notification approach:
 *  - We write a document to the "notifications" Firestore collection
 *  - The recipient's NotificationListenerService picks it up in real time
 *  - This replaces the deprecated FCM Legacy HTTP API
 *
 * Notification triggers:
 *  createBooking()         → notify HOST    (new booking arrived)
 *  updateStatus(CONFIRMED) → notify GUEST   (booking was confirmed)
 *  updateStatus(CANCELLED) → notify GUEST or HOST depending on who cancelled
 */
object BookingRepository {

    private val firestore          = FirebaseFirestore.getInstance()
    private val bookingsCollection = firestore.collection("bookings")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Saves a new booking to Firestore and notifies the host.
     */
    suspend fun createBooking(booking: Booking): Booking {
        // Write to Firestore and get the auto-generated document ID
        val docRef       = bookingsCollection.add(booking.toMap()).await()
        val savedBooking = booking.copy(firestoreId = docRef.id)
        docRef.update("firestoreId", docRef.id).await()

        // Notify the host that a new booking has been made for their hotel.
        // We use the hostUserId directly — no FCM token lookup needed.
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
     * Updates the booking status in Firestore and notifies the affected party.
     *
     * CONFIRMED → notify GUEST  (host confirmed their booking)
     * CANCELLED by guest → notify HOST  (guest cancelled)
     * CANCELLED by host  → notify GUEST (host cancelled)
     */
    suspend fun updateStatus(
        firestoreId: String,
        newStatus:   BookingStatus,
        booking:     Booking? = null
    ) {
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
                    // Host confirmed → notify the guest
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
                        // Guest cancelled → notify the host
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
                        // Host cancelled → notify the guest
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

                BookingStatus.PENDING -> {
                    // PENDING is only set on creation in createBooking()
                }
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
            Booking(
                firestoreId   = getString("firestoreId")        ?: id,
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