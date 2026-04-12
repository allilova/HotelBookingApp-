package com.example.hotelbookingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DbResult<out T> {
    data class Success<T>(val data: T) : DbResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : DbResult<Nothing>()
}

class HotelDetailViewModel(app: Application) : AndroidViewModel(app) {

    // Room DB is still needed for FavoriteHotel only
    private val db = DatabaseProvider.get(app)

    // ── Favourite state ───────────────────────────────────────────────────────

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite

    // ── Events ────────────────────────────────────────────────────────────────

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent

    private val _bookingSaved = MutableSharedFlow<Booking>()
    val bookingSaved: SharedFlow<Booking> = _bookingSaved

    // ── Favourite operations ──────────────────────────────────────────────────

    /**
     * Loads the favourite state for a hotel from Room.
     * MUST run on Dispatchers.IO because Room is a blocking operation.
     */
    fun loadFavouriteState(hotelId: Int) {
        viewModelScope.launch {
            when (val result = safeIoCall { db.hotelDao().getFavoriteById(hotelId) }) {
                is DbResult.Success -> _isFavourite.value = result.data != null
                is DbResult.Error   -> _errorEvent.emit(result.message)
            }
        }
    }

    /**
     * Toggles the favourite state for a hotel in Room.
     * MUST run on Dispatchers.IO because Room is a blocking operation.
     */
    fun toggleFavourite(hotel: FavoriteHotel) {
        viewModelScope.launch {
            val op: suspend () -> Unit = if (_isFavourite.value) {
                { db.hotelDao().deleteFavorite(hotel) }
            } else {
                { db.hotelDao().insertFavorite(hotel) }
            }
            when (val result = safeIoCall { op() }) {
                is DbResult.Success -> _isFavourite.value = !_isFavourite.value
                is DbResult.Error   -> _errorEvent.emit(result.message)
            }
        }
    }

    // ── Booking operation (Firestore) ─────────────────────────────────────────

    /**
     * Saves a new booking to Firestore via BookingRepository.
     * Firestore handles its own threading so no dispatcher switch needed.
     */
    fun saveBooking(booking: Booking) {
        viewModelScope.launch {
            when (val result = safeCall { BookingRepository.createBooking(booking) }) {
                is DbResult.Success -> _bookingSaved.emit(result.data)
                is DbResult.Error   -> _errorEvent.emit(result.message)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Runs a Room (blocking) operation on Dispatchers.IO.
     * Always use this for any Room call.
     */
    private suspend fun <T> safeIoCall(block: suspend () -> T): DbResult<T> {
        return try {
            val result = withContext(Dispatchers.IO) { block() }
            DbResult.Success(result)
        } catch (e: Exception) {
            DbResult.Error(
                message = e.localizedMessage ?: "Непозната грешка",
                cause   = e
            )
        }
    }

    /**
     * Runs a Firestore (non-blocking, handles own threading) operation.
     * Use this for Firestore calls — no dispatcher switch needed.
     */
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