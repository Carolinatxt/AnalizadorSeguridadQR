package com.carolina.analizadorseguridadqr.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.carolina.analizadorseguridadqr.data.repository.ScanHistoryRepository

class HistoryViewModelFactory(
    private val historyRepository: ScanHistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(historyRepository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
