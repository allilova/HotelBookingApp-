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


object NotificationHelper {

    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCollection = firestore.collection("notifications")

    const val CHANNEL_ID = "booking_channel"

    // ── Remote notification (Firestore) ───────────────────────────────────────


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