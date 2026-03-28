package com.example.hotelbookingapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

enum class SortOrder { NONE, PRICE_ASC, PRICE_DESC, RATING_DESC }

data class ListUiState(
    val hotels: List<Hotel> = emptyList(),
    val isEmpty: Boolean = false
)

class HotelListViewModel(private val app: Application) : AndroidViewModel(app) {


    private var resourceContext: Context = app.applicationContext


    fun setResourceContext(context: Context) {
        resourceContext = context
        // Force the Flow to re-emit with the new context by toggling a trigger.
        _reload.value = !_reload.value
    }

    private fun loadHotels() = HotelRepository.getHotels(resourceContext)

    private val _query     = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.NONE)
    private val _reload    = MutableStateFlow(false)   // locale-change trigger

    val query: StateFlow<String>        = _query
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    val uiState: StateFlow<ListUiState> =
        combine(_query, _sortOrder, _reload) { query, sort, _ ->
            var list = if (query.isBlank()) loadHotels()
            else loadHotels().filter {
                it.name.lowercase().contains(query.lowercase()) ||
                        it.city.lowercase().contains(query.lowercase())
            }
            list = when (sort) {
                SortOrder.PRICE_ASC   -> list.sortedBy { it.price }
                SortOrder.PRICE_DESC  -> list.sortedByDescending { it.price }
                SortOrder.RATING_DESC -> list.sortedByDescending { it.rating }
                SortOrder.NONE        -> list
            }
            ListUiState(hotels = list, isEmpty = list.isEmpty())
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ListUiState(loadHotels())
        )

    fun onQueryChanged(q: String)       { _query.value     = q     }
    fun onSortChanged(order: SortOrder) { _sortOrder.value = order }
}