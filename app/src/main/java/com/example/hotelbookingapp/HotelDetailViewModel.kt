package com.example.hotelbookingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HotelDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val db = DatabaseProvider.get(app)

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite

    fun loadFavouriteState(hotelId: Int) {
        viewModelScope.launch {
            _isFavourite.value = withContext(Dispatchers.IO) {
                db.hotelDao().getFavoriteById(hotelId) != null
            }
        }
    }

    fun toggleFavourite(hotel: FavoriteHotel) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (_isFavourite.value) {
                    db.hotelDao().deleteFavorite(hotel)
                } else {
                    db.hotelDao().insertFavorite(hotel)
                }
            }
            _isFavourite.value = !_isFavourite.value
        }
    }

    fun saveBooking(booking: Booking) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.bookingDao().insertBooking(booking)
            }
        }
    }
}