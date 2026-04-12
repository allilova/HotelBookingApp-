package com.example.hotelbookingapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * BookingRepository handles all Firestore operations for bookings
 * and triggers push notifications after each status-changing operation.
 *
 * Notification triggers:
 *  createBooking()         → notify HOST    (new booking arrived)
 *  updateStatus(CONFIRMED) → notify GUEST   (booking was confirmed)
 *  updateStatus(CANCELLED) → notify GUEST or HOST depending on who cancelled
 *
 * Notification sending is best-effort — if it fails, the booking operation
 * still succeeds. We never let a notification failure roll back a booking.
 */
object BookingRepository {

    private val firestore         = FirebaseFirestore.getInstance()
    private val bookingsCollection = firestore.collection("bookings")

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Saves a new booking to Firestore and notifies the hotel owner (host).
     *
     * Steps:
     *  1. Write the booking document to Firestore
     *  2. Store the generated document ID back into the document
     *  3. Look up the host's FCM token from Firestore
     *  4. Send a push notification to the host's device
     *
     * @return The saved Booking with firestoreId filled in.
     */
    suspend fun createBooking(booking: Booking): Booking {
        // Step 1 & 2: Write to Firestore and get the generated document ID
        val docRef     = bookingsCollection.add(booking.toMap()).await()
        val savedBooking = booking.copy(firestoreId = docRef.id)
        docRef.update("firestoreId", docRef.id).await()

        // Step 3 & 4: Notify the host — best effort, never throws
        if (booking.hostUserId.isNotBlank()) {
            try {
                val hostToken = FirebaseAuthManager.getFcmTokenForUser(booking.hostUserId)
                if (!hostToken.isNullOrBlank()) {
                    NotificationHelper.sendNotification(
                        recipientToken = hostToken,
                        title          = "Нова резервация!",
                        body           = "${booking.guestUserName} резервира " +
                                "${booking.hotelName} " +
                                "(${booking.checkIn} → ${booking.checkOut})"
                    )
                }
            } catch (e: Exception) {
                // Notification failure must never affect the booking result
                android.util.Log.e("BookingRepository",
                    "Failed to notify host: ${e.message}")
            }
        }

        return savedBooking
    }

    // ── Read: Guest ───────────────────────────────────────────────────────────

    /**
     * Fetches all bookings made by a specific guest, newest first.
     */
    suspend fun getBookingsForGuest(uid: String): List<Booking> {
        val snapshot = bookingsCollection
            .whereEqualTo("guestUserId", uid)
            .orderBy("bookedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toBooking() }
    }

    // ── Read: Host ────────────────────────────────────────────────────────────

    /**
     * Fetches all bookings for hotels owned by a specific host, newest first.
     */
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
     * Updates the status of a booking and sends a push notification
     * to the affected party.
     *
     * Notification logic:
     *  CONFIRMED → notify the GUEST (their booking was accepted)
     *  CANCELLED → notify the GUEST (their booking was rejected/cancelled)
     *
     * We always notify the guest here because:
     *  - If the HOST confirms/cancels → guest needs to know
     *  - If the GUEST cancels → we still call this but the guest
     *    doesn't need to notify themselves. We detect this by checking
     *    if the canceller is the guest (guestUserId == currentUid).
     *
     * @param firestoreId The document ID of the booking to update.
     * @param newStatus   The new status (CONFIRMED or CANCELLED).
     * @param booking     The full booking object needed for notification content.
     *                    Optional — if null, notification is skipped.
     */
    suspend fun updateStatus(
        firestoreId: String,
        newStatus:   BookingStatus,
        booking:     Booking? = null
    ) {
        // Step 1: Update the status field in Firestore
        bookingsCollection
            .document(firestoreId)
            .update("status", newStatus.name)
            .await()

        // Step 2: Send notification — best effort, never throws
        if (booking == null) return

        try {
            val currentUid = FirebaseAuthManager.currentUid

            when (newStatus) {
                BookingStatus.CONFIRMED -> {
                    // Host confirmed → notify the guest
                    val guestToken = FirebaseAuthManager
                        .getFcmTokenForUser(booking.guestUserId)
                    if (!guestToken.isNullOrBlank()) {
                        NotificationHelper.sendNotification(
                            recipientToken = guestToken,
                            title          = "Резервацията е потвърдена! ✓",
                            body           = "${booking.hotelName} " +
                                    "(${booking.checkIn} → ${booking.checkOut})"
                        )
                    }
                }

                BookingStatus.CANCELLED -> {
                    // Determine who cancelled to decide who to notify
                    if (currentUid == booking.guestUserId) {
                        // Guest cancelled → notify the host
                        val hostToken = FirebaseAuthManager
                            .getFcmTokenForUser(booking.hostUserId)
                        if (!hostToken.isNullOrBlank()) {
                            NotificationHelper.sendNotification(
                                recipientToken = hostToken,
                                title          = "Резервация отказана от госта",
                                body           = "${booking.guestUserName} отказа " +
                                        "${booking.hotelName} " +
                                        "(${booking.checkIn} → ${booking.checkOut})"
                            )
                        }
                    } else {
                        // Host cancelled → notify the guest
                        val guestToken = FirebaseAuthManager
                            .getFcmTokenForUser(booking.guestUserId)
                        if (!guestToken.isNullOrBlank()) {
                            NotificationHelper.sendNotification(
                                recipientToken = guestToken,
                                title          = "Резервацията е отказана ✗",
                                body           = "${booking.hotelName} " +
                                        "(${booking.checkIn} → ${booking.checkOut})"
                            )
                        }
                    }
                }

                BookingStatus.PENDING -> {
                    // PENDING is only set on creation, handled in createBooking()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BookingRepository",
                "Failed to send status notification: ${e.message}")
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