package com.example.hotelbookingapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await


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


        try {
            val token = FirebaseMessaging.getInstance().token.await()
            usersCollection.document(uid)
                .update("fcmToken", token)
                .await()
        } catch (e: Exception) {

        }

        return user
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): User {
        auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()


        try {
            val token = FirebaseMessaging.getInstance().token.await()
            updateFcmToken(token)
        } catch (e: Exception) {

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


    suspend fun updateFcmToken(token: String) {
        val uid = currentUid ?: return
        usersCollection.document(uid)
            .update("fcmToken", token)
            .await()
    }


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