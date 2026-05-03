package com.carolina.analizadorseguridadqr.network

import com.carolina.analizadorseguridadqr.network.model.ScreenshotRequest
import com.carolina.analizadorseguridadqr.network.model.ScreenshotResponse

// Servicio separado para no mezclar la logica de analisis con la de screenshots.
class ScreenshotService(
    private val api: ScreenshotApi = AnalysisApiClient.retrofit.create(ScreenshotApi::class.java),
) {
    suspend fun createScreenshot(url: String): ScreenshotResponse {
        return api.createScreenshot(ScreenshotRequest(url = url))
    }
}
