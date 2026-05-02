package com.example.hotelbookingapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOrder { NONE, PRICE_ASC, PRICE_DESC, RATING_DESC, RANDOM }

data class ListUiState(
    val hotels: List<Hotel> = emptyList(),
    val isEmpty: Boolean    = false
)

class HotelListViewModel(private val app: Application) : AndroidViewModel(app) {

    private var resourceContext: Context = app.applicationContext

    fun setResourceContext(context: Context) {
        resourceContext = context
        triggerReload()
    }

    private val _query      = MutableStateFlow("")
    private val _sortOrder  = MutableStateFlow(SortOrder.NONE)
    private val _allHotels  = MutableStateFlow<List<Hotel>>(emptyList())

    val query:     StateFlow<String>     = _query
    val sortOrder: StateFlow<SortOrder>  = _sortOrder

    init {
        triggerReload()
    }

    fun triggerReload() {
        viewModelScope.launch {
            try {
                val hotels = withContext(Dispatchers.IO) {
                    HotelRepository.getAllHotels(resourceContext)
                }
                _allHotels.value = hotels
            } catch (e: Exception) {
                if (_allHotels.value.isEmpty()) {
                    _allHotels.value = HotelRepository.getStaticHotels(resourceContext)
                }
            }
        }
    }

    val uiState: StateFlow<ListUiState> =
        combine(_query, _sortOrder, _allHotels) { query, sort, all ->
            var list = if (query.isBlank()) all
            else all.filter {
                it.name.lowercase().contains(query.lowercase()) ||
                        it.city.lowercase().contains(query.lowercase())
            }
            list = when (sort) {
                SortOrder.PRICE_ASC   -> list.sortedBy { it.price }
                SortOrder.PRICE_DESC  -> list.sortedByDescending { it.price }
                SortOrder.RATING_DESC -> list.sortedByDescending { it.rating }
                SortOrder.RANDOM      -> list.shuffled()
                SortOrder.NONE        -> list
            }
            ListUiState(hotels = list, isEmpty = list.isEmpty())
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ListUiState()
        )

    fun onQueryChanged(q: String)       { _query.value     = q     }
    fun onSortChanged(order: SortOrder) { _sortOrder.value = order }

    fun shuffle() {
        _sortOrder.value = SortOrder.RANDOM
        triggerReload()
    }
}