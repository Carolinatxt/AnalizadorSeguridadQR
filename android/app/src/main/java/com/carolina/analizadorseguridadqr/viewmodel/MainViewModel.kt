package com.carolina.analizadorseguridadqr.viewmodel

import android.net.Uri
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

    fun showError(message: String) {
        // Permite mostrar errores de forma controlada en la UI.
        _uiState.value = ScanUiState.Error(message)
    }

    // Recibe el texto bruto del QR y aplica una validacion funcional minima.
    fun onScanResult(rawValue: String?) {
        val content = rawValue?.trim()
        if (content.isNullOrEmpty()) {
            _uiState.value = ScanUiState.NotAWebUrl(
                "No se detecto un enlace web valido en el codigo QR."
            )
            return
        }

        val parsed = Uri.parse(content)
        val scheme = parsed.scheme?.lowercase()
        val hasWebScheme = scheme == "http" || scheme == "https"
        val hasHost = !parsed.host.isNullOrBlank()

        if (!hasWebScheme || !hasHost) {
            _uiState.value = ScanUiState.NotAWebUrl(
                "El codigo QR no contiene un enlace web valido (http o https)."
            )
            return
        }

        // Si pasa el filtro, queda listo para conectar backend en el siguiente paso.
        _uiState.value = ScanUiState.ReadyToAnalyze(content)
    }
}
