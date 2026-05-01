package com.carolina.analizadorseguridadqr.ui.state

enum class RiskLevel {
    SAFE,
    SUSPICIOUS,
    DANGEROUS,
    UNKNOWN,
}

enum class AnalysisStatus {
    COMPLETE,
    PARTIAL,
    UNAVAILABLE,
    UNKNOWN,
}

// Estados de la pantalla principal durante el flujo de escaneo/análisis.
sealed class ScanUiState {
    // Estado inicial: aún no hay datos de QR.
    data object Idle : ScanUiState()

    // Estado de trabajo (por ejemplo, esperando respuesta del backend de analisis).
    data object Loading : ScanUiState()

    // El QR existe, pero su contenido no es un enlace web válido para esta app.
    data class NotAWebUrl(val message: String) : ScanUiState()

    // Estado transitorio: URL válida detectada y lista para analizar.
    data class ReadyToAnalyze(val url: String) : ScanUiState()

    // Resultado final devuelto por el backend.
    data class AnalysisResult(
        val riskLevel: RiskLevel,
        val analysisStatus: AnalysisStatus,
        val summary: String,
        val reasons: List<String> = emptyList(),
        val analyzedUrl: String? = null,
    ) : ScanUiState()

    // Estado de error genérico para fallos inesperados de flujo.
    data class Error(val message: String) : ScanUiState()
}
