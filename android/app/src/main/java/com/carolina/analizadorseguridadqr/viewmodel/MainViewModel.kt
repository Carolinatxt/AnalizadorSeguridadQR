package com.carolina.analizadorseguridadqr.viewmodel

import androidx.lifecycle.ViewModel
import com.carolina.analizadorseguridadqr.ui.state.ScanUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ViewModel: concentra el estado de pantalla para que la Activity quede limpia.
class MainViewModel : ViewModel() {
    // Mutable interno: solo el ViewModel puede cambiar el estado.
    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)

    // Exponemos solo lectura para evitar cambios desde fuera.
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Simula que el usuario empieza un escaneo.
    fun onScanButtonClicked() {
        _uiState.value = ScanUiState.Loading
    }

    fun showIdle() {
        // Reinicia la pantalla al punto de inicio.
        _uiState.value = ScanUiState.Idle
    }

    fun showMockReadyUrl() {
        // Dato de ejemplo para validar la UI sin usar camara ni backend.
        _uiState.value = ScanUiState.ReadyToAnalyze(
            url = "https://ejemplo.com/oferta-qr"
        )
    }

    fun showMockResult() {
        // Resultado simulado para comprobar como se renderiza la tarjeta.
        _uiState.value = ScanUiState.AnalysisResult(
            riskLevel = "suspicious",
            analysisStatus = "partial",
            summary = "Resultado mock para validar la UI sin backend.",
        )
    }

    fun showError(message: String) {
        // Permite mostrar errores de forma controlada en la UI.
        _uiState.value = ScanUiState.Error(message)
    }
}
