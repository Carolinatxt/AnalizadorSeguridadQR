package com.carolina.analizadorseguridadqr.network.model

import com.carolina.analizadorseguridadqr.security.MAX_WEB_URL_LENGTH

data class ScreenshotRequest(
    val url: String,
) {
    init {
        // Segunda linea de defensa: el flujo principal ya valida antes,
        // pero no conviene enviar entradas claramente invalidas al backend.
        require(url.isNotBlank()) { "La URL no puede estar vacia." }
        require(url.length <= MAX_WEB_URL_LENGTH) {
            "La URL supera la longitud maxima permitida."
        }
        require(!url.any { it.isISOControl() }) {
            "La URL contiene caracteres de control no permitidos."
        }
    }
}
