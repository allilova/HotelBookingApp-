package com.example.hotelbookingapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val db   = DatabaseProvider.get(app)
    private val pref = app.getSharedPreferences("HotelAppPrefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent



    fun isLoggedIn(): Boolean = pref.getInt("logged_in_user_id", -1) != -1

    fun getLoggedInUserId(): Int = pref.getInt("logged_in_user_id", -1)

    fun logout() {
        pref.edit().remove("logged_in_user_id").apply()
    }

    fun getLoggedInUser(onResult: (User?) -> Unit) {
        val id = getLoggedInUserId()
        if (id == -1) { onResult(null); return }
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) { db.userDao().getUserById(id) }
            onResult(user)
        }
    }



    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _errorEvent.emit("Всички полета са задължителни.") }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModelScope.launch { _errorEvent.emit("Невалиден имейл адрес.") }
            return
        }
        if (password.length < 6) {
            viewModelScope.launch { _errorEvent.emit("Паролата трябва да е поне 6 символа.") }
            return
        }
        if (password != confirmPassword) {
            viewModelScope.launch { _errorEvent.emit("Паролите не съвпадат.") }
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = withContext(Dispatchers.IO) {
                try {
                    val existing = db.userDao().getUserByEmail(email.trim().lowercase())
                    if (existing != null) return@withContext DbResult.Error("Имейлът вече е регистриран.")
                    val user = User(
                        fullName     = fullName.trim(),
                        email        = email.trim().lowercase(),
                        passwordHash = sha256(password)
                    )
                    val newId = db.userDao().insertUser(user)
                    DbResult.Success(user.copy(id = newId.toInt()))
                } catch (e: Exception) {
                    DbResult.Error(e.localizedMessage ?: "Грешка при регистрация.")
                }
            }
            when (result) {
                is DbResult.Success -> {
                    saveSession(result.data.id)
                    _authState.value = AuthState.Success(result.data)
                }
                is DbResult.Error -> {
                    _authState.value = AuthState.Idle
                    _errorEvent.emit(result.message)
                }
            }
        }
    }


    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _errorEvent.emit("Въведи имейл и парола.") }
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = withContext(Dispatchers.IO) {
                try {
                    val user = db.userDao().getUserByEmail(email.trim().lowercase())
                        ?: return@withContext DbResult.Error("Потребителят не е намерен.")
                    if (user.passwordHash != sha256(password))
                        return@withContext DbResult.Error("Грешна парола.")
                    DbResult.Success(user)
                } catch (e: Exception) {
                    DbResult.Error(e.localizedMessage ?: "Грешка при вход.")
                }
            }
            when (result) {
                is DbResult.Success -> {
                    saveSession(result.data.id)
                    _authState.value = AuthState.Success(result.data)
                }
                is DbResult.Error -> {
                    _authState.value = AuthState.Idle
                    _errorEvent.emit(result.message)
                }
            }
        }
    }


    private fun saveSession(userId: Int) {
        pref.edit().putInt("logged_in_user_id", userId).apply()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}