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
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
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

    // Guarda la ultima URL web valida para el boton "Reintentar".
    private var lastValidWebUrl: String? = null

    // Evita lanzar varias peticiones al backend por dobles toques del usuario.
    private var isAnalyzing: Boolean = false

    // Permite cancelar el analisis anterior cuando llega una URL mas reciente.
    private var analysisJob: Job? = null

    // El usuario ha pulsado el boton de escanear.
    // No cambiamos estado aqui porque Loading se usa solo al consultar el backend.
    fun onScanButtonClicked() {
        analysisJob?.cancel()
        isAnalyzing = false

        if (_uiState.value is ScanUiState.Loading ||
            _uiState.value is ScanUiState.ReadyToAnalyze
        ) {
            _uiState.value = ScanUiState.Idle
        }
    }

    fun showIdle() {
        // Decision de UX: volver al inicio reinicia todo el flujo y exige reescanear.
        analysisJob?.cancel()
        isAnalyzing = false
        _uiState.value = ScanUiState.Idle
        lastValidWebUrl = null
    }

    fun showError(message: String) {
        // Permite mostrar errores de forma controlada en la UI.
        analysisJob?.cancel()
        isAnalyzing = false
        _uiState.value = ScanUiState.Error(message)
    }

    // Recibe el texto bruto del QR y lo envía al flujo comun de validacion y analisis.
    fun onScanResult(rawValue: String?) {
        handleUrlCandidate(
            rawValue = rawValue,
            invalidMessage = "Este QR no contiene una URL web válida.",
        )
    }

    fun onManualUrlSubmitted(input: String) {
        handleUrlCandidate(
            rawValue = input,
            invalidMessage = "Introduce una URL que empiece por http:// o https://.",
        )
    }

    fun onManualUrlChanged() {
        analysisJob?.cancel()
        isAnalyzing = false

        if (_uiState.value !is ScanUiState.Idle &&
            _uiState.value !is ScanUiState.ReadyToAnalyze
        ) {
            _uiState.value = ScanUiState.Idle
        }
    }

    // Reintenta el analisis de la ultima URL valida conocida.
    fun retryLastAnalysis() {
        analysisJob?.cancel()
        isAnalyzing = false

        val url = lastValidWebUrl
        if (url.isNullOrBlank()) {
            Log.w(TAG, "No hay URL válida reciente para reintentar análisis.")
            _uiState.value = ScanUiState.Error(
                "No hay un enlace válido reciente para reintentar. Escanea un QR o introduce una URL.",
            )
            return
        }

        analyzeUrl(url)
    }

    fun analyzeUrlFromHistory(url: String) {
        Log.d(TAG, "Reanalisis solicitado desde historial")
        handleUrlCandidate(
            rawValue = url,
            invalidMessage = "El enlace guardado ya no tiene un formato web válido.",
        )
    }

    private fun handleUrlCandidate(
        rawValue: String?,
        invalidMessage: String,
    ) {
        analysisJob?.cancel()
        isAnalyzing = false

        val cleanedValue = rawValue?.trim().orEmpty()
        if (cleanedValue.isBlank()) {
            _uiState.value = ScanUiState.Idle
            return
        }

        val unsupportedReason = getUnsupportedWebUrlReason(cleanedValue)
        if (unsupportedReason != null) {
            logUnsupportedWebUrlReason(unsupportedReason)
            _uiState.value = ScanUiState.NotAWebUrl(invalidMessage)
            return
        }

        lastValidWebUrl = cleanedValue
        _uiState.value = ScanUiState.ReadyToAnalyze(cleanedValue)
        analyzeUrl(cleanedValue)
    }

    private fun analyzeUrl(url: String) {
        analysisJob?.cancel()

        analysisJob = viewModelScope.launch {
            val currentJob = currentCoroutineContext()[Job]
            isAnalyzing = true
            var historySummary: String? = null
            var historyReasons: List<String>? = null
            var historyRiskLevel: RiskLevel? = null
            var historyAnalysisStatus: AnalysisStatus? = null

            try {
                _uiState.value = ScanUiState.Loading

                val response = analysisService.analyzeUrl(url)
                val riskLevel = mapRiskLevel(response.riskLevel)
                val analysisStatus = mapAnalysisStatus(response.analysisStatus)
                historyRiskLevel = riskLevel
                historyAnalysisStatus = analysisStatus
                historySummary = response.summary
                historyReasons = response.reasons

                _uiState.value = ScanUiState.AnalysisResult(
                    riskLevel = riskLevel,
                    analysisStatus = analysisStatus,
                    summary = response.summary,
                    reasons = response.reasons,
                    analyzedUrl = url,
                )

                // Solo el Job activo puede liberar el candado del analisis.
                if (analysisJob === currentJob) {
                    isAnalyzing = false
                }
            } catch (exception: CancellationException) {
                if (analysisJob === currentJob && _uiState.value is ScanUiState.Loading) {
                    _uiState.value = ScanUiState.Idle
                }
                // La cancelacion forma parte del flujo normal: hay que relanzarla para que
                // coroutines cierre correctamente este trabajo sin convertirlo en error funcional.
                throw exception
            } catch (exception: IOException) {
                // Error controlado de red: warning para diagnostico sin marcar fallo critico.
                Log.w(TAG, "Error de conexión con backend", exception)
                _uiState.value = ScanUiState.Error(
                    "No se pudo conectar con el servicio de análisis.",
                )
            } catch (exception: HttpException) {
                // Error HTTP controlado: el servidor respondio, pero no en estado exitoso.
                Log.w(TAG, "Error HTTP backend: ${exception.code()}", exception)
                _uiState.value = ScanUiState.Error(
                    "No se pudo completar el análisis. Inténtalo de nuevo.",
                )
            } catch (exception: Exception) {
                // Error no previsto: se registra para facilitar investigacion.
                Log.e(TAG, "Error inesperado al analizar URL", exception)
                _uiState.value = ScanUiState.Error(
                    "No se pudo completar el análisis. Inténtalo de nuevo.",
                )
            } finally {
                if (analysisJob === currentJob) {
                    isAnalyzing = false
                    analysisJob = null
                }
            }

            val summaryToSave = historySummary
            val reasonsToSave = historyReasons
            val riskLevelToSave = historyRiskLevel
            val analysisStatusToSave = historyAnalysisStatus
            if (
                summaryToSave != null &&
                reasonsToSave != null &&
                riskLevelToSave != null &&
                analysisStatusToSave != null
            ) {
                try {
                    historyRepository.saveAnalysisResult(
                        url = url,
                        riskLevel = riskLevelToSave,
                        analysisStatus = analysisStatusToSave,
                        summary = summaryToSave,
                        reasons = reasonsToSave,
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.w(
                        TAG,
                        "No se pudo guardar en historial: ${exception.javaClass.simpleName}",
                    )
                }
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

    private fun logUnsupportedWebUrlReason(reason: UnsupportedWebUrlReason) {
        when (reason) {
            UnsupportedWebUrlReason.TOO_LONG -> {
                Log.w(TAG, "URL rechazada: supera la longitud máxima permitida.")
            }
            UnsupportedWebUrlReason.CONTROL_CHARS -> {
                Log.w(TAG, "URL rechazada: contiene caracteres de control.")
            }
            UnsupportedWebUrlReason.USERINFO -> {
                Log.w(TAG, "URL rechazada: contiene userinfo.")
            }
            UnsupportedWebUrlReason.PARSE_ERROR -> {
                Log.w(TAG, "Uri.parse falló al validar URL web.")
            }
            UnsupportedWebUrlReason.UNSUPPORTED_WEB_TARGET -> {
                Log.w(TAG, "URL rechazada: el destino no es una web http/https válida.")
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
