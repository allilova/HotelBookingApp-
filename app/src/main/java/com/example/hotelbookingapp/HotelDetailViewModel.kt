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

    private val db = DatabaseProvider.get(app)


    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent


    private val _bookingSaved = MutableSharedFlow<Boolean>()
    val bookingSaved: SharedFlow<Boolean> = _bookingSaved


    fun loadFavouriteState(hotelId: Int) {
        viewModelScope.launch {
            when (val result = safeDbCall { db.hotelDao().getFavoriteById(hotelId) }) {
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

            when (val result = safeDbCall { op() }) {
                is DbResult.Success -> _isFavourite.value = !_isFavourite.value
                is DbResult.Error   -> _errorEvent.emit(result.message)
            }
        }
    }

    fun saveBooking(booking: Booking) {
        viewModelScope.launch {
            when (val result = safeDbCall { db.bookingDao().insertBooking(booking) }) {
                is DbResult.Success -> _bookingSaved.emit(true)
                is DbResult.Error   -> {
                    _bookingSaved.emit(false)
                    _errorEvent.emit(result.message)
                }
            }
        }
    }

    private suspend fun <T> safeDbCall(block: suspend () -> T): DbResult<T> =
        withContext(Dispatchers.IO) {
            try {
                DbResult.Success(block())
            } catch (e: Exception) {
                DbResult.Error(
                    message = e.localizedMessage ?: "Непозната грешка с базата данни",
                    cause   = e
                )
            }
        }
}