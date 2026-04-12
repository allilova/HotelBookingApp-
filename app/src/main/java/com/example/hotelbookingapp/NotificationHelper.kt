package com.example.hotelbookingapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * NotificationHelper handles two types of notifications:
 *
 * 1. LOCAL notifications — shown immediately on THIS device using
 *    Android NotificationCompat. Used after booking confirmation, etc.
 *
 * 2. REMOTE notifications — written to Firestore so the RECIPIENT's
 *    device can read them in real time via a Firestore listener.
 *    This replaces the deprecated FCM Legacy HTTP API.
 *
 * Firestore structure for remote notifications:
 *   notifications/
 *     {auto-id}/
 *       recipientUid: String   ← Firebase UID of the person to notify
 *       title:        String
 *       body:         String
 *       createdAt:    Long     ← epoch millis
 *       read:         Boolean  ← false until the recipient reads it
 */
object NotificationHelper {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCollection = firestore.collection("notifications")

    const val CHANNEL_ID = "booking_channel"

    // ── Remote notification (Firestore) ───────────────────────────────────────

    /**
     * Writes a notification document to Firestore for the recipient.
     * The recipient's device reads this via a real-time Firestore listener
     * in HotelBookingListenerService and shows it as a system notification.
     *
     * This is best-effort — if it fails, we log but never throw.
     *
     * @param recipientUid Firebase UID of the user to notify.
     * @param title        Notification title.
     * @param body         Notification body text.
     */
    suspend fun sendRemoteNotification(
        recipientUid: String,
        title:        String,
        body:         String
    ) {
        if (recipientUid.isBlank()) return

        try {
            notificationsCollection.add(
                mapOf(
                    "recipientUid" to recipientUid,
                    "title"        to title,
                    "body"         to body,
                    "createdAt"    to System.currentTimeMillis(),
                    "read"         to false
                )
            ).await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper",
                "Failed to write notification to Firestore: ${e.message}")
        }
    }

    // ── Local notification (shown on THIS device immediately) ─────────────────

    /**
     * Shows a system notification on the current device immediately.
     * Used for confirming the user's own action (e.g. after booking).
     *
     * @param context Android context.
     * @param title   Notification title.
     * @param body    Notification body text.
     */
    fun showLocalNotification(context: Context, title: String, body: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            // Create channel — safe to call multiple times on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Booking Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableLights(true)
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }

            // Tapping the notification opens MainActivity
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper",
                "Failed to show local notification: ${e.message}")
        }
    }
}