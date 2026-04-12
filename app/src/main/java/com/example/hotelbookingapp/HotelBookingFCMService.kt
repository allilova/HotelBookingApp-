package com.example.hotelbookingapp

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * HotelBookingFCMService handles FCM token refresh events.
 *
 * We no longer use FCM for sending notifications between users
 * (the Legacy API is deprecated and V1 requires a server).
 * Instead we use Firestore-based notifications via NotificationListenerService.
 *
 * This service still needs to exist to:
 *  1. Keep the FCM token up to date in Firestore (for potential future use)
 *  2. Handle any direct FCM messages if Firebase sends them automatically
 */
class HotelBookingFCMService : FirebaseMessagingService() {

    /**
     * Called by Firebase when the device gets a new FCM token.
     * We save it to Firestore even though we don't use it for notifications
     * currently — keeps the door open for future FCM V1 migration.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseAuthManager.updateFcmToken(token)
            } catch (e: Exception) {
                // Silently ignore
            }
        }
    }

    /**
     * Called when a message arrives while the app is in the foreground.
     * Currently not used since we route notifications through Firestore,
     * but kept here for completeness.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""
        NotificationHelper.showLocalNotification(this, title, body)
    }
}