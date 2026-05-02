package com.example.hotelbookingapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


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