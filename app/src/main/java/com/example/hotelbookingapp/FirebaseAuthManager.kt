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

    /**
     * Registers a new user and saves their profile + FCM token.
     */
    suspend fun register(
        fullName: String,
        email:    String,
        password: String,
        role:     UserRole
    ): User {
        // Step 1: Create the Firebase Auth account.
        val authResult = auth
            .createUserWithEmailAndPassword(email.trim().lowercase(), password)
            .await()

        val uid = authResult.user?.uid
            ?: throw Exception("Registration succeeded but UID is null — please try again.")

        // Step 2: Build the user profile.
        val user = User(
            fullName  = fullName.trim(),
            email     = email.trim().lowercase(),
            role      = role.name,
            createdAt = System.currentTimeMillis(),
            points    = 0
        )

        // Step 3: Write to Firestore.
        usersCollection.document(uid).set(user.toMap()).await()

        // Step 4: Save the FCM token so the user can receive notifications immediately.
        try {
            FirebaseMessaging.getInstance()
                .token
                .await()
                .let { token ->
                    usersCollection.document(uid)
                        .update("fcmToken", token)
                        .await()
                }
        } catch (e: Exception) {
            // Non-critical — token will be saved on next onNewToken() call
        }

        return user
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Logs in the user and refreshes their FCM token in Firestore.
     */
    suspend fun login(email: String, password: String): User {
        auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()

        // After successful login, refresh the FCM token in Firestore.
        // The token may have changed since the last login (e.g. app reinstall).
        try {
            FirebaseMessaging.getInstance()
                .token
                .await()
                .let { token -> updateFcmToken(token) }
        } catch (e: Exception) {
            // Token refresh is non-critical — login still succeeds
        }

        return fetchCurrentUserProfile()
            ?: throw Exception("Login succeeded but user profile not found in Firestore.")
    }

    fun logout() {
        auth.signOut()
    }

    // ── Helper methods ────────────────────────────────────────────────────────

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

    suspend fun updateFcmToken(token: String) {
        val uid = currentUid ?: return
        usersCollection.document(uid)
            .update("fcmToken", token)
            .await()
    }

    suspend fun getFcmTokenForUser(uid: String): String? {
        val snapshot = usersCollection.document(uid).get().await()
        return snapshot.getString("fcmToken")
    }

    private fun User.toMap(): Map<String, Any> = mapOf(
        "fullName"  to fullName,
        "email"     to email,
        "role"      to role,
        "createdAt" to createdAt,
        "fcmToken"  to fcmToken,
        "points"    to points
    )
}