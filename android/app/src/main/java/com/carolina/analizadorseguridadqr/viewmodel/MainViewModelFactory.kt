package com.carolina.analizadorseguridadqr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.carolina.analizadorseguridadqr.data.repository.ScanHistoryRepository

class MainViewModelFactory(
    private val historyRepository: ScanHistoryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(historyRepository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
