package com.example.hotelbookingapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * FirebaseAuthManager is a singleton that owns all Firebase Authentication
 * and user-profile (Firestore) operations.
 *
 * Why a singleton and not injected?
 * For a course project this keeps things simple — FirebaseAuth and
 * FirebaseFirestore are themselves singletons managed by the Firebase SDK,
 * so wrapping them in our own singleton adds no extra state.
 *
 * Responsibilities:
 *  1. Register a new user with email + password via FirebaseAuth
 *  2. Write the user profile (name, role, etc.) to Firestore users/{uid}
 *  3. Login with email + password via FirebaseAuth
 *  4. Logout
 *  5. Fetch the current user's profile from Firestore
 *  6. Update the FCM token in Firestore so push notifications can be sent
 *  7. Expose the currently logged-in FirebaseUser
 */
object FirebaseAuthManager {

    // FirebaseAuth handles email/password authentication.
    // It persists the session automatically — after login the user stays
    // logged in even after the app is killed and restarted.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // FirebaseFirestore is where we store the user profile fields that
    // FirebaseAuth doesn't store natively (fullName, role, points, fcmToken).
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Convenience reference to the Firestore "users" collection.
    // Each document ID = the Firebase UID of the user.
    private val usersCollection = firestore.collection("users")

    // ── Current user ──────────────────────────────────────────────────────────

    /**
     * Returns the currently signed-in FirebaseUser, or null if nobody is
     * logged in. This is the Firebase equivalent of our old SharedPreferences
     * "logged_in_user_id" check.
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Returns true if a user is currently signed in.
     * Replaces: pref.getInt("logged_in_user_id", -1) != -1
     */
    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Returns the Firebase UID of the currently signed-in user, or null.
     * This replaces our old integer user ID from Room.
     */
    val currentUid: String?
        get() = auth.currentUser?.uid

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a new user with Firebase Authentication and then writes
     * their profile to Firestore.
     *
     * Steps:
     *  1. Call FirebaseAuth.createUserWithEmailAndPassword — this creates
     *     the auth record and signs the user in automatically.
     *  2. Get the new user's UID from the result.
     *  3. Write a User document to Firestore at users/{uid}.
     *
     * @throws Exception if email is already in use, password is too weak,
     *                   or Firestore write fails.
     * @return The newly created User data class (not a Room entity).
     */
    suspend fun register(
        fullName: String,
        email:    String,
        password: String,
        role:     UserRole
    ): User {
        // Step 1: Create the Firebase Auth account.
        // .await() is a Kotlin coroutine extension from
        // kotlinx-coroutines-play-services that suspends until the Task completes.
        val authResult = auth
            .createUserWithEmailAndPassword(email.trim().lowercase(), password)
            .await()

        // Step 2: authResult.user is never null after a successful registration,
        // but we throw explicitly to give a clear error message if something
        // unexpected happened.
        val uid = authResult.user?.uid
            ?: throw Exception("Registration succeeded but UID is null — please try again.")

        // Step 3: Build the user profile and write it to Firestore.
        val user = User(
            fullName  = fullName.trim(),
            email     = email.trim().lowercase(),
            role      = role.name,
            createdAt = System.currentTimeMillis(),
            points    = 0
        )


        usersCollection.document(uid).set(user.toMap()).await()

        return user
    }


    suspend fun login(email: String, password: String): User {

        auth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()


        return fetchCurrentUserProfile()
            ?: throw Exception("Login succeeded but user profile not found in Firestore.")
    }


    fun logout() {
        auth.signOut()
    }



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