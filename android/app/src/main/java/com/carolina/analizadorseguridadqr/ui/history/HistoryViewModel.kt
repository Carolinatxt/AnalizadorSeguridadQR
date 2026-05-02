package com.carolina.analizadorseguridadqr.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carolina.analizadorseguridadqr.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyRepository: ScanHistoryRepository,
) : ViewModel() {
    // WhileSubscribed(5_000) evita reiniciar la consulta a Room si la UI
    // se desuscribe brevemente, por ejemplo durante una rotacion.
    val historyItems: StateFlow<List<HistoryUiItem>> =
        historyRepository.observeRecentScans()
            .map { entities ->
                entities.map { it.toHistoryUiItem() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}
