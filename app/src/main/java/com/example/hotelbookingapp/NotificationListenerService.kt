package com.example.hotelbookingapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * NotificationListenerService is a foreground-capable background service
 * that listens for new notification documents in Firestore for the
 * currently logged-in user.
 *
 * When a new unread notification arrives (read == false), it:
 *  1. Shows a system notification using NotificationHelper.showLocalNotification()
 *  2. Marks the notification as read (read = true) so it is not shown again
 *
 * Lifecycle:
 *  - Started in MainActivity.onResume()
 *  - Stopped in MainActivity.onDestroy()
 *
 * Firestore query:
 *   notifications WHERE recipientUid == currentUid
 *               AND read == false
 *               ORDER BY createdAt DESC
 */
class NotificationListenerService : Service() {

    private var listenerRegistration: ListenerRegistration? = null
    private val firestore = FirebaseFirestore.getInstance()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startListening()
        // START_STICKY means the service will be restarted if killed by the system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    /**
     * Registers a Firestore real-time listener for unread notifications
     * belonging to the current user.
     *
     * The listener fires immediately with any existing unread notifications,
     * and then again every time a new notification document is added.
     */
    private fun startListening() {
        val uid = FirebaseAuthManager.currentUid ?: return

        listenerRegistration = firestore
            .collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documentChanges) {
                    // Only react to newly ADDED documents, not modifications
                    if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val title = doc.document.getString("title") ?: continue
                        val body  = doc.document.getString("body")  ?: ""

                        // Show the system notification
                        NotificationHelper.showLocalNotification(this, title, body)

                        // Mark as read so it is not shown again on next app launch
                        firestore.collection("notifications")
                            .document(doc.document.id)
                            .update("read", true)
                    }
                }
            }
    }

    private fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}