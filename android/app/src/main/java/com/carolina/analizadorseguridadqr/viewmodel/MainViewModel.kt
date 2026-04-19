package com.carolina.analizadorseguridadqr.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carolina.analizadorseguridadqr.network.AnalysisService
import com.carolina.analizadorseguridadqr.ui.state.ScanUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// ViewModel: concentra el estado de pantalla para que la Activity quede limpia.
class MainViewModel(
    private val analysisService: AnalysisService = AnalysisService(),
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

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

        // Si pasa el filtro, marcamos URL valida y lanzamos analisis real.
        _uiState.value = ScanUiState.ReadyToAnalyze(content)
        analyzeUrl(content)
    }

    private fun analyzeUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Loading

            try {
                val response = analysisService.analyzeUrl(url)
                _uiState.value = ScanUiState.AnalysisResult(
                    riskLevel = response.riskLevel,
                    analysisStatus = response.analysisStatus,
                    summary = response.summary,
                )
            } catch (exception: IOException) {
                Log.d(TAG, "Error de conexion con backend: ${exception.message}")
                _uiState.value = ScanUiState.Error(
                    "No se pudo conectar con el servicio de analisis."
                )
            } catch (exception: HttpException) {
                Log.d(TAG, "Error HTTP backend: ${exception.code()}")
                _uiState.value = ScanUiState.Error(
                    "No se pudo completar el analisis. Intentalo de nuevo."
                )
            } catch (exception: Exception) {
                Log.d(TAG, "Error inesperado al analizar URL: ${exception.message}")
                _uiState.value = ScanUiState.Error(
                    "No se pudo completar el analisis. Intentalo de nuevo."
                )
            }
        }
    }
}
