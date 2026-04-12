package com.example.hotelbookingapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * FirebaseAuthManager is a singleton that owns all Firebase Authentication
 * and user-profile (Firestore) operations.
 */
object FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // ── Current user ──────────────────────────────────────────────────────────

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUid: String?
        get() = auth.currentUser?.uid

    // ── Registration ──────────────────────────────────────────────────────────

    suspend fun register(
        fullName: String,
        email:    String,
        password: String,
        role:     UserRole
    ): User {
        val authResult = auth
            .createUserWithEmailAndPassword(email.trim().lowercase(), password)
            .await()

        val uid = authResult.user?.uid
            ?: throw Exception("Registration succeeded but UID is null.")

        val user = User(
            fullName  = fullName.trim(),
            email     = email.trim().lowercase(),
            role      = role.name,
            createdAt = System.currentTimeMillis(),
            points    = 0
        )

        usersCollection.document(uid).set(user.toMap()).await()

        // Save FCM token immediately after registration so the user
        // can receive notifications right away
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            usersCollection.document(uid)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {
            // Non-critical — token will be saved on next onNewToken() call
        }

        return user
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): User {
        auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()

        // Refresh the FCM token after every login.
        // The token may have changed if the user reinstalled the app or
        // cleared app data since their last login.
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            updateFcmToken(token)
        } catch (e: Exception) {
            // Non-critical — login still succeeds without a fresh token
        }

        return fetchCurrentUserProfile()
            ?: throw Exception("Login succeeded but user profile not found in Firestore.")
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout() {
        auth.signOut()
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    suspend fun fetchCurrentUserProfile(): User? {
        val uid = currentUid ?: return null
        val snapshot = usersCollection.document(uid).get().await()
        if (!snapshot.exists()) return null

        return User(
            fullName  = snapshot.getString("fullName")  ?: "",
            email     = snapshot.getString("email")     ?: "",
            role      = snapshot.getString("role")      ?: UserRole.GUEST.name,
            createdAt = snapshot.getLong("createdAt")   ?: System.currentTimeMillis(),
            fcmToken  = snapshot.getString("fcmToken")  ?: "",
            points    = (snapshot.getLong("points")     ?: 0L).toInt()
        )
    }

    suspend fun isHost(): Boolean {
        val user = fetchCurrentUserProfile() ?: return false
        return user.role == UserRole.HOST.name
    }

    // ── FCM Token ─────────────────────────────────────────────────────────────

    /**
     * Saves this device's FCM token to Firestore at users/{uid}/fcmToken.
     * Called after login, registration, and whenever Firebase refreshes the token.
     */
    suspend fun updateFcmToken(token: String) {
        val uid = currentUid ?: return
        usersCollection.document(uid)
            .update("fcmToken", token)
            .await()
    }

    /**
     * Fetches the FCM token of ANY user by their Firebase UID.
     * Used by BookingRepository to find the recipient's token before
     * sending a push notification.
     *
     * Returns null if the user document doesn't exist or has no token.
     */
    suspend fun getFcmTokenForUser(uid: String): String? {
        if (uid.isBlank()) return null
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.getString("fcmToken")
        } catch (e: Exception) {
            null
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun User.toMap(): Map<String, Any> = mapOf(
        "fullName"  to fullName,
        "email"     to email,
        "role"      to role,
        "createdAt" to createdAt,
        "fcmToken"  to "",  // will be updated immediately after
        "points"    to points
    )
}