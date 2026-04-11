package com.example.hotelbookingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DbResult<out T> {
    data class Success<T>(val data: T) : DbResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : DbResult<Nothing>()
}

class HotelDetailViewModel(app: Application) : AndroidViewModel(app) {

    // Room DB is still needed for FavoriteHotel
    private val db = DatabaseProvider.get(app)

    // ── Favourite state ───────────────────────────────────────────────────────

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite

    // ── Events ────────────────────────────────────────────────────────────────

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent

    // bookingSaved emits the saved Booking (with firestoreId) on success,
    // or null is communicated via errorEvent on failure.
    private val _bookingSaved = MutableSharedFlow<Booking>()
    val bookingSaved: SharedFlow<Booking> = _bookingSaved

    // ── Favourite operations (still use Room) ─────────────────────────────────

    fun loadFavouriteState(hotelId: Int) {
        viewModelScope.launch {
            when (val result = safeCall { db.hotelDao().getFavoriteById(hotelId) }) {
                is DbResult.Success -> _isFavourite.value = result.data != null
                is DbResult.Error   -> _errorEvent.emit(result.message)
            }
        }
    }

    fun toggleFavourite(hotel: FavoriteHotel) {
        viewModelScope.launch {
            val op: suspend () -> Unit = if (_isFavourite.value) {
                { db.hotelDao().deleteFavorite(hotel) }
            } else {
                { db.hotelDao().insertFavorite(hotel) }
            }
            when (val result = safeCall { op() }) {
                is DbResult.Success -> _isFavourite.value = !_isFavourite.value
                is DbResult.Error   -> _errorEvent.emit(result.message)
            }
        }
    }


    fun saveBooking(booking: Booking) {
        viewModelScope.launch {
            when (val result = safeCall { BookingRepository.createBooking(booking) }) {
                is DbResult.Success -> _bookingSaved.emit(result.data)
                is DbResult.Error   -> {
                    _errorEvent.emit(result.message)
                }
            }
        }
    }


    private suspend fun <T> safeCall(block: suspend () -> T): DbResult<T> {
        return try {
            DbResult.Success(block())
        } catch (e: Exception) {
            DbResult.Error(
                message = e.localizedMessage ?: "Непозната грешка",
                cause   = e
            )
        }
    }
}