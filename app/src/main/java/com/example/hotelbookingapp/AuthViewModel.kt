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

class AuthViewModel(app: Application) : AndroidViewModel(app) {


    private val authManager = FirebaseAuthManager

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    private val _errorEvent = MutableSharedFlow<String>(replay = 0)
    val errorEvent: SharedFlow<String> = _errorEvent

    // ── Session checks ────────────────────────────────────────────────────────

    fun isLoggedIn(): Boolean = authManager.isLoggedIn
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

    fun logout() {
        authManager.logout()
    }

    // ── User profile ──────────────────────────────────────────────────────────
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
    private fun emitError(message: String) {
        viewModelScope.launch { _errorEvent.emit(message) }
    }
}