package com.example.hotelbookingapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * HotelBookingFCMService handles incoming Firebase Cloud Messaging events.
 *
 * Two responsibilities:
 *
 * 1. onNewToken() — called by Firebase whenever our device gets a new FCM token.
 *    We save the new token to Firestore so other users can send us notifications.
 *    This happens automatically on:
 *      - First app install
 *      - User clears app data
 *      - User restores the app on a new device
 *
 * 2. onMessageReceived() — called when a push notification arrives while the
 *    app is in the FOREGROUND. When the app is in the background or killed,
 *    the Firebase SDK shows the notification automatically without calling this.
 *    We build and show the notification manually here for the foreground case.
 *
 * Notification channel:
 *   ID:   "booking_channel"
 *   Name: "Booking Notifications"
 *   This channel must be created before showing any notification on Android 8+.
 *   We create it here so it is always ready before the first notification arrives.
 */
class HotelBookingFCMService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "booking_channel"
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    /**
     * Called whenever Firebase generates a new FCM token for this device.
     *
     * We save the token to Firestore at users/{uid}/fcmToken so that
     * other users can send push notifications to this device.
     *
     * If no user is logged in when this is called (e.g. first install),
     * the token will be saved the next time the user logs in — the
     * login flow in AuthViewModel calls FirebaseAuthManager.updateFcmToken()
     * after a successful login.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Save to Firestore on the IO dispatcher — this is a network call
        // and must not block the main thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseAuthManager.updateFcmToken(token)
            } catch (e: Exception) {
                // Silently ignore — the token will be updated on next login
            }
        }
    }

    // ── Foreground notification display ───────────────────────────────────────

    /**
     * Called when a push notification arrives while the app is OPEN (foreground).
     *
     * When the app is in the background or killed, Firebase shows the notification
     * automatically from the "notification" payload. We only need to handle the
     * foreground case here.
     *
     * The notification payload we send has:
     *   title: notification title (e.g. "Резервацията е потвърдена!")
     *   body:  notification body (e.g. "Хотел Гранд — 3 нощ/и")
     *
     * When the user taps the notification, we open MainActivity.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Read the notification title and body from the FCM payload
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.notif_title)

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        showNotification(title, body)
    }

    // ── Notification builder ──────────────────────────────────────────────────

    /**
     * Builds and displays a system notification.
     *
     * @param title The notification title.
     * @param body  The notification body text.
     */
    private fun showNotification(title: String, body: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel (required for Android 8.0+).
        // Safe to call multiple times — Android ignores duplicate channel creation.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH  // HIGH = shows as popup/heads-up
            ).apply {
                description = "Notifications for booking status updates"
                enableLights(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        // Tapping the notification opens MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            // FLAG_IMMUTABLE required on Android 12+ for security
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)  // Dismiss notification when tapped
            .setContentIntent(pendingIntent)
            .build()

        // Use a unique notification ID based on time to avoid overwriting
        // previous notifications if multiple arrive quickly
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}