package com.carolina.analizadorseguridadqr.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carolina.analizadorseguridadqr.data.repository.ScanHistoryRepository
import com.carolina.analizadorseguridadqr.network.AnalysisService
import com.carolina.analizadorseguridadqr.security.UnsupportedWebUrlReason
import com.carolina.analizadorseguridadqr.security.getUnsupportedWebUrlReason
import com.carolina.analizadorseguridadqr.ui.state.AnalysisStatus
import com.carolina.analizadorseguridadqr.ui.state.RiskLevel
import com.carolina.analizadorseguridadqr.ui.state.ScanUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

// ViewModel: concentra el estado de pantalla para que la Activity quede limpia.
class MainViewModel(
    private val historyRepository: ScanHistoryRepository,
    private val analysisService: AnalysisService = AnalysisService(),
) : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
        private const val MAX_LOG_VALUE_LENGTH = 40
    }

    // Mutable interno: solo el ViewModel puede cambiar el estado.
    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)

    // Exponemos solo lectura para evitar cambios desde fuera.
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()
    // Guarda la última URL web válida para el botón "Reintentar".
    private var lastValidWebUrl: String? = null
    // Evita lanzar varias peticiones al backend por dobles toques del usuario.
    private var isAnalyzing: Boolean = false

    // El usuario ha pulsado el botón de escanear.
    // No cambiamos estado aquí porque Loading se usa solo al consultar el backend.
    fun onScanButtonClicked() {
    }

    fun showIdle() {
        // Decisión de UX: volver al inicio reinicia todo el flujo y exige reescanear.
        _uiState.value = ScanUiState.Idle
        lastValidWebUrl = null
    }

    fun showError(message: String) {
        // Permite mostrar errores de forma controlada en la UI.
        _uiState.value = ScanUiState.Error(message)
    }

    // Recibe el texto bruto del QR y aplica una validación funcional mínima.
    fun onScanResult(rawValue: String?) {
        val content = rawValue?.trim()
        if (content.isNullOrEmpty()) {
            _uiState.value = ScanUiState.NotAWebUrl(
                "No se detectó un enlace web válido en el código QR.",
            )
            return
        }

        if (!isValidWebUrl(content)) {
            _uiState.value = ScanUiState.NotAWebUrl(
                "El código QR no contiene un enlace web válido (http o https).",
            )
            return
        }

        // ReadyToAnalyze se mantiene como puente conceptual del flujo.
        lastValidWebUrl = content
        _uiState.value = ScanUiState.ReadyToAnalyze(content)
        analyzeUrl(content)
    }

    // Reintenta el análisis de la última URL válida conocida.
    fun retryLastAnalysis() {
        // Si ya hay una petición activa, ignoramos el toque extra.
        if (isAnalyzing) {
            Log.w(TAG, "Reintento ignorado: ya hay un análisis en curso.")
            return
        }

        val url = lastValidWebUrl
        if (url.isNullOrBlank()) {
            Log.w(TAG, "No hay URL válida reciente para reintentar análisis.")
            _uiState.value = ScanUiState.Error(
                "No hay un enlace válido reciente para reintentar. Escanea otro QR.",
            )
            return
        }

        analyzeUrl(url)
    }

    fun analyzeUrlFromHistory(url: String) {
        val trimmedUrl = url.trim()
        if (!isValidWebUrl(trimmedUrl)) {
            _uiState.value = ScanUiState.NotAWebUrl(
                "El enlace guardado ya no tiene un formato web válido.",
            )
            return
        }

        // Reutilizamos la misma URL validada para el flujo de reintento y análisis.
        Log.d(TAG, "Reanálisis solicitado desde historial")
        lastValidWebUrl = trimmedUrl
        analyzeUrl(trimmedUrl)
    }

    private fun analyzeUrl(url: String) {
        if (isAnalyzing) {
            Log.w(TAG, "Solicitud ignorada: ya hay un análisis en curso.")
            return
        }

        isAnalyzing = true
        viewModelScope.launch {
            _uiState.value = ScanUiState.Loading

            try {
                val response = analysisService.analyzeUrl(url)
                val riskLevel = mapRiskLevel(response.riskLevel)
                val analysisStatus = mapAnalysisStatus(response.analysisStatus)

                _uiState.value = ScanUiState.AnalysisResult(
                    riskLevel = riskLevel,
                    analysisStatus = analysisStatus,
                    summary = response.summary,
                    reasons = response.reasons,
                    analyzedUrl = url,
                )

                // Guardado secundario:
                // primero actualizamos la UI para no alargar Loading en móviles lentos.
                // Dejamos libre el candado de análisis antes de persistir, para evitar
                // bloquear acciones del usuario si Room tarda en dispositivos lentos.
                isAnalyzing = false
                historyRepository.saveAnalysisResult(
                    url = url,
                    riskLevel = riskLevel,
                    analysisStatus = analysisStatus,
                    summary = response.summary,
                    reasons = response.reasons,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                // Error controlado de red: warning para diagnóstico sin marcar fallo crítico.
                Log.w(TAG, "Error de conexión con backend", exception)
                _uiState.value = ScanUiState.Error(
                    "No se pudo conectar con el servicio de análisis.",
                )
            } catch (exception: HttpException) {
                // Error HTTP controlado: el servidor respondió, pero no en estado exitoso.
                Log.w(TAG, "Error HTTP backend: ${exception.code()}", exception)
                _uiState.value = ScanUiState.Error(
                    "No se pudo completar el análisis. Inténtalo de nuevo.",
                )
            } catch (exception: Exception) {
                // Error no previsto: se registra como error para facilitar investigación.
                Log.e(TAG, "Error inesperado al analizar URL", exception)
                _uiState.value = ScanUiState.Error(
                    "No se pudo completar el análisis. Inténtalo de nuevo.",
                )
            } finally {
                isAnalyzing = false
            }
        }
    }

    private fun mapRiskLevel(value: String): RiskLevel {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "safe" -> RiskLevel.SAFE
            "suspicious" -> RiskLevel.SUSPICIOUS
            "dangerous" -> RiskLevel.DANGEROUS
            else -> {
                Log.w(TAG, "riskLevel desconocido del backend: '${safeLogValue(value)}'")
                RiskLevel.UNKNOWN
            }
        }
    }

    private fun mapAnalysisStatus(value: String): AnalysisStatus {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "complete" -> AnalysisStatus.COMPLETE
            "partial" -> AnalysisStatus.PARTIAL
            "unavailable" -> AnalysisStatus.UNAVAILABLE
            else -> {
                Log.w(TAG, "analysisStatus desconocido del backend: '${safeLogValue(value)}'")
                AnalysisStatus.UNKNOWN
            }
        }
    }

    // Fuente unica de verdad para aceptar solo enlaces web HTTP/HTTPS con host.
    private fun isValidWebUrl(value: String): Boolean {
        return when (getUnsupportedWebUrlReason(value)) {
            null -> true
            UnsupportedWebUrlReason.TOO_LONG -> {
                Log.w(TAG, "URL rechazada: supera la longitud máxima permitida.")
                false
            }
            UnsupportedWebUrlReason.CONTROL_CHARS -> {
                Log.w(TAG, "URL rechazada: contiene caracteres de control.")
                false
            }
            UnsupportedWebUrlReason.USERINFO -> {
                Log.w(TAG, "URL rechazada: contiene userinfo.")
                false
            }
            UnsupportedWebUrlReason.PARSE_ERROR -> {
                Log.w(TAG, "Uri.parse falló al validar URL web.")
                false
            }
            UnsupportedWebUrlReason.UNSUPPORTED_WEB_TARGET -> {
                false
            }
        }
    }

    private fun safeLogValue(value: String): String {
        return value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .trim()
            .take(MAX_LOG_VALUE_LENGTH)
    }
}

