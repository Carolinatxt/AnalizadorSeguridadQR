package com.carolina.analizadorseguridadqr.network.model

data class AnalyzeUrlRequest(
    val url: String,
) {
    init {
        // El ViewModel ya valida antes de llegar aquí.
        // Esta comprobación es una segunda línea de defensa contra errores de programación.
        require(url.isNotBlank()) { "La URL no puede estar vacía." }
    }
}
