package com.example.hotelbookingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * AuthViewModel manages the authentication UI state for LoginActivity
 * and RegisterActivity.
 *
 * All Firebase Auth calls go through FirebaseAuthManager which handles
 * the actual Firebase SDK interactions. AuthViewModel's job is to:
 *  1. Validate input before calling FirebaseAuthManager
 *  2. Translate Firebase exceptions into human-readable Bulgarian/English messages
 *  3. Expose StateFlow and SharedFlow for the UI to observe
 *
 * Key difference from the old version:
 *  - No more Room DB calls, no more SHA-256, no more SharedPreferences session
 *  - Firebase persists the session automatically across app restarts
 *  - isLoggedIn() now checks FirebaseAuth.currentUser instead of SharedPreferences
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {

    // All auth operations are delegated to FirebaseAuthManager
    private val authManager = FirebaseAuthManager

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // SharedFlow for one-shot error events (Toast messages).
    // replay = 0 means a new observer won't receive old errors.
    private val _errorEvent = MutableSharedFlow<String>(replay = 0)
    val errorEvent: SharedFlow<String> = _errorEvent

    // ── Session checks ────────────────────────────────────────────────────────

    /**
     * Returns true if a Firebase user is currently signed in.
     * FirebaseAuth persists the session to disk so this returns true
     * even after the app is killed and restarted.
     *
     * Replaces: pref.getInt("logged_in_user_id", -1) != -1
     */
    fun isLoggedIn(): Boolean = authManager.isLoggedIn

    /**
     * Checks if the current user is a HOST by reading their Firestore profile.
     * This is a suspend function — call it from a coroutine or use
     * the isHostResult StateFlow pattern if you need it in the UI.
     */
    fun checkIsHost(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(authManager.isHost())
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Validates registration input and then calls FirebaseAuthManager.register().
     *
     * Validation rules (same as before):
     *  - All fields required
     *  - Valid email format
     *  - Password at least 6 characters (Firebase minimum)
     *  - Passwords must match
     *
     * Firebase-specific errors caught:
     *  - FirebaseAuthUserCollisionException: email already registered
     *  - FirebaseAuthWeakPasswordException: password too weak
     */
    fun register(
        fullName:        String,
        email:           String,
        password:        String,
        confirmPassword: String,
        role:            UserRole = UserRole.GUEST
    ) {
        // ── Input validation ──────────────────────────────────────────────────
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            emitError("Всички полета са задължителни.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emitError("Невалиден имейл адрес.")
            return
        }
        if (password.length < 6) {
            emitError("Паролата трябва да е поне 6 символа.")
            return
        }
        if (password != confirmPassword) {
            emitError("Паролите не съвпадат.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = authManager.register(fullName, email, password, role)
                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Idle
                // Translate Firebase error codes to human-readable messages
                val message = when (e) {
                    is FirebaseAuthUserCollisionException ->
                        "Имейлът вече е регистриран."
                    is FirebaseAuthWeakPasswordException ->
                        "Паролата е твърде слаба. Използвай поне 6 символа."
                    else ->
                        e.localizedMessage ?: "Грешка при регистрация."
                }
                _errorEvent.emit(message)
            }
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Validates login input and then calls FirebaseAuthManager.login().
     *
     * Firebase-specific errors caught:
     *  - FirebaseAuthInvalidUserException: no account with this email
     *  - FirebaseAuthInvalidCredentialsException: wrong password
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            emitError("Въведи имейл и парола.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = authManager.login(email, password)
                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Idle
                val message = when (e) {
                    is FirebaseAuthInvalidUserException ->
                        "Потребителят не е намерен."
                    is FirebaseAuthInvalidCredentialsException ->
                        "Грешна парола."
                    else ->
                        e.localizedMessage ?: "Грешка при вход."
                }
                _errorEvent.emit(message)
            }
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Signs out the current user.
     * FirebaseAuth clears the persisted session automatically.
     */
    fun logout() {
        authManager.logout()
    }

    // ── User profile ──────────────────────────────────────────────────────────

    /**
     * Fetches the logged-in user's profile from Firestore asynchronously
     * and delivers it via callback on the main thread.
     *
     * Used by UserProfileActivity to display name, email, role, points.
     */
    fun getLoggedInUser(onResult: (User?) -> Unit) {
        viewModelScope.launch {
            try {
                val user = authManager.fetchCurrentUserProfile()
                onResult(user)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Emits an error event from a non-coroutine context.
     * Uses launch because MutableSharedFlow.emit() is a suspend function.
     */
    private fun emitError(message: String) {
        viewModelScope.launch { _errorEvent.emit(message) }
    }
}