package com.example.ais_sst_mobile.presentation.home.details

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EventDetailUiModel(
    val id: Int,
    val title: String,
    val dateStr: String,
    val venue: String,
    val description: String,
    val imageUrl: String? = null
)

class UpcomingEventDetailsScreenModel : ViewModel() {
    private val _event = MutableStateFlow<EventDetailUiModel?>(null)
    val event = _event.asStateFlow()

    fun loadEvent(id: Int) {
        _event.value = EventDetailUiModel(
            id = id,
            title = "Вечер талантов",
            dateStr = "15 мая, 18:00 - 21:00",
            venue = "Кронштадтский бульвар, 37Б, Актовый зал",
            description = "Присоединяйтесь к нам на ежегодном вечере талантов, где студенты могут продемонстрировать свои таланты в различных областях, от вокала до танцев и юмора."
        )
    }
}