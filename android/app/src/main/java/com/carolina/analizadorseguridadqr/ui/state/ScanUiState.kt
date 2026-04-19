package com.carolina.analizadorseguridadqr.ui.state

// Estados de la pantalla principal durante el flujo de escaneo/análisis.
sealed class ScanUiState {
    // Estado inicial: aún no hay datos de QR.
    data object Idle : ScanUiState()

    // Estado de trabajo (por ejemplo, esperando resultado de escaneo).
    data object Loading : ScanUiState()

    // El QR existe, pero su contenido no es un enlace web válido para esta app.
    data class NotAWebUrl(val message: String) : ScanUiState()

    // El QR contiene una URL válida y lista para el siguiente paso.
    data class ReadyToAnalyze(val url: String) : ScanUiState()

    // Resultado final devuelto por el backend.
    data class AnalysisResult(
        val riskLevel: String,
        val analysisStatus: String,
        val summary: String,
    ) : ScanUiState()

    // Estado de error genérico para fallos inesperados de flujo.
    data class Error(val message: String) : ScanUiState()
}
