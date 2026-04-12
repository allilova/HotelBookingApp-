package com.example.hotelbookingapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * NotificationHelper sends push notifications to specific devices via
 * the Firebase Cloud Messaging (FCM) legacy HTTP API.
 *
 * Why the legacy HTTP API and not FCM HTTP v1?
 * The v1 API requires an OAuth 2.0 access token generated from a service
 * account JSON file. Embedding a service account JSON in an APK is a
 * serious security risk and should never be done in production.
 *
 * The legacy API uses a simple Server Key which is slightly safer for a
 * course project, though in production this would move to a backend server
 * or Firebase Cloud Functions.
 *
 * FCM legacy API endpoint:
 *   POST https://fcm.googleapis.com/fcm/send
 *   Authorization: key=YOUR_SERVER_KEY
 *   Content-Type: application/json
 *
 * Payload format:
 *   {
 *     "to": "RECIPIENT_FCM_TOKEN",
 *     "notification": {
 *       "title": "...",
 *       "body": "..."
 *     },
 *     "data": {
 *       "title": "...",
 *       "body": "..."
 *     }
 *   }
 *
 * We send both "notification" and "data" payloads because:
 *   - "notification" payload: shown automatically when app is in background
 *   - "data" payload: read by HotelBookingFCMService when app is in foreground
 */
object NotificationHelper {

    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

    /**
     * Sends a push notification to a specific device identified by its FCM token.
     *
     * This function makes a network request so it MUST be called from a
     * coroutine. It switches to Dispatchers.IO internally so the caller
     * can call it from any coroutine context.
     *
     * @param recipientToken  FCM token of the recipient's device.
     *                        Fetched from Firestore users/{uid}/fcmToken.
     * @param title           Notification title.
     * @param body            Notification body text.
     *
     * Silently returns if:
     *  - recipientToken is blank (user has no token — e.g. never logged in on this device)
     *  - FCM_SERVER_KEY is blank (key not configured in local.properties)
     *  - Network request fails (we don't want notification failures to crash the booking flow)
     */
    suspend fun sendNotification(
        recipientToken: String,
        title:          String,
        body:           String
    ) {
        // Don't attempt to send if we don't have the required values
        if (recipientToken.isBlank()) return
        if (BuildConfig.FCM_SERVER_KEY.isBlank()) return

        withContext(Dispatchers.IO) {
            try {
                val url = URL(FCM_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "key=${BuildConfig.FCM_SERVER_KEY}")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10_000  // 10 second connection timeout
                    readTimeout    = 10_000  // 10 second read timeout
                }

                // Build the FCM JSON payload.
                // We send both "notification" (for background) and "data" (for foreground).
                val payload = JSONObject().apply {
                    put("to", recipientToken)
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body",  body)
                        put("sound", "default")
                    })
                    put("data", JSONObject().apply {
                        put("title", title)
                        put("body",  body)
                    })
                }

                // Write the payload to the connection output stream
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                // Read the response code — 200 means success
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Log but don't throw — notification failure shouldn't
                    // affect the booking flow
                    android.util.Log.w(
                        "NotificationHelper",
                        "FCM send failed with response code: $responseCode"
                    )
                }

                connection.disconnect()

            } catch (e: Exception) {
                // Network errors, timeouts, etc.
                // We log but do not rethrow — notification failure is non-critical
                android.util.Log.e(
                    "NotificationHelper",
                    "Failed to send FCM notification: ${e.message}"
                )
            }
        }
    }
}