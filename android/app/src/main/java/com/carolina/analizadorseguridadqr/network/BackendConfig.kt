package com.carolina.analizadorseguridadqr.network

import com.carolina.analizadorseguridadqr.BuildConfig

object BackendConfig {
    /*
     * SOLO DESARROLLO LOCAL:
     * En desarrollo con móvil físico + USB, ejecutar:
     *   adb reverse tcp:8000 tcp:8000
     *
     * Esto hace que 127.0.0.1:8000 en el móvil apunte al backend del PC.
     * Sin adb reverse, 127.0.0.1/localhost apuntan al propio móvil.
     *
     * PRODUCCIÓN:
     * - Usar HTTPS.
     * - No usar 127.0.0.1 como destino.
     * - Desactivar cleartextTraffic en AndroidManifest.xml.
     */
    const val BASE_URL = BuildConfig.BACKEND_BASE_URL
}
