package com.carolina.analizadorseguridadqr.network

object BackendConfig {
    /*
     * En desarrollo con movil fisico + USB, `adb reverse tcp:8000 tcp:8000`
     * hace que 127.0.0.1:8000 en el movil apunte al backend del PC.
     * Sin adb reverse, 127.0.0.1/localhost apuntan al propio movil.
     */
    const val BASE_URL = "http://127.0.0.1:8000/"
}
