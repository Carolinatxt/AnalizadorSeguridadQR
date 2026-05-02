package com.carolina.analizadorseguridadqr.network.model

import com.carolina.analizadorseguridadqr.security.MAX_WEB_URL_LENGTH

data class AnalyzeUrlRequest(
    val url: String,
) {
    init {
        // El ViewModel ya valida antes de llegar aquí.
        // Esta comprobación es una segunda línea de defensa contra errores de programación.
        require(url.isNotBlank()) { "La URL no puede estar vacía." }
        require(url.length <= MAX_WEB_URL_LENGTH) {
            "La URL supera la longitud máxima permitida."
        }
        require(!url.any { it.isISOControl() }) {
            "La URL contiene caracteres de control no permitidos."
        }
    }
}
