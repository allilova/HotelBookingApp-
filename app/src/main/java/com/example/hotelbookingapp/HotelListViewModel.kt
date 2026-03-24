package com.example.hotelbookingapp

import androidx.lifecycle.ViewModel
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

class HotelListViewModel : ViewModel() {

    private val allHotels = HotelRepository.getHotels()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sortOrder = MutableStateFlow(SortOrder.NONE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder


    val uiState: StateFlow<ListUiState> =
        combine(_query, _sortOrder) { query, sort ->
            var list = if (query.isBlank()) allHotels
            else allHotels.filter {
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState(allHotels))

    fun onQueryChanged(q: String) { _query.value = q }
    fun onSortChanged(order: SortOrder) { _sortOrder.value = order }
}